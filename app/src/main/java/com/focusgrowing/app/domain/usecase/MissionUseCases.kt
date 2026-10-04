package com.focusgrowing.app.domain.usecase

import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import javax.inject.Inject

data class MissionDraft(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val estimatedPomodoros: Int = 4,
    val priority: MissionPriority = MissionPriority.MEDIUM,
    val category: MissionCategory = MissionCategory.GENERAL,
    /** Only used when creating: initial checklist items. */
    val subTasks: List<String> = emptyList(),
)

enum class MissionValidationError { EMPTY_TITLE, TITLE_TOO_LONG, INVALID_ESTIMATE }

sealed interface SaveMissionResult {
    data class Saved(val missionId: Long, val created: Boolean) : SaveMissionResult
    data class Invalid(val error: MissionValidationError) : SaveMissionResult
}

class SaveMissionUseCase @Inject constructor(
    private val missions: MissionRepository,
    private val notifications: NotificationRepository,
    private val celebrations: CelebrationQueue,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(draft: MissionDraft): SaveMissionResult {
        val title = draft.title.trim()
        validate(title, draft.estimatedPomodoros)?.let { return SaveMissionResult.Invalid(it) }
        val now = time.now()
        val description = draft.description.trim().ifBlank { null }

        val existing = draft.id?.let { missions.getMission(it) }
        if (existing != null) {
            missions.updateMission(
                existing.copy(
                    title = title,
                    description = description,
                    estimatedPomodoros = draft.estimatedPomodoros,
                    priority = draft.priority,
                    category = draft.category,
                    updatedAt = now,
                ),
            )
            return SaveMissionResult.Saved(existing.id, created = false)
        }

        val id = missions.createMission(
            Mission(
                title = title,
                description = description,
                estimatedPomodoros = draft.estimatedPomodoros,
                priority = draft.priority,
                category = draft.category,
                createdAt = now,
            ),
            draft.subTasks.map { it.trim() }.filter { it.isNotEmpty() },
        )
        notifications.add(
            AppNotification(
                type = NotificationType.MISSION,
                title = "New Mission Assigned",
                message = "\"$title\"",
                detail = "Estimated ${draft.estimatedPomodoros} Pomodoros",
                missionId = id,
                createdAt = now,
            ),
        )
        celebrations.enqueue(
            listOf(
                Celebration.MissionCreated(id, title, draft.estimatedPomodoros, draft.priority, draft.category),
            ),
        )
        return SaveMissionResult.Saved(id, created = true)
    }

    companion object {
        const val MAX_TITLE = 80
        const val MAX_ESTIMATE = 50

        fun validate(title: String, estimate: Int): MissionValidationError? = when {
            title.isBlank() -> MissionValidationError.EMPTY_TITLE
            title.length > MAX_TITLE -> MissionValidationError.TITLE_TOO_LONG
            estimate !in 1..MAX_ESTIMATE -> MissionValidationError.INVALID_ESTIMATE
            else -> null
        }
    }
}

/** User marks a mission as done manually (e.g. finished before the estimate). */
class CompleteMissionUseCase @Inject constructor(
    private val missions: MissionRepository,
    private val notifications: NotificationRepository,
    private val xpAwarder: XpAwarder,
    private val celebrations: CelebrationQueue,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(missionId: Long) {
        val mission = missions.getMission(missionId) ?: return
        if (mission.isCompleted) return
        val now = time.now()
        missions.updateStatus(missionId, MissionStatus.COMPLETED, now)
        val bonus = WorldProgression.missionCompletionBonus(mission.estimatedPomodoros)
        notifications.add(
            AppNotification(
                type = NotificationType.MISSION,
                title = "Mission Completed!",
                message = "\"${mission.title}\"",
                detail = "You earned +$bonus bonus XP",
                missionId = missionId,
                createdAt = now,
            ),
        )
        val (_, levelUp) = xpAwarder.award(bonus, now)
        celebrations.enqueue(
            listOfNotNull(
                Celebration.MissionCompleted(
                    missionId = missionId,
                    title = mission.title,
                    xpEarned = bonus,
                    pomodoros = mission.completedPomodoros,
                    focusMinutes = 0,
                ),
                levelUp,
            ),
        )
    }
}

class ReopenMissionUseCase @Inject constructor(private val missions: MissionRepository) {
    suspend operator fun invoke(missionId: Long) {
        missions.updateStatus(missionId, MissionStatus.IN_PROGRESS, completedAt = null)
    }
}

class ArchiveMissionUseCase @Inject constructor(private val missions: MissionRepository) {
    suspend operator fun invoke(missionId: Long) {
        missions.updateStatus(missionId, MissionStatus.ARCHIVED, completedAt = null)
    }
}
