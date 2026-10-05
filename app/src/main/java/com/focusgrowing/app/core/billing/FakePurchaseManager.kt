package com.focusgrowing.app.core.billing

import android.app.Activity
import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.logic.PremiumPricing
import com.focusgrowing.app.domain.model.ActiveSubscription
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PlanChangeMode
import com.focusgrowing.app.domain.model.PremiumOffer
import com.focusgrowing.app.domain.model.PurchaseResult
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulated store for the DEBUG build type only (BuildConfig.FAKE_BILLING). No money moves.
 * Behaves like Google Play: buy, switch plan (yearly now / monthly at renewal), buy lifetime, cancel, restore.
 */
@Singleton
class FakePurchaseManager @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : PurchaseManager {

    private val _state = MutableStateFlow<BillingState>(BillingState.Loading)
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _offers = MutableStateFlow<List<PremiumOffer>>(emptyList())
    override val offers: StateFlow<List<PremiumOffer>> = _offers.asStateFlow()

    private val autoRenewing = MutableStateFlow(true)

    /** A subscription that is still running next to a lifetime purchase (until "cancelled"). */
    private val lingeringSubscription = MutableStateFlow<String?>(null)

    override val activeSubscription: StateFlow<ActiveSubscription?> =
        combine(subscriptions.isPremium, subscriptions.activePlanId, autoRenewing, lingeringSubscription) { premium, plan, renew, lingering ->
            val subProduct = when {
                !premium -> null
                plan == BillingProducts.LIFETIME -> lingering
                else -> plan ?: BillingProducts.MONTHLY
            }
            subProduct?.let { ActiveSubscription(it, BillingProducts.periodOf(it), renew) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override val ownsLifetime: StateFlow<Boolean> =
        combine(subscriptions.isPremium, subscriptions.activePlanId) { premium, plan ->
            premium && plan == BillingProducts.LIFETIME
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 4)
    override val purchaseResults: SharedFlow<PurchaseResult> = _purchaseResults.asSharedFlow()

    override val isTestMode: Boolean = true

    override fun refreshAsync() {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        delay(300)
        _offers.value = listOf(
            PremiumOffer(
                productId = BillingProducts.MONTHLY,
                basePlanId = "monthly",
                offerId = null,
                offerToken = "fake-monthly",
                period = BillingPeriod.MONTHLY,
                formattedPrice = "₺49,99",
                priceMicros = 49_990_000,
                currencyCode = "TRY",
                freeTrial = null,
            ),
            PremiumOffer(
                productId = BillingProducts.YEARLY,
                basePlanId = "yearly",
                offerId = "trial",
                offerToken = "fake-yearly",
                period = BillingPeriod.YEARLY,
                formattedPrice = "₺399,99",
                priceMicros = 399_990_000,
                currencyCode = "TRY",
                freeTrial = IsoPeriod(days = 7),
            ),
            PremiumOffer(
                productId = BillingProducts.LIFETIME,
                basePlanId = "",
                offerId = null,
                offerToken = "fake-lifetime",
                period = BillingPeriod.LIFETIME,
                formattedPrice = "₺999,99",
                priceMicros = 999_990_000,
                currencyCode = "TRY",
                freeTrial = null,
            ),
        )
        _state.value = BillingState.Ready
    }

    override fun launchPurchase(activity: Activity, offer: PremiumOffer): PurchaseResult? {
        if (ownsLifetime.value) return PurchaseResult.Failure("You already own lifetime Premium.")
        val current = activeSubscription.value
        if (offer.period == BillingPeriod.LIFETIME) {
            scope.launch {
                delay(700)
                // Like in Google Play, a running subscription is NOT cancelled by buying lifetime.
                lingeringSubscription.value = current?.productId
                subscriptions.setPremium(true, BillingProducts.LIFETIME)
                _purchaseResults.emit(PurchaseResult.Success)
            }
            return null
        }
        val mode = current?.let { PremiumPricing.planChangeMode(it.period, offer.period) }
        if (current != null && mode == null) return PurchaseResult.Failure("You already have this plan.")
        scope.launch {
            delay(700)
            autoRenewing.value = true
            if (mode == PlanChangeMode.AT_NEXT_RENEWAL) {
                subscriptions.setScheduledPlan(offer.productId)
                _purchaseResults.emit(PurchaseResult.ChangeScheduled)
            } else {
                subscriptions.setScheduledPlan(null)
                subscriptions.setPremium(true, offer.productId)
                _purchaseResults.emit(PurchaseResult.Success)
            }
        }
        return null
    }

    override suspend fun restore(): PurchaseResult {
        delay(400)
        return if (subscriptions.isPremium.first()) PurchaseResult.Success
        else PurchaseResult.Failure("No active Premium subscription was found (test mode).")
    }

    override fun manageSubscriptionUrl(): String = "https://play.google.com/store/account/subscriptions"

    override suspend fun resetTestPurchase() {
        subscriptions.setPremium(false)
        lingeringSubscription.value = null
        autoRenewing.value = true
    }

    override suspend fun toggleTestAutoRenew() {
        autoRenewing.value = !autoRenewing.value
    }
}
