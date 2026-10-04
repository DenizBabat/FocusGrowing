package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.domain.model.TimerState
import java.util.Locale

/** Timestamp based timer math (endAt - now). No counters that drift or die with the UI. */
object TimerCalculator {

    fun remainingMillis(state: TimerState, now: Long): Long = when (state.phase) {
        TimerPhase.RUNNING -> (state.endAt - now).coerceIn(0L, state.plannedDurationMillis)
        TimerPhase.PAUSED -> state.remainingWhenPausedMillis.coerceAtLeast(0L)
        TimerPhase.IDLE, TimerPhase.CANCELLED -> state.plannedDurationMillis
        TimerPhase.COMPLETED -> 0L
    }

    fun elapsedMillis(state: TimerState, now: Long): Long =
        (state.plannedDurationMillis - remainingMillis(state, now)).coerceAtLeast(0L)

    fun progress(state: TimerState, now: Long): Float {
        if (state.plannedDurationMillis <= 0L) return 0f
        return (elapsedMillis(state, now).toFloat() / state.plannedDurationMillis).coerceIn(0f, 1f)
    }

    fun isElapsed(state: TimerState, now: Long): Boolean =
        state.phase == TimerPhase.RUNNING && now >= state.endAt

    /** Which break follows a finished focus session. [completedInCycle] includes that session. */
    fun nextBreakType(completedInCycle: Int, longBreakInterval: Int): SessionType =
        if (longBreakInterval > 0 && completedInCycle > 0 && completedInCycle % longBreakInterval == 0) {
            SessionType.LONG_BREAK
        } else {
            SessionType.SHORT_BREAK
        }

    fun formatMmSs(millis: Long): String {
        val totalSeconds = (millis + 999) / 1000 // round up so 24:59.4 shows 25:00 → 24:59 naturally
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
