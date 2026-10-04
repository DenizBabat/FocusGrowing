package com.focusgrowing.app.core.timer

import com.focusgrowing.app.core.notification.FocusNotifier
import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.logic.TimerCalculator
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.SessionReward
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.domain.repository.TimerStateRepository
import com.focusgrowing.app.domain.usecase.CompleteFocusSessionUseCase
import com.focusgrowing.app.domain.usecase.RecordInterruptedSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TimerEvent {
    data class Finished(val reward: SessionReward) : TimerEvent
}

/**
 * Application-scoped owner of the Pomodoro timer. It is independent of any Activity or
 * Composable: state is persisted after every change and remaining time is derived from
 * timestamps, so rotation, backgrounding and process death never lose a session.
 *
 * All mutations go through [mutex], which also makes completion idempotent when the UI
 * ticker and the alarm fire at the same moment.
 */
@Singleton
class FocusTimerManager @Inject constructor(
    private val repository: TimerStateRepository,
    private val settings: SettingsRepository,
    private val missions: MissionRepository,
    private val completeSession: CompleteFocusSessionUseCase,
    private val recordInterrupted: RecordInterruptedSessionUseCase,
    private val celebrations: CelebrationQueue,
    private val alarms: TimerAlarmScheduler,
    private val notifier: FocusNotifier,
    private val foreground: AppForegroundTracker,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /** Null until the persisted state has been read once. */
    val state: StateFlow<TimerState?> = repository.state
        .map<TimerState, TimerState?> { it }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    fun canScheduleExactAlarms(): Boolean = alarms.canScheduleExact()

    suspend fun start(type: SessionType, missionId: Long?) = mutex.withLock {
        val current = repository.current()
        if (current.isActive) return@withLock
        val prefs = settings.current()
        val planned = prefs.pomodoro.minutesFor(type) * 60_000L
        val now = time.now()
        val cycle = if (current.phase == TimerPhase.COMPLETED && current.sessionType == SessionType.LONG_BREAK) 0
        else current.focusCountInCycle
        val new = TimerState(
            phase = TimerPhase.RUNNING,
            sessionType = type,
            missionId = missionId,
            startedAt = now,
            endAt = now + planned,
            plannedDurationMillis = planned,
            remainingWhenPausedMillis = 0L,
            pauseCount = 0,
            focusCountInCycle = cycle,
        )
        repository.save(new)
        alarms.schedule(new.endAt)
        if (type == SessionType.FOCUS && missionId != null) {
            missions.getMission(missionId)?.let { m ->
                if (m.status == MissionStatus.TODO) missions.updateStatus(m.id, MissionStatus.IN_PROGRESS, null)
            }
        }
        showOngoing(new)
    }

    suspend fun pause() = mutex.withLock {
        val s = repository.current()
        if (s.phase != TimerPhase.RUNNING) return@withLock
        val now = time.now()
        if (TimerCalculator.isElapsed(s, now)) {
            finishLocked(s, finishedEarly = false)
            return@withLock
        }
        val paused = s.copy(
            phase = TimerPhase.PAUSED,
            remainingWhenPausedMillis = TimerCalculator.remainingMillis(s, now),
            pauseCount = s.pauseCount + 1,
        )
        repository.save(paused)
        alarms.cancel()
        showOngoing(paused)
    }

    suspend fun resume() = mutex.withLock {
        val s = repository.current()
        if (s.phase != TimerPhase.PAUSED) return@withLock
        val now = time.now()
        val running = s.copy(phase = TimerPhase.RUNNING, endAt = now + s.remainingWhenPausedMillis, remainingWhenPausedMillis = 0L)
        repository.save(running)
        alarms.schedule(running.endAt)
        showOngoing(running)
    }

    /** "Finish" button: end the session now and keep the focused time. */
    suspend fun finishEarly() = mutex.withLock {
        val s = repository.current()
        if (!s.isActive) return@withLock
        finishLocked(s, finishedEarly = true)
    }

    /** "Cancel" button: stop without rewards. The partial session is recorded for insights. */
    suspend fun cancel() = mutex.withLock {
        val s = repository.current()
        if (!s.isActive) return@withLock
        recordInterrupted(s, time.now())
        repository.save(s.copy(phase = TimerPhase.CANCELLED, remainingWhenPausedMillis = 0L))
        alarms.cancel()
        notifier.cancelRunning()
    }

    /** Called by the UI ticker and by the alarm. Safe to call repeatedly. */
    suspend fun onTimeElapsed() = mutex.withLock {
        val s = repository.current()
        if (TimerCalculator.isElapsed(s, time.now())) finishLocked(s, finishedEarly = false)
    }

    /** Back to the idle screen after a finished/cancelled session. Keeps the cycle counter. */
    suspend fun reset() = mutex.withLock {
        val s = repository.current()
        if (s.isActive) return@withLock
        repository.save(TimerState(focusCountInCycle = s.focusCountInCycle, sessionType = s.sessionType, missionId = s.missionId))
    }

    /** App start / boot / return to foreground: finish overdue sessions, re-arm alarms. */
    fun reconcile() {
        scope.launch {
            mutex.withLock {
                val s = repository.current()
                when {
                    TimerCalculator.isElapsed(s, time.now()) -> finishLocked(s, finishedEarly = false)
                    s.phase == TimerPhase.RUNNING -> {
                        alarms.schedule(s.endAt)
                        showOngoing(s)
                    }
                    s.phase == TimerPhase.PAUSED -> showOngoing(s)
                    else -> Unit
                }
            }
        }
    }

    /** Fire-and-forget helper for callers without a coroutine scope. */
    fun runAsync(block: suspend FocusTimerManager.() -> Unit) {
        scope.launch { block(this@FocusTimerManager) }
    }

    private suspend fun finishLocked(s: TimerState, finishedEarly: Boolean) {
        val endedAt = if (finishedEarly) time.now() else s.endAt
        val reward = completeSession(s, endedAt, finishedEarly)
        val cycle = when {
            s.sessionType == SessionType.FOCUS && reward.counted -> s.focusCountInCycle + 1
            else -> s.focusCountInCycle
        }
        repository.save(
            s.copy(
                phase = TimerPhase.COMPLETED,
                endAt = endedAt,
                remainingWhenPausedMillis = 0L,
                focusCountInCycle = cycle,
            ),
        )
        alarms.cancel()
        notifier.cancelRunning()

        val prefs = settings.current()
        if (prefs.notifications.sessionAlerts && !foreground.isInForeground) {
            notifier.showSessionFinished(reward, prefs.notifications.alertSoundAndVibration)
        }
        celebrations.enqueue(reward.celebrations)
        _events.tryEmit(TimerEvent.Finished(reward))
    }

    private suspend fun showOngoing(s: TimerState) {
        val prefs = settings.current()
        if (!prefs.notifications.ongoingTimerNotification) {
            notifier.cancelRunning()
            return
        }
        val title = s.missionId?.let { missions.getMission(it)?.title }
        if (s.phase == TimerPhase.RUNNING) notifier.showRunning(s, title) else notifier.showPaused(s, title)
    }
}
