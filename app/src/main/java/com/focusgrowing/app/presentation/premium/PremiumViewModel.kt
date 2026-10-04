package com.focusgrowing.app.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.billing.PurchaseManager
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.PremiumOffer
import com.focusgrowing.app.domain.model.PurchaseResult
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val activePlanId: String? = null,
    val billing: BillingState = BillingState.Loading,
    val offers: List<PremiumOffer> = emptyList(),
    val selectedOfferId: String? = null,
    val processing: Boolean = false,
    val message: String? = null,
    val justPurchased: Boolean = false,
    val isTestMode: Boolean = false,
) {
    /** Selected offer, defaulting to the yearly plan (best value) like most subscription apps. */
    val selectedOffer: PremiumOffer?
        get() = offers.firstOrNull { it.id == selectedOfferId }
            ?: offers.firstOrNull { it.period == BillingPeriod.YEARLY }
            ?: offers.firstOrNull()

    val monthlyOffer: PremiumOffer? get() = offers.firstOrNull { it.period == BillingPeriod.MONTHLY }
}

private data class LocalState(
    val selectedOfferId: String? = null,
    val processing: Boolean = false,
    val message: String? = null,
    val justPurchased: Boolean = false,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    premium: PremiumManager,
    subscriptions: SubscriptionRepository,
    private val store: PurchaseManager,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<PremiumUiState> = combine(
        premium.premiumState,
        subscriptions.activePlanId,
        store.state,
        store.offers,
        local,
    ) { isPremium, plan, billing, offers, l ->
        PremiumUiState(
            isPremium = isPremium,
            activePlanId = plan,
            billing = billing,
            offers = offers,
            selectedOfferId = l.selectedOfferId,
            processing = l.processing,
            message = l.message,
            justPurchased = l.justPurchased,
            isTestMode = store.isTestMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PremiumUiState(isTestMode = store.isTestMode))

    init {
        store.refreshAsync()
        viewModelScope.launch {
            store.purchaseResults.collect { result -> showResult(result, fromPurchase = true) }
        }
    }

    fun selectOffer(offer: PremiumOffer) = local.update { it.copy(selectedOfferId = offer.id) }

    /** [activity] is only used to show Google Play's purchase sheet; it isn't kept. */
    fun purchase(activity: Activity) {
        val offer = uiState.value.selectedOffer ?: return
        if (local.value.processing) return
        local.update { it.copy(processing = true, message = null) }
        val immediate = store.launchPurchase(activity, offer)
        if (immediate != null) showResult(immediate, fromPurchase = true)
    }

    fun restore() {
        if (local.value.processing) return
        local.update { it.copy(processing = true, message = null) }
        viewModelScope.launch { showResult(store.restore(), fromPurchase = false) }
    }

    fun retry() = store.refreshAsync()

    fun manageSubscriptionUrl(): String = store.manageSubscriptionUrl()

    fun resetTestPurchase() {
        viewModelScope.launch {
            store.resetTestPurchase()
            local.update { LocalState() }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    private fun showResult(result: PurchaseResult, fromPurchase: Boolean) {
        val (message, success) = when (result) {
            PurchaseResult.Success -> (if (fromPurchase) "Welcome to Premium! 🎉" else "Your Premium subscription was restored.") to true
            PurchaseResult.Pending -> "Your payment is being processed. Premium unlocks as soon as Google Play confirms it." to false
            PurchaseResult.Cancelled -> null to false
            is PurchaseResult.Failure -> result.message to false
        }
        local.update { it.copy(processing = false, message = message, justPurchased = success && fromPurchase) }
    }
}
