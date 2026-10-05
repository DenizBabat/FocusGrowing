package com.focusgrowing.app.domain.usecase

import com.focusgrowing.app.domain.logic.StreakCalculator
import com.focusgrowing.app.domain.logic.TimerCalculator
import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.domain.model.SessionReward
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.repository.FocusSessionRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.domain.repository.DomainStrings
import java.time.Instant
import javax.inject.Inject

/**
 * The heart of the product loop:
 * Focus session → mission progress → world XP → streak → statistics / inbox / celebrations.
 */
class CompleteFocusSessionUseCase @Inject constructor(
    private val sessions: FocusSessionRepository,
    private val missions: MissionRepository,
    private val notifications: NotificationRepository,
    private val xpAwarder: XpAwarder,
    private val time: TimeProvider,
    private val strings: DomainStrings,
) {
    /**
     * @param finishedEarly the user pressed "Finish" before the timer ran out.
     *        Early finishes count only when at least half of the planned time was focused.
     */
    suspend operator fun invoke(state: TimerState, endedAt: Long, finishedEarly: Boolean): SessionReward {
        val elapsedMillis = if (finishedEarly) TimerCalculator.elapsedMillis(state, endedAt) else state.plannedDurationMillis
        val actualSeconds = elapsedMillis / 1000
        val plannedSeconds = state.plannedDurationMillis / 1000
        val counts = !finishedEarly || elapsedMillis * 2 >= state.plannedDurationMillis

        fun session(completed: Boolean, xp: Int) = FocusSession(
            missionId = state.missionId,
            type = state.sessionType,
            startedAt = state.startedAt,
            endedAt = endedAt,
            plannedDurationSeconds = plannedSeconds,
            actualDurationSeconds = actualSeconds,
            completed = completed,
            interruptionCount = state.pauseCount,
            xpEarned = xp,
        )

        val focusMinutes = (actualSeconds / 60).toInt()
        if (state.sessionType.isBreak || !counts) {
            sessions.insert(session(completed = counts, xp = 0))
            return SessionReward(
                sessionType = state.sessionType,
                counted = counts,
                focusMinutes = focusMinutes,
                xpEarned = 0,
                missionId = state.missionId,
                missionTitle = null,
                missionCompleted = false,
                newLevel = null,
                streak = null,
                streakIncreased = false,
                celebrations = emptyList(),
            )
        }

        val zone = time.zone()
        val today = Instant.ofEpochMilli(endedAt).atZone(zone).toLocalDate()
        val streakBefore = StreakCalculator.calculate(sessions.getCompletedFocusStartTimes(), zone, today)

        val sessionXp = WorldProgression.xpForFocus(actualSeconds)
        sessions.insert(session(completed = true, xp = sessionXp))

        // Mission progress
        var missionTitle: String? = null
        var missionCompleted = false
        var missionBonus = 0
        var missionPomodoros = 0
        val mission = state.missionId?.let { missions.getMission(it) }
        if (mission != null && mission.isActive) {
            val newCompleted = mission.completedPomodoros + 1
            val complete = newCompleted >= mission.estimatedPomodoros && mission.subTasks.all { it.isDone }
            missions.updateMission(
                mission.copy(
                    completedPomodoros = newCompleted,
                    status = if (complete) MissionStatus.COMPLETED else MissionStatus.IN_PROGRESS,
                    completedAt = if (complete) endedAt else null,
                    updatedAt = endedAt,
                ),
            )
            missionTitle = mission.title
            missionPomodoros = newCompleted
            if (complete) {
                missionCompleted = true
                missionBonus = WorldProgression.missionCompletionBonus(mission.estimatedPomodoros)
            }
        }

        val (award, levelUp) = xpAwarder.award(sessionXp + missionBonus, endedAt)

        val streakAfter = StreakCalculator.calculate(sessions.getCompletedFocusStartTimes(), zone, today)
        val streakIncreased = !streakBefore.isActiveToday && streakAfter.isActiveToday

        // Inbox entries
        notifications.add(
            AppNotification(
                type = NotificationType.FOCUS,
                title = strings.focusCompletedTitle(),
                message = strings.focusCompletedMessage(focusMinutes),
                detail = strings.xpDetail(sessionXp, missionTitle),
                missionId = state.missionId,
                createdAt = endedAt,
            ),
        )
        if (missionCompleted && missionTitle != null) {
            notifications.add(
                AppNotification(
                    type = NotificationType.MISSION,
                    title = strings.missionCompletedTitle(),
                    message = strings.quoted(missionTitle),
                    detail = strings.bonusXpDetail(missionBonus),
                    missionId = state.missionId,
                    createdAt = endedAt,
                ),
            )
        }
        if (streakIncreased && streakAfter.currentDays >= 2) {
            notifications.add(
                AppNotification(
                    type = NotificationType.STREAK,
                    title = strings.streakTitle(),
                    message = strings.streakMessage(streakAfter.currentDays),
                    detail = strings.streakDetail(),
                    createdAt = endedAt,
                ),
            )
        }

        // Celebration screens, in the order they should appear
        val celebrations = buildList {
            add(Celebration.FocusCompleted(focusMinutes, sessionXp, if (mission != null) 1 else 0))
            if (missionCompleted && mission != null) {
                add(
                    Celebration.MissionCompleted(
                        missionId = mission.id,
                        title = mission.title,
                        xpEarned = missionBonus,
                        pomodoros = missionPomodoros,
                        focusMinutes = focusMinutes,
                    ),
                )
            }
            if (streakIncreased && streakAfter.currentDays >= 2) {
                add(Celebration.StreakContinued(streakAfter.currentDays, streakAfter.weekActivity))
            }
            levelUp?.let { add(it) }
        }

        return SessionReward(
            sessionType = state.sessionType,
            counted = true,
            focusMinutes = focusMinutes,
            xpEarned = award.amount,
            missionId = state.missionId,
            missionTitle = missionTitle,
            missionCompleted = missionCompleted,
            newLevel = if (award.leveledUp) award.levelAfter else null,
            streak = streakAfter,
            streakIncreased = streakIncreased,
            celebrations = celebrations,
        )
    }
}

/** Stores a session the user cancelled so interruption analysis stays honest. */
class RecordInterruptedSessionUseCase @Inject constructor(
    private val sessions: FocusSessionRepository,
) {
    suspend operator fun invoke(state: TimerState, endedAt: Long) {
        val elapsed = TimerCalculator.elapsedMillis(state, endedAt)
        if (elapsed < 60_000L) return // under a minute: treat as a mis-tap, don't pollute statistics
        sessions.insert(
            FocusSession(
                missionId = state.missionId,
                type = state.sessionType,
                startedAt = state.startedAt,
                endedAt = endedAt,
                plannedDurationSeconds = state.plannedDurationMillis / 1000,
                actualDurationSeconds = elapsed / 1000,
                completed = false,
                interruptionCount = state.pauseCount,
                xpEarned = 0,
            ),
        )
    }
}
