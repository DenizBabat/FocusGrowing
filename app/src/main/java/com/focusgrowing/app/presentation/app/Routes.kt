package com.focusgrowing.app.presentation.app

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. `-1L` means "none" for optional ids. */
@Serializable data object HomeRoute
@Serializable data object MissionsRoute
@Serializable data class MissionDetailRoute(val missionId: Long)
@Serializable data class MissionEditorRoute(val missionId: Long = NO_ID)
@Serializable data class FocusRoute(val missionId: Long = NO_ID)
@Serializable data object StatisticsRoute
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute
@Serializable data object BackgroundGalleryRoute
@Serializable data class BackgroundAdjustRoute(val backgroundId: String)
@Serializable data object PremiumRoute
@Serializable data object NotificationsRoute
@Serializable data object CelebrationRoute

const val NO_ID = -1L
