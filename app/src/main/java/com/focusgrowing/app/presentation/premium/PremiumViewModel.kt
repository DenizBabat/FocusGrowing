package com.focusgrowing.app.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.billing.BillingProducts
import com.focusgrowing.app.core.billing.PurchaseManager
import com.focusgrowing.app.domain.logic.PremiumPricing
import com.focusgrowing.app.domain.model.ActiveSubscription
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.PlanChangeMode
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
    /** Live subscription from Google Play; null while loading, offline, or when not subscribed. */
    val active: ActiveSubscription? = null,
    /** Live answer from Google Play: the one-time lifetime product is owned. */
    val ownsLifetime: Boolean = false,
    /** Last known plan (product id), used when Play can't be reached. */
    val cachedPlanId: String? = null,
    /** Product the user switches to at the next renewal, if a deferred change is pending. */
    val scheduledPlanId: String? = null,
    val billing: BillingState = BillingState.Loading,
    val offers: List<PremiumOffer> = emptyList(),
    val selectedOfferId: String? = null,
    val processing: Boolean = false,
    val message: String? = null,
    val justPurchased: Boolean = false,
    val isTestMode: Boolean = false,
) {
    /** Selected offer. Preselects [DEFAULT_PLAN] when it is on sale, otherwise yearly. */
    val selectedOffer: PremiumOffer?
        get() = offers.firstOrNull { it.id == selectedOfferId }
            ?: offers.firstOrNull { it.period == DEFAULT_PLAN }
            ?: offers.firstOrNull { it.period == BillingPeriod.YEARLY }
            ?: offers.firstOrNull()

    val lifetimeOffer: PremiumOffer? get() = offers.firstOrNull { it.period == BillingPeriod.LIFETIME }

    /** Lifetime owner (live from Play, or the cached answer while offline). */
    val hasLifetime: Boolean get() = ownsLifetime || cachedPlanId == BillingProducts.LIFETIME

    val monthlyOffer: PremiumOffer? get() = offers.firstOrNull { it.period == BillingPeriod.MONTHLY }
    val yearlyOffer: PremiumOffer? get() = offers.firstOrNull { it.period == BillingPeriod.YEARLY }

    val currentProductId: String?
        get() = if (hasLifetime) BillingProducts.LIFETIME else active?.productId ?: cachedPlanId
    val currentPeriod: BillingPeriod get() = BillingProducts.periodOf(currentProductId)

    /** Price information of the plan the user owns (when the catalogue is loaded). */
    val currentOffer: PremiumOffer? get() = offers.firstOrNull { it.productId == currentProductId }

    /** The other plan, offered as a switch. Only when Play confirmed the subscription and nothing is scheduled. */
    val switchTarget: PremiumOffer?
        get() {
            val sub = active ?: return null
            if (hasLifetime || !sub.isAutoRenewing || scheduledPlanId != null) return null
            return offers.firstOrNull { it.productId != sub.productId && PremiumPricing.planChangeMode(sub.period, it.period) != null }
        }

    val switchMode: PlanChangeMode?
        get() = switchTarget?.let { target -> active?.let { PremiumPricing.planChangeMode(it.period, target.period) } }

    companion object {
        /** Which plan is preselected for new customers. Change to BillingPeriod.YEARLY to push the subscription instead. */
        val DEFAULT_PLAN = BillingPeriod.LIFETIME
    }
}

private data class LocalState(
    val selectedOfferId: String? = null,
    val processing: Boolean = false,
    val message: String? = null,
    val justPurchased: Boolean = false,
)

private data class Entitlement(val isPremium: Boolean, val cachedPlanId: String?, val scheduledPlanId: String?)
private data class StoreSnapshot(
    val billing: BillingState,
    val offers: List<PremiumOffer>,
    val active: ActiveSubscription?,
    val ownsLifetime: Boolean,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    premium: PremiumManager,
    subscriptions: SubscriptionRepository,
    private val store: PurchaseManager,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    private val entitlement = combine(premium.premiumState, subscriptions.activePlanId, subscriptions.scheduledPlanId) { p, plan, scheduled ->
        Entitlement(p, plan, scheduled)
    }
    private val storeSnapshot = combine(store.state, store.offers, store.activeSubscription, store.ownsLifetime) { billing, offers, active, lifetime ->
        StoreSnapshot(billing, offers, active, lifetime)
    }

    val uiState: StateFlow<PremiumUiState> = combine(entitlement, storeSnapshot, local) { e, s, l ->
        PremiumUiState(
            isPremium = e.isPremium,
            active = s.active,
            ownsLifetime = s.ownsLifetime,
            cachedPlanId = e.cachedPlanId,
            scheduledPlanId = e.scheduledPlanId,
            billing = s.billing,
            offers = s.offers,
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

    /** New subscription. [activity] is only used to show Google Play's purchase sheet; it isn't kept. */
    fun purchase(activity: Activity) {
        val offer = uiState.value.selectedOffer ?: return
        launch(activity, offer)
    }

    /** Monthly ↔ yearly for an existing subscriber. */
    fun switchPlan(activity: Activity) {
        val target = uiState.value.switchTarget ?: return
        launch(activity, target)
    }

    /** One-time purchase for an existing subscriber ("go lifetime"). */
    fun purchaseLifetime(activity: Activity) {
        val offer = uiState.value.lifetimeOffer ?: return
        launch(activity, offer)
    }

    /** Whether the user was already subscribed when the purchase sheet opened (new purchase vs plan change). */
    private var premiumAtLaunch = false
    private var launchedPeriod: BillingPeriod? = null

    private fun launch(activity: Activity, offer: PremiumOffer) {
        if (local.value.processing) return
        premiumAtLaunch = uiState.value.isPremium
        launchedPeriod = offer.period
        local.update { it.copy(processing = true, message = null) }
        val immediate = store.launchPurchase(activity, offer)
        if (immediate != null) showResult(immediate, fromPurchase = true)
    }

    fun restore() {
        if (local.value.processing) return
        local.update { it.copy(processing = true, message = null) }
        viewModelScope.launch { showResult(store.restore(), fromPurchase = false) }
    }

    /** Called when the user comes back from Google Play's subscription page. */
    fun refresh() = store.refreshAsync()

    fun manageSubscriptionUrl(): String = store.manageSubscriptionUrl()

    fun resetTestPurchase() {
        viewModelScope.launch {
            store.resetTestPurchase()
            local.update { LocalState() }
        }
    }

    fun toggleTestAutoRenew() {
        viewModelScope.launch { store.toggleTestAutoRenew() }
    }

    private fun showResult(result: PurchaseResult, fromPurchase: Boolean) {
        val wasPremium = premiumAtLaunch
        val (message, celebrate) = when (result) {
            PurchaseResult.Success -> when {
                !fromPurchase -> "Your Premium was restored." to false
                launchedPeriod == BillingPeriod.LIFETIME -> "Premium is yours forever. 🎉" to true
                wasPremium -> "Your plan was changed." to false
                else -> "Welcome to Premium! 🎉" to true
            }
            PurchaseResult.ChangeScheduled -> "Done. Your new plan starts when the current period ends." to false
            PurchaseResult.Pending -> "Your payment is being processed. Premium unlocks as soon as Google Play confirms it." to false
            PurchaseResult.Cancelled -> null to false
            is PurchaseResult.Failure -> result.message to false
        }
        local.update { it.copy(processing = false, message = message, justPurchased = celebrate) }
    }
}
