package com.focusgrowing.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.domain.model.SessionType

// Room stores enums by name, so renaming an enum constant requires a migration.

@Entity(tableName = "missions", indices = [Index("status")])
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val estimatedPomodoros: Int,
    val completedPomodoros: Int,
    val status: MissionStatus,
    val priority: MissionPriority,
    val category: MissionCategory,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)

@Entity(
    tableName = "sub_tasks",
    foreignKeys = [
        ForeignKey(
            entity = MissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("missionId")],
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Long,
    val title: String,
    val isDone: Boolean,
    val position: Int,
)

data class MissionWithSubTasks(
    @Embedded val mission: MissionEntity,
    @Relation(parentColumn = "id", entityColumn = "missionId")
    val subTasks: List<SubTaskEntity>,
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("missionId"), Index("startedAt")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

/** Single-row table holding the world XP (id is always 1). Level is derived from XP. */
@Entity(tableName = "world")
data class WorldEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val totalXp: Int,
    val updatedAt: Long,
) {
    companion object { const val SINGLE_ROW_ID = 1 }
}

@Entity(tableName = "background_images")
data class BackgroundImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val name: String,
    val source: BackgroundSource,
    val createdAt: Long,
    val isFavorite: Boolean,
    val alignX: Float,
    val alignY: Float,
    val zoom: Float,
)

@Entity(tableName = "notifications", indices = [Index("createdAt")])
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val detail: String?,
    val missionId: Long?,
    val createdAt: Long,
    val isRead: Boolean,
)

/** Favorite flags for built-in scenes (they have no row in background_images). */
@Entity(tableName = "default_background_prefs")
data class DefaultBackgroundPrefEntity(
    @PrimaryKey val sceneName: String,
    val isFavorite: Boolean,
)
