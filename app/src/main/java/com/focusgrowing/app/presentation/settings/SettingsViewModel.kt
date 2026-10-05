package com.focusgrowing.app.presentation.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.R
import com.focusgrowing.app.core.ads.AdsManager
import com.focusgrowing.app.core.ads.ConsentManager
import com.focusgrowing.app.core.ads.RewardedResult
import com.focusgrowing.app.core.locale.StringProvider
import com.focusgrowing.app.core.timer.FocusTimerManager
import com.focusgrowing.app.domain.logic.PaletteAccess
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.FocusScreenSettings
import com.focusgrowing.app.domain.model.NotificationSettings
import com.focusgrowing.app.domain.model.PaletteTrial
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.UserPreferences
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.RewardRepository
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isPremium: Boolean = false,
    /** Selected focus background; the screen shows its (translated) name. */
    val background: BackgroundImage? = null,
    /** Premium palette currently unlocked by a rewarded ad (null when none or expired). */
    val trialPaletteId: String? = null,
    val trialHoursLeft: Int = 0,
    /** A rewarded ad is loading or playing. */
    val adInProgress: Boolean = false,
    val adMessage: String? = null,
    /** The user is in a region where the ad consent choice must stay changeable. */
    val showAdPrivacyOptions: Boolean = false,
)

private data class AdUiState(val inProgress: Boolean = false, val message: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val premium: PremiumManager,
    private val timer: FocusTimerManager,
    private val rewards: RewardRepository,
    private val ads: AdsManager,
    private val consent: ConsentManager,
    private val time: TimeProvider,
    private val strings: StringProvider,
    backgrounds: BackgroundRepository,
) : ViewModel() {

    private val background = settings.preferences
        .map { it.focusScreen.selectedBackgroundId }
        .flatMapLatest { backgrounds.observeBackground(it) }

    private val adState = MutableStateFlow(AdUiState())

    /** Trial + consent requirement, bundled so the main combine stays at five inputs. */
    private val adContext = combine(rewards.paletteTrial, consent.privacyOptionsRequired) { trial, privacy -> trial to privacy }

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.preferences, premium.premiumState, background, adContext, adState) { prefs, isPremium, bg, context, ad ->
            val (trial, privacyRequired) = context
            val now = time.now()
            val active = trial?.takeIf { PaletteAccess.isTrialActive(it, now) }
            SettingsUiState(
                prefs = prefs,
                isPremium = isPremium,
                background = bg,
                trialPaletteId = active?.paletteId,
                trialHoursLeft = PaletteAccess.hoursLeft(active, now),
                adInProgress = ad.inProgress,
                adMessage = ad.message,
                showAdPrivacyOptions = privacyRequired && !isPremium,
            )
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

    /**
     * Rewarded ad: watching it to the end unlocks [paletteId] for 24 hours and applies it.
     * [activity] is only used to show the ad; it isn't kept.
     */
    fun watchAdForPalette(activity: Activity, paletteId: String, paletteName: String) {
        if (adState.value.inProgress) return
        adState.value = AdUiState(inProgress = true)
        ads.showRewarded(activity) { result ->
            when (result) {
                RewardedResult.EARNED -> viewModelScope.launch {
                    rewards.setPaletteTrial(PaletteAccess.newTrial(paletteId, time.now()))
                    settings.updateAppearance { it.copy(paletteId = paletteId) }
                    adState.value = AdUiState(message = strings.get(R.string.settings_ad_palette_unlocked, paletteName))
                }
                RewardedResult.DISMISSED ->
                    adState.value = AdUiState(message = strings.get(R.string.settings_ad_watch_to_end))
                RewardedResult.UNAVAILABLE ->
                    adState.value = AdUiState(message = strings.get(R.string.settings_ad_unavailable))
            }
        }
    }

    fun clearAdMessage() = adState.update { it.copy(message = null) }

    /** Opens Google's form where the user can change their ad consent choice. */
    fun showAdPrivacyOptions(activity: Activity) = consent.showPrivacyOptions(activity) {}
}
