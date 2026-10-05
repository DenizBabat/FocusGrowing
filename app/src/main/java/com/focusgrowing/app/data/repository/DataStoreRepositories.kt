package com.focusgrowing.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.focusgrowing.app.data.local.datastore.SettingsKeys
import com.focusgrowing.app.data.local.datastore.TimerKeys
import com.focusgrowing.app.data.local.datastore.enumOrDefault
import com.focusgrowing.app.di.SettingsStore
import com.focusgrowing.app.di.TimerStore
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.FocusScreenSettings
import com.focusgrowing.app.domain.model.NotificationSettings
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TextScale
import com.focusgrowing.app.domain.model.ThemeMode
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.model.UserPreferences
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import com.focusgrowing.app.domain.repository.TimerStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A corrupted/unreadable file must never crash the app: fall back to defaults. */
private fun Flow<Preferences>.safe(): Flow<Preferences> = catch { e ->
    if (e is IOException) emit(emptyPreferences()) else throw e
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val preferences: Flow<UserPreferences> = store.data.safe().map { it.toUserPreferences() }.distinctUntilChanged()

    override suspend fun current(): UserPreferences = preferences.first()

    override suspend fun setUserName(name: String) {
        store.edit { it[SettingsKeys.UserName] = name.trim().take(40) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[SettingsKeys.OnboardingCompleted] = completed }
    }

    override suspend fun updatePomodoro(transform: (PomodoroSettings) -> PomodoroSettings) {
        store.edit { prefs ->
            val range = PomodoroSettings.MIN_MINUTES..PomodoroSettings.MAX_MINUTES
            val new = transform(prefs.toUserPreferences().pomodoro)
            prefs[SettingsKeys.FocusMinutes] = new.focusMinutes.coerceIn(range)
            prefs[SettingsKeys.ShortBreakMinutes] = new.shortBreakMinutes.coerceIn(range)
            prefs[SettingsKeys.LongBreakMinutes] = new.longBreakMinutes.coerceIn(range)
            prefs[SettingsKeys.LongBreakInterval] = new.longBreakInterval.coerceIn(1, 12)
        }
    }

    override suspend fun updateFocusScreen(transform: (FocusScreenSettings) -> FocusScreenSettings) {
        store.edit { prefs ->
            val new = transform(prefs.toUserPreferences().focusScreen)
            prefs[SettingsKeys.SelectedBackground] = new.selectedBackgroundId
            prefs[SettingsKeys.ShowClock] = new.showClock
            prefs[SettingsKeys.ShowMotivation] = new.showMotivation
            val custom = new.customMotivation?.trim()?.take(60)
            if (custom.isNullOrEmpty()) prefs.remove(SettingsKeys.CustomMotivation)
            else prefs[SettingsKeys.CustomMotivation] = custom
            prefs[SettingsKeys.ShowSessionDots] = new.showSessionDots
            prefs[SettingsKeys.ShowMissionTitle] = new.showMissionTitle
            prefs[SettingsKeys.OverlayAlpha] = new.overlayAlpha.coerceIn(0f, 0.9f)
            prefs[SettingsKeys.BlurRadius] = new.blurRadius.coerceIn(0f, 25f)
        }
    }

    override suspend fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        store.edit { prefs ->
            val new = transform(prefs.toUserPreferences().appearance)
            prefs[SettingsKeys.ThemeMode] = new.themeMode.name
            prefs[SettingsKeys.DynamicColor] = new.dynamicColor
            prefs[SettingsKeys.PaletteId] = new.paletteId
            prefs[SettingsKeys.TextScale] = new.textScale.name
            prefs[SettingsKeys.ReduceMotion] = new.reduceMotion
        }
    }

    override suspend fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings) {
        store.edit { prefs ->
            val new = transform(prefs.toUserPreferences().notifications)
            prefs[SettingsKeys.SessionAlerts] = new.sessionAlerts
            prefs[SettingsKeys.OngoingNotification] = new.ongoingTimerNotification
            prefs[SettingsKeys.AlertSound] = new.alertSoundAndVibration
            prefs[SettingsKeys.Haptics] = new.hapticFeedback
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val d = UserPreferences()
        return UserPreferences(
            userName = this[SettingsKeys.UserName] ?: d.userName,
            onboardingCompleted = this[SettingsKeys.OnboardingCompleted] ?: false,
            pomodoro = PomodoroSettings(
                focusMinutes = this[SettingsKeys.FocusMinutes] ?: d.pomodoro.focusMinutes,
                shortBreakMinutes = this[SettingsKeys.ShortBreakMinutes] ?: d.pomodoro.shortBreakMinutes,
                longBreakMinutes = this[SettingsKeys.LongBreakMinutes] ?: d.pomodoro.longBreakMinutes,
                longBreakInterval = this[SettingsKeys.LongBreakInterval] ?: d.pomodoro.longBreakInterval,
            ),
            focusScreen = FocusScreenSettings(
                selectedBackgroundId = this[SettingsKeys.SelectedBackground] ?: BackgroundImage.FallbackId,
                showClock = this[SettingsKeys.ShowClock] ?: d.focusScreen.showClock,
                showMotivation = this[SettingsKeys.ShowMotivation] ?: d.focusScreen.showMotivation,
                customMotivation = this[SettingsKeys.CustomMotivation],
                showSessionDots = this[SettingsKeys.ShowSessionDots] ?: d.focusScreen.showSessionDots,
                showMissionTitle = this[SettingsKeys.ShowMissionTitle] ?: d.focusScreen.showMissionTitle,
                overlayAlpha = this[SettingsKeys.OverlayAlpha] ?: d.focusScreen.overlayAlpha,
                blurRadius = this[SettingsKeys.BlurRadius] ?: d.focusScreen.blurRadius,
            ),
            appearance = AppearanceSettings(
                themeMode = enumOrDefault(this[SettingsKeys.ThemeMode], ThemeMode.SYSTEM),
                dynamicColor = this[SettingsKeys.DynamicColor] ?: d.appearance.dynamicColor,
                paletteId = this[SettingsKeys.PaletteId] ?: d.appearance.paletteId,
                textScale = enumOrDefault(this[SettingsKeys.TextScale], TextScale.MEDIUM),
                reduceMotion = this[SettingsKeys.ReduceMotion] ?: d.appearance.reduceMotion,
            ),
            notifications = NotificationSettings(
                sessionAlerts = this[SettingsKeys.SessionAlerts] ?: d.notifications.sessionAlerts,
                ongoingTimerNotification = this[SettingsKeys.OngoingNotification] ?: d.notifications.ongoingTimerNotification,
                alertSoundAndVibration = this[SettingsKeys.AlertSound] ?: d.notifications.alertSoundAndVibration,
                hapticFeedback = this[SettingsKeys.Haptics] ?: d.notifications.hapticFeedback,
            ),
        )
    }
}

@Singleton
class TimerStateRepositoryImpl @Inject constructor(
    @TimerStore private val store: DataStore<Preferences>,
) : TimerStateRepository {

    override val state: Flow<TimerState> = store.data.safe().map { it.toTimerState() }.distinctUntilChanged()

    override suspend fun current(): TimerState = state.first()

    override suspend fun save(state: TimerState) {
        store.edit { p ->
            p[TimerKeys.Phase] = state.phase.name
            p[TimerKeys.SessionType] = state.sessionType.name
            if (state.missionId != null) p[TimerKeys.MissionId] = state.missionId else p.remove(TimerKeys.MissionId)
            p[TimerKeys.StartedAt] = state.startedAt
            p[TimerKeys.EndAt] = state.endAt
            p[TimerKeys.Planned] = state.plannedDurationMillis
            p[TimerKeys.RemainingPaused] = state.remainingWhenPausedMillis
            p[TimerKeys.PauseCount] = state.pauseCount
            p[TimerKeys.FocusCountInCycle] = state.focusCountInCycle
        }
    }

    private fun Preferences.toTimerState() = TimerState(
        phase = enumOrDefault(this[TimerKeys.Phase], TimerPhase.IDLE),
        sessionType = enumOrDefault(this[TimerKeys.SessionType], SessionType.FOCUS),
        missionId = this[TimerKeys.MissionId],
        startedAt = this[TimerKeys.StartedAt] ?: 0L,
        endAt = this[TimerKeys.EndAt] ?: 0L,
        plannedDurationMillis = this[TimerKeys.Planned] ?: 0L,
        remainingWhenPausedMillis = this[TimerKeys.RemainingPaused] ?: 0L,
        pauseCount = this[TimerKeys.PauseCount] ?: 0,
        focusCountInCycle = this[TimerKeys.FocusCountInCycle] ?: 0,
    )
}

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
) : SubscriptionRepository {
    override val isPremium: Flow<Boolean> = store.data.safe().map { it[SettingsKeys.PremiumActive] ?: false }.distinctUntilChanged()
    override val activePlanId: Flow<String?> = store.data.safe().map { it[SettingsKeys.PremiumPlan] }.distinctUntilChanged()
    override suspend fun setPremium(active: Boolean, planId: String?) {
        store.edit { prefs ->
            prefs[SettingsKeys.PremiumActive] = active
            when {
                !active -> {
                    prefs.remove(SettingsKeys.PremiumPlan)
                    prefs.remove(SettingsKeys.PremiumScheduledPlan)
                }
                planId != null -> {
                    prefs[SettingsKeys.PremiumPlan] = planId
                    // The scheduled switch has happened.
                    if (prefs[SettingsKeys.PremiumScheduledPlan] == planId) prefs.remove(SettingsKeys.PremiumScheduledPlan)
                }
                else -> Unit // still premium, plan unknown: keep what we had
            }
        }
    }

    override val scheduledPlanId: Flow<String?> =
        store.data.safe().map { it[SettingsKeys.PremiumScheduledPlan] }.distinctUntilChanged()

    override suspend fun setScheduledPlan(planId: String?) {
        store.edit { prefs ->
            if (planId == null) prefs.remove(SettingsKeys.PremiumScheduledPlan) else prefs[SettingsKeys.PremiumScheduledPlan] = planId
        }
    }
}
