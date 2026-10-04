package com.focusgrowing.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.timer.FocusTimerManager
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.FocusScreenSettings
import com.focusgrowing.app.domain.model.NotificationSettings
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.UserPreferences
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isPremium: Boolean = false,
    val backgroundName: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val premium: PremiumManager,
    private val timer: FocusTimerManager,
    backgrounds: BackgroundRepository,
) : ViewModel() {

    private val backgroundName = settings.preferences
        .map { it.focusScreen.selectedBackgroundId }
        .flatMapLatest { backgrounds.observeBackground(it) }
        .map { it?.name.orEmpty() }

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.preferences, premium.premiumState, backgroundName) { prefs, isPremium, bg ->
            SettingsUiState(prefs, isPremium, bg)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun canScheduleExactAlarms(): Boolean = timer.canScheduleExactAlarms()

    fun canUse(feature: PremiumFeature) = premium.hasAccess(feature)

    fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { settings.updateAppearance(transform) }
    }

    fun updatePomodoro(transform: (PomodoroSettings) -> PomodoroSettings) {
        viewModelScope.launch { settings.updatePomodoro(transform) }
    }

    fun updateFocusScreen(transform: (FocusScreenSettings) -> FocusScreenSettings) {
        viewModelScope.launch { settings.updateFocusScreen(transform) }
    }

    fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { settings.updateNotifications(transform) }
    }
}
