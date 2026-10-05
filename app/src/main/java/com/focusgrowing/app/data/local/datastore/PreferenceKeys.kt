package com.focusgrowing.app.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Keys for the "settings" DataStore. Changing a key name loses the stored value. */
internal object SettingsKeys {
    val UserName = stringPreferencesKey("user_name")
    val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")

    val FocusMinutes = intPreferencesKey("focus_minutes")
    val ShortBreakMinutes = intPreferencesKey("short_break_minutes")
    val LongBreakMinutes = intPreferencesKey("long_break_minutes")
    val LongBreakInterval = intPreferencesKey("long_break_interval")

    val SelectedBackground = stringPreferencesKey("selected_background")
    val ShowClock = booleanPreferencesKey("show_clock")
    val ShowMotivation = booleanPreferencesKey("show_motivation")
    val CustomMotivation = stringPreferencesKey("custom_motivation")
    val ShowSessionDots = booleanPreferencesKey("show_session_dots")
    val ShowMissionTitle = booleanPreferencesKey("show_mission_title")
    val OverlayAlpha = floatPreferencesKey("overlay_alpha")
    val BlurRadius = floatPreferencesKey("blur_radius")

    val ThemeMode = stringPreferencesKey("theme_mode")
    val DynamicColor = booleanPreferencesKey("dynamic_color")
    val PaletteId = stringPreferencesKey("palette_id")
    val TextScale = stringPreferencesKey("text_scale")
    val ReduceMotion = booleanPreferencesKey("reduce_motion")

    val SessionAlerts = booleanPreferencesKey("session_alerts")
    val OngoingNotification = booleanPreferencesKey("ongoing_notification")
    val AlertSound = booleanPreferencesKey("alert_sound")
    val Haptics = booleanPreferencesKey("haptics")

    val PremiumActive = booleanPreferencesKey("premium_active")
    val PremiumPlan = stringPreferencesKey("premium_plan")
    val PremiumScheduledPlan = stringPreferencesKey("premium_scheduled_plan")

    // Premium palette unlocked for 24 hours by a rewarded ad.
    val TrialPaletteId = stringPreferencesKey("trial_palette_id")
    val TrialGrantedAt = longPreferencesKey("trial_granted_at")
    val TrialExpiresAt = longPreferencesKey("trial_expires_at")
}

/** Keys for the "timer_state" DataStore. */
internal object TimerKeys {
    val Phase = stringPreferencesKey("phase")
    val SessionType = stringPreferencesKey("session_type")
    val MissionId = longPreferencesKey("mission_id")
    val StartedAt = longPreferencesKey("started_at")
    val EndAt = longPreferencesKey("end_at")
    val Planned = longPreferencesKey("planned_ms")
    val RemainingPaused = longPreferencesKey("remaining_paused_ms")
    val PauseCount = intPreferencesKey("pause_count")
    val FocusCountInCycle = intPreferencesKey("focus_count_in_cycle")
}

internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
