package com.focusgrowing.app.domain.model

enum class NotificationType { MISSION, FOCUS, WORLD, STREAK, SYSTEM }

/** An entry in the in-app notification inbox. */
data class AppNotification(
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val detail: String? = null,
    val missionId: Long? = null,
    val createdAt: Long,
    val isRead: Boolean = false,
)
