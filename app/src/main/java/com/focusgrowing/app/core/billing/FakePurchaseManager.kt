package com.focusgrowing.app.core.billing

import android.app.Activity
import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PremiumOffer
import com.focusgrowing.app.domain.model.PurchaseResult
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulated store for DEBUG builds only (BuildConfig.FAKE_BILLING). No money moves.
 * Lets you test the Premium screen, the locked features and "restore" without Google Play.
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
                productId = BillingConfig.PREMIUM_SUBSCRIPTION_ID,
                basePlanId = BillingConfig.BASE_PLAN_MONTHLY,
                offerId = null,
                offerToken = "fake-monthly",
                period = BillingPeriod.MONTHLY,
                formattedPrice = "₺49,99",
                priceMicros = 49_990_000,
                currencyCode = "TRY",
                freeTrial = null,
            ),
            PremiumOffer(
                productId = BillingConfig.PREMIUM_SUBSCRIPTION_ID,
                basePlanId = BillingConfig.BASE_PLAN_YEARLY,
                offerId = "trial",
                offerToken = "fake-yearly",
                period = BillingPeriod.YEARLY,
                formattedPrice = "₺399,99",
                priceMicros = 399_990_000,
                currencyCode = "TRY",
                freeTrial = IsoPeriod(days = 7),
            ),
        )
        _state.value = BillingState.Ready
    }

    override fun launchPurchase(activity: Activity, offer: PremiumOffer): PurchaseResult? {
        scope.launch {
            delay(700)
            subscriptions.setPremium(true, offer.basePlanId)
            _purchaseResults.emit(PurchaseResult.Success)
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
    }
}
