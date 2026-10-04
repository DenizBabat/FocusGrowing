package com.focusgrowing.app.domain.model

enum class SessionType { FOCUS, SHORT_BREAK, LONG_BREAK;
    val isBreak: Boolean get() = this != FOCUS
}

data class FocusSession(
    val id: Long = 0,
    val missionId: Long?,
    val type: SessionType,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedDurationSeconds: Long,
    val actualDurationSeconds: Long,
    val completed: Boolean,
    val interruptionCount: Int,
    val xpEarned: Int,
)
