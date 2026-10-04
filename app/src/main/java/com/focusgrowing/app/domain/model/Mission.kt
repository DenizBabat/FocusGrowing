package com.focusgrowing.app.domain.model

enum class MissionStatus { TODO, IN_PROGRESS, COMPLETED, ARCHIVED }

enum class MissionPriority { LOW, MEDIUM, HIGH }

enum class MissionCategory { GENERAL, DEVELOPMENT, DESIGN, STUDY, READING, WORK, HEALTH, PERSONAL }

data class SubTask(
    val id: Long = 0,
    val missionId: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val position: Int = 0,
)

data class Mission(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val estimatedPomodoros: Int,
    val completedPomodoros: Int = 0,
    val status: MissionStatus = MissionStatus.TODO,
    val priority: MissionPriority = MissionPriority.MEDIUM,
    val category: MissionCategory = MissionCategory.GENERAL,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val completedAt: Long? = null,
    val subTasks: List<SubTask> = emptyList(),
) {
    val progress: Float
        get() = if (estimatedPomodoros <= 0) 0f
        else (completedPomodoros.toFloat() / estimatedPomodoros).coerceIn(0f, 1f)

    val isCompleted: Boolean get() = status == MissionStatus.COMPLETED
    val isActive: Boolean get() = status == MissionStatus.TODO || status == MissionStatus.IN_PROGRESS
}
