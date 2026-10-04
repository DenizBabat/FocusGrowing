package com.focusgrowing.app.data.mapper

import com.focusgrowing.app.data.local.entity.BackgroundImageEntity
import com.focusgrowing.app.data.local.entity.FocusSessionEntity
import com.focusgrowing.app.data.local.entity.MissionEntity
import com.focusgrowing.app.data.local.entity.MissionWithSubTasks
import com.focusgrowing.app.data.local.entity.NotificationEntity
import com.focusgrowing.app.data.local.entity.SubTaskEntity
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.BackgroundCategory
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundScene
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.SubTask

fun MissionWithSubTasks.toDomain(): Mission = Mission(
    id = mission.id,
    title = mission.title,
    description = mission.description,
    estimatedPomodoros = mission.estimatedPomodoros,
    completedPomodoros = mission.completedPomodoros,
    status = mission.status,
    priority = mission.priority,
    category = mission.category,
    createdAt = mission.createdAt,
    updatedAt = mission.updatedAt,
    completedAt = mission.completedAt,
    subTasks = subTasks.sortedBy { it.position }.map { it.toDomain() },
)

fun SubTaskEntity.toDomain() = SubTask(id = id, missionId = missionId, title = title, isDone = isDone, position = position)

fun Mission.toEntity() = MissionEntity(
    id = id,
    title = title,
    description = description,
    estimatedPomodoros = estimatedPomodoros,
    completedPomodoros = completedPomodoros,
    status = status,
    priority = priority,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    missionId = missionId,
    type = type,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedDurationSeconds = plannedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    completed = completed,
    interruptionCount = interruptionCount,
    xpEarned = xpEarned,
)

fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    missionId = missionId,
    type = type,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedDurationSeconds = plannedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    completed = completed,
    interruptionCount = interruptionCount,
    xpEarned = xpEarned,
)

fun BackgroundImageEntity.toDomain() = BackgroundImage(
    id = BackgroundImage.customId(id),
    name = name,
    source = source,
    category = BackgroundCategory.MY_PHOTOS,
    uri = uri,
    scene = null,
    createdAt = createdAt,
    isFavorite = isFavorite,
    alignX = alignX,
    alignY = alignY,
    zoom = zoom,
)

fun BackgroundScene.toDomain(isFavorite: Boolean) = BackgroundImage(
    id = BackgroundImage.defaultId(this),
    name = displayName,
    source = BackgroundSource.DEFAULT,
    category = category,
    uri = null,
    scene = this,
    createdAt = 0L,
    isFavorite = isFavorite,
)

fun NotificationEntity.toDomain() = AppNotification(
    id = id,
    type = type,
    title = title,
    message = message,
    detail = detail,
    missionId = missionId,
    createdAt = createdAt,
    isRead = isRead,
)

fun AppNotification.toEntity() = NotificationEntity(
    id = id,
    type = type,
    title = title,
    message = message,
    detail = detail,
    missionId = missionId,
    createdAt = createdAt,
    isRead = isRead,
)
