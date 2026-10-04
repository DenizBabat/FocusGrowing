package com.focusgrowing.app.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.designsystem.theme.FocusPalette
import com.focusgrowing.app.core.designsystem.theme.FocusPalettes
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(
        val onboardingCompleted: Boolean,
        val appearance: AppearanceSettings,
        val palette: FocusPalette,
        val hapticsEnabled: Boolean,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    premium: PremiumManager,
    private val celebrationQueue: CelebrationQueue,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(settings.preferences, premium.premiumState) { prefs, isPremium ->
        val requested = FocusPalettes.byId(prefs.appearance.paletteId)
        // A premium palette falls back to the default when premium is not active.
        val palette = if (requested.isPremium && !isPremium) FocusPalettes.Default else requested
        MainUiState.Ready(
            onboardingCompleted = prefs.onboardingCompleted,
            appearance = prefs.appearance,
            palette = palette,
            hapticsEnabled = prefs.notifications.hapticFeedback,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    val celebration: StateFlow<Celebration?> = celebrationQueue.current
}
