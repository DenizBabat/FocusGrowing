package com.focusgrowing.app.domain.model

enum class TimerPhase { IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED }

/**
 * Persisted timer state. Remaining time is always derived from timestamps
 * (`endAt - now`), never from a decrementing counter, so it survives
 * rotation, backgrounding and process death.
 */
data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val sessionType: SessionType = SessionType.FOCUS,
    val missionId: Long? = null,
    val startedAt: Long = 0L,
    /** Wall-clock end time while RUNNING. */
    val endAt: Long = 0L,
    val plannedDurationMillis: Long = 0L,
    /** Remaining time captured when PAUSED. */
    val remainingWhenPausedMillis: Long = 0L,
    val pauseCount: Int = 0,
    /** Focus sessions finished since the last long break (drives the long break interval). */
    val focusCountInCycle: Int = 0,
) {
    val isActive: Boolean get() = phase == TimerPhase.RUNNING || phase == TimerPhase.PAUSED
}
