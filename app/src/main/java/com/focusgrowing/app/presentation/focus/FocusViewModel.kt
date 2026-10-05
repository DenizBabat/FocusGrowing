package com.focusgrowing.app.presentation.focus

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.R
import com.focusgrowing.app.core.timer.FocusTimerManager
import com.focusgrowing.app.domain.logic.TimerCalculator
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.FocusScreenSettings
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.presentation.app.FocusRoute
import com.focusgrowing.app.presentation.app.NO_ID
import com.focusgrowing.app.presentation.app.route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

data class FocusUiState(
    val loading: Boolean = true,
    val phase: TimerPhase = TimerPhase.IDLE,
    val sessionType: SessionType = SessionType.FOCUS,
    val timeText: String = "25:00",
    val progress: Float = 0f,
    val remainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val plannedMillis: Long = 0L,
    val mission: Mission? = null,
    val sessionIndex: Int = 1,
    val sessionsPerCycle: Int = 4,
    val nextBreak: SessionType = SessionType.SHORT_BREAK,
    val pomodoro: PomodoroSettings = PomodoroSettings(),
    val screen: FocusScreenSettings = FocusScreenSettings(),
    val background: BackgroundImage? = null,
    val clockText: String = "",
    /** The user's own motivational text (Premium); blank when a built-in one is shown instead. */
    val motivation: String = "",
    /** Built-in motivational text, resolved in the UI so it follows the app language. */
    @StringRes val motivationRes: Int? = null,
    val isPremium: Boolean = false,
    val activeMissions: List<Mission> = emptyList(),
) {
    val isRunning: Boolean get() = phase == TimerPhase.RUNNING
    val isPaused: Boolean get() = phase == TimerPhase.PAUSED
    val isActive: Boolean get() = isRunning || isPaused
    val isFinished: Boolean get() = phase == TimerPhase.COMPLETED
    /** Finishing early only counts when at least half of the planned time was focused. */
    val earlyFinishCounts: Boolean get() = elapsedMillis * 2 >= plannedMillis
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timer: FocusTimerManager,
    private val settings: SettingsRepository,
    private val missions: MissionRepository,
    backgrounds: BackgroundRepository,
    private val premium: PremiumManager,
    private val time: TimeProvider,
) : ViewModel() {

    private val routeMissionId: Long? = savedStateHandle.route<FocusRoute>().missionId.takeIf { it != NO_ID }
    private val selectedMissionId = MutableStateFlow(routeMissionId)

    private val timerState: Flow<TimerState> = timer.state.filterNotNull()

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(time.now())
            delay(TICK_MS)
        }
    }

    private val effectiveMissionId: Flow<Long?> = combine(timerState, selectedMissionId) { t, selected ->
        if (t.isActive || t.phase == TimerPhase.COMPLETED) t.missionId ?: selected else selected
    }.distinctUntilChanged()

    private val mission: Flow<Mission?> = effectiveMissionId.flatMapLatest { id ->
        if (id == null) flowOf(null) else missions.observeMission(id)
    }

    private val background: Flow<BackgroundImage?> = settings.preferences
        .map { it.focusScreen.selectedBackgroundId }
        .distinctUntilChanged()
        .flatMapLatest { id -> backgrounds.observeBackground(id) }

    private val extras = combine(background, premium.premiumState, missions.observeMissions()) { bg, isPremium, all ->
        Triple(bg, isPremium, all.filter { it.isActive })
    }

    private var lastElapsedSignal = 0L

    val uiState: StateFlow<FocusUiState> = combine(
        timerState, ticker, settings.preferences, mission, extras,
    ) { t, now, prefs, m, (bg, isPremium, active) ->
        val idlePlanned = prefs.pomodoro.minutesFor(t.sessionType.takeIf { t.phase != TimerPhase.IDLE } ?: SessionType.FOCUS) * 60_000L
        val planned = if (t.isActive || t.phase == TimerPhase.COMPLETED) t.plannedDurationMillis else idlePlanned
        val remaining = when (t.phase) {
            TimerPhase.IDLE, TimerPhase.CANCELLED -> planned
            else -> TimerCalculator.remainingMillis(t, now)
        }
        if (TimerCalculator.isElapsed(t, now) && lastElapsedSignal != t.endAt) {
            lastElapsedSignal = t.endAt
            viewModelScope.launch { timer.onTimeElapsed() }
        }
        val interval = prefs.pomodoro.longBreakInterval
        val sessionIndex = if (t.sessionType == SessionType.FOCUS && t.phase != TimerPhase.COMPLETED) {
            (t.focusCountInCycle % interval) + 1
        } else {
            ((t.focusCountInCycle - 1).coerceAtLeast(0) % interval) + 1
        }
        FocusUiState(
            loading = false,
            phase = t.phase,
            sessionType = if (t.phase == TimerPhase.IDLE || t.phase == TimerPhase.CANCELLED) SessionType.FOCUS else t.sessionType,
            timeText = TimerCalculator.formatMmSs(remaining),
            progress = if (planned > 0) 1f - remaining.toFloat() / planned else 0f,
            remainingMillis = remaining,
            elapsedMillis = (planned - remaining).coerceAtLeast(0L),
            plannedMillis = planned,
            mission = m,
            sessionIndex = sessionIndex,
            sessionsPerCycle = interval,
            nextBreak = TimerCalculator.nextBreakType(t.focusCountInCycle, interval),
            pomodoro = prefs.pomodoro,
            screen = prefs.focusScreen,
            background = bg,
            clockText = CLOCK_FORMAT.format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())),
            motivation = customMotivation(prefs.focusScreen, isPremium),
            motivationRes = MOTIVATIONS[abs((t.startedAt / 1000) % MOTIVATIONS.size).toInt()],
            isPremium = isPremium,
            activeMissions = active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun selectMission(id: Long?) {
        selectedMissionId.value = id
    }

    fun start() = launchTimer { t -> t.start(SessionType.FOCUS, uiState.value.mission?.id) }

    fun startBreak() = launchTimer { t ->
        val s = uiState.value
        t.start(s.nextBreak, s.mission?.id)
    }

    fun startNextFocus() = launchTimer { t ->
        t.reset()
        t.start(SessionType.FOCUS, uiState.value.mission?.id)
    }

    fun pause() = launchTimer { t -> t.pause() }
    fun resume() = launchTimer { t -> t.resume() }
    fun finish() = launchTimer { t -> t.finishEarly() }
    fun cancel() = launchTimer { t -> t.cancel() }
    fun reset() = launchTimer { t -> t.reset() }

    fun updateScreenSettings(transform: (FocusScreenSettings) -> FocusScreenSettings) {
        viewModelScope.launch { settings.updateFocusScreen(transform) }
    }

    fun canUse(feature: PremiumFeature): Boolean = premium.hasAccess(feature)

    private fun launchTimer(block: suspend (FocusTimerManager) -> Unit) {
        viewModelScope.launch { block(timer) }
    }

    private fun customMotivation(screen: FocusScreenSettings, isPremium: Boolean): String {
        val custom = screen.customMotivation
        return if (isPremium && !custom.isNullOrBlank()) custom else ""
    }

    private companion object {
        const val TICK_MS = 250L
        val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val MOTIVATIONS = listOf(
            R.string.focus_quote_1,
            R.string.focus_quote_2,
            R.string.focus_quote_3,
            R.string.focus_quote_4,
            R.string.focus_quote_5,
        )
    }
}
