package com.focusgrowing.app.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.designsystem.theme.FocusPalette
import com.focusgrowing.app.core.designsystem.theme.FocusPalettes
import com.focusgrowing.app.domain.logic.PaletteAccess
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.RewardRepository
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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
    rewards: RewardRepository,
    time: TimeProvider,
    private val celebrationQueue: CelebrationQueue,
) : ViewModel() {

    /** Re-checks once a minute so a 24-hour palette trial ends while the app is open. */
    private val clock = flow {
        while (true) {
            emit(time.now())
            delay(60_000)
        }
    }

    val uiState: StateFlow<MainUiState> =
        combine(settings.preferences, premium.premiumState, rewards.paletteTrial, clock) { prefs, isPremium, trial, now ->
            val requested = FocusPalettes.byId(prefs.appearance.paletteId)
            // A premium palette falls back to the default without Premium or an active rewarded-ad trial.
            val allowed = PaletteAccess.canUse(requested.id, requested.isPremium, isPremium, trial, now)
            MainUiState.Ready(
                onboardingCompleted = prefs.onboardingCompleted,
                appearance = prefs.appearance,
                palette = if (allowed) requested else FocusPalettes.Default,
                hapticsEnabled = prefs.notifications.hapticFeedback,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    val celebration: StateFlow<Celebration?> = celebrationQueue.current
}
