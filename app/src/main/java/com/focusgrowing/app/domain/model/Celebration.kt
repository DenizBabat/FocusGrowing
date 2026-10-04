package com.focusgrowing.app.domain.model

/** Full-screen reward moments shown after something good happens. */
sealed interface Celebration {
    data class FocusCompleted(val focusMinutes: Int, val xpEarned: Int, val missionProgressDelta: Int) : Celebration
    data class MissionCompleted(
        val missionId: Long,
        val title: String,
        val xpEarned: Int,
        val pomodoros: Int,
        val focusMinutes: Int,
    ) : Celebration
    data class StreakContinued(val days: Int, val weekActivity: List<Boolean>) : Celebration
    data class WorldLevelUp(
        val level: Int,
        val xpIntoLevel: Int,
        val xpForNextLevel: Int,
        val unlocked: List<WorldItemType>,
    ) : Celebration
    data class MissionCreated(
        val missionId: Long,
        val title: String,
        val estimatedPomodoros: Int,
        val priority: MissionPriority,
        val category: MissionCategory,
    ) : Celebration
}

/** Result of finishing a focus session, used for notifications and celebrations. */
data class SessionReward(
    val sessionType: SessionType,
    /** False when a focus session was finished too early to count. */
    val counted: Boolean,
    val focusMinutes: Int,
    val xpEarned: Int,
    val missionId: Long?,
    val missionTitle: String?,
    val missionCompleted: Boolean,
    val newLevel: Int?,
    val streak: StreakInfo?,
    val streakIncreased: Boolean,
    val celebrations: List<Celebration>,
)
