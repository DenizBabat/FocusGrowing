package com.focusgrowing.app.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TextScale(val factor: Float) { SMALL(0.9f), MEDIUM(1f), LARGE(1.15f) }

data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
) {
    fun minutesFor(type: SessionType) = when (type) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    }

    companion object {
        val FocusPresets = listOf(15, 20, 25, 30, 45, 50, 60)
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
    }
}

data class FocusScreenSettings(
    val selectedBackgroundId: String = BackgroundImage.FallbackId,
    val showClock: Boolean = false,
    val showMotivation: Boolean = true,
    val customMotivation: String? = null,
    val showSessionDots: Boolean = true,
    val showMissionTitle: Boolean = true,
    /** 0..1 darkness of the overlay drawn over the background. */
    val overlayAlpha: Float = 0.25f,
    /** Premium: blur radius in dp (0 = off). */
    val blurRadius: Float = 0f,
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val paletteId: String = "mint_meadow",
    val textScale: TextScale = TextScale.MEDIUM,
    val reduceMotion: Boolean = false,
)

data class NotificationSettings(
    val sessionAlerts: Boolean = true,
    val ongoingTimerNotification: Boolean = true,
    val alertSoundAndVibration: Boolean = true,
    val hapticFeedback: Boolean = true,
)

data class UserPreferences(
    val userName: String = "",
    val onboardingCompleted: Boolean = false,
    val pomodoro: PomodoroSettings = PomodoroSettings(),
    val focusScreen: FocusScreenSettings = FocusScreenSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
)
