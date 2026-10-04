package com.focusgrowing.app.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.logic.PremiumPricing
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PremiumOffer
import com.focusgrowing.app.domain.model.PurchaseResult
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing for the Premium subscription.
 *
 * Flow:
 * 1. [refresh] connects, loads the subscription's base plans (localized prices) and the user's purchases.
 * 2. [launchPurchase] opens Play's purchase sheet; Play calls [onPurchasesUpdated] with the result.
 * 3. Every purchase is signature-checked, acknowledged (otherwise Play refunds it after 3 days)
 *    and saved to [SubscriptionRepository], which drives all Premium checks in the app.
 * 4. On every app start / return to foreground purchases are re-read, so renewals, cancellations,
 *    refunds and expiries are picked up automatically. If Play can't be reached, the cached state is kept.
 */
@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptions: SubscriptionRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : PurchaseManager, PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val connectMutex = Mutex()
    private val refreshMutex = Mutex()
    private val productDetails = mutableMapOf<String, ProductDetails>()

    /** Base plan of the purchase sheet currently open. Play's Purchase object doesn't carry it. */
    @Volatile
    private var launchedBasePlanId: String? = null

    private val _state = MutableStateFlow<BillingState>(BillingState.Loading)
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _offers = MutableStateFlow<List<PremiumOffer>>(emptyList())
    override val offers: StateFlow<List<PremiumOffer>> = _offers.asStateFlow()

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 4)
    override val purchaseResults: SharedFlow<PurchaseResult> = _purchaseResults.asSharedFlow()

    override val isTestMode: Boolean = false

    override fun refreshAsync() {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        refreshMutex.withLock {
            val connection = connect()
            if (connection.responseCode != BillingResponseCode.OK) {
                _state.value = BillingState.Unavailable(connection.userMessage())
                return
            }
            loadOffers()
            syncPurchases()
        }
    }

    override fun launchPurchase(activity: Activity, offer: PremiumOffer): PurchaseResult? {
        val details = productDetails[offer.productId]
        if (!client.isReady || details == null) {
            refreshAsync()
            return PurchaseResult.Failure("Google Play is not ready yet. Please try again in a moment.")
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build(),
                ),
            )
            .build()
        launchedBasePlanId = offer.basePlanId
        val result = client.launchBillingFlow(activity, params)
        return when (result.responseCode) {
            BillingResponseCode.OK -> null // Play sheet is showing; the outcome arrives in onPurchasesUpdated.
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                refreshAsync()
                PurchaseResult.Success
            }
            BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
            else -> PurchaseResult.Failure(result.userMessage())
        }
    }

    override suspend fun restore(): PurchaseResult {
        val connection = connect()
        if (connection.responseCode != BillingResponseCode.OK) return PurchaseResult.Failure(connection.userMessage())
        return when (syncPurchases()) {
            SyncOutcome.ACTIVE -> PurchaseResult.Success
            SyncOutcome.PENDING -> PurchaseResult.Pending
            SyncOutcome.NONE -> PurchaseResult.Failure("No active Premium subscription was found for this Google account.")
            SyncOutcome.ERROR -> PurchaseResult.Failure("Couldn't reach Google Play. Check your connection and try again.")
        }
    }

    override fun manageSubscriptionUrl(): String =
        "https://play.google.com/store/account/subscriptions?sku=${BillingConfig.PREMIUM_SUBSCRIPTION_ID}&package=${context.packageName}"

    // ---------------------------------------------------------------------------------------
    // Play callbacks
    // ---------------------------------------------------------------------------------------

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            val outcome: PurchaseResult = when (result.responseCode) {
                BillingResponseCode.OK -> {
                    val list = purchases.orEmpty()
                    var granted = false
                    var pending = false
                    list.forEach { purchase ->
                        when (handlePurchase(purchase)) {
                            SyncOutcome.ACTIVE -> granted = true
                            SyncOutcome.PENDING -> pending = true
                            else -> Unit
                        }
                    }
                    when {
                        granted -> PurchaseResult.Success
                        pending -> PurchaseResult.Pending
                        else -> PurchaseResult.Failure("The purchase couldn't be verified. If you were charged, tap “Restore purchase”.")
                    }
                }
                BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
                BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    syncPurchases()
                    PurchaseResult.Success
                }
                else -> PurchaseResult.Failure(result.userMessage())
            }
            _purchaseResults.emit(outcome)
        }
    }

    // ---------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------

    private enum class SyncOutcome { ACTIVE, PENDING, NONE, ERROR }

    /** Connects once; with auto-reconnection enabled, later calls reconnect by themselves. */
    private suspend fun connect(): BillingResult = connectMutex.withLock {
        if (client.isReady) return@withLock okResult()
        withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine<BillingResult> { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(billingResult)
                    }

                    override fun onBillingServiceDisconnected() {
                        // Auto-reconnection handles this; nothing to do.
                    }
                })
            }
        } ?: BillingResult.newBuilder()
            .setResponseCode(BillingResponseCode.SERVICE_UNAVAILABLE)
            .setDebugMessage("Connection timeout")
            .build()
    }

    private suspend fun loadOffers() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConfig.PREMIUM_SUBSCRIPTION_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        val (result, detailsList) = suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (continuation.isActive) continuation.resume(billingResult to queryResult.productDetailsList)
            }
        }

        if (result.responseCode != BillingResponseCode.OK) {
            _state.value = BillingState.Unavailable(result.userMessage())
            return
        }
        productDetails.clear()
        detailsList.forEach { productDetails[it.productId] = it }

        val candidates = detailsList.flatMap { details ->
            details.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
                val phases = offer.pricingPhases.pricingPhaseList
                val recurring = phases.lastOrNull() ?: return@mapNotNull null
                val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
                PremiumOffer(
                    productId = details.productId,
                    basePlanId = offer.basePlanId,
                    offerId = offer.offerId,
                    offerToken = offer.offerToken,
                    period = PremiumPricing.periodOf(recurring.billingPeriod),
                    formattedPrice = recurring.formattedPrice,
                    priceMicros = recurring.priceAmountMicros,
                    currencyCode = recurring.priceCurrencyCode,
                    freeTrial = trialPhase?.let { IsoPeriod.parse(it.billingPeriod) },
                )
            }
        }
        _offers.value = PremiumPricing.chooseOffers(candidates)
        _state.value = if (_offers.value.isEmpty()) {
            BillingState.Unavailable("Premium isn't available in your country or on this Google Play account yet.")
        } else {
            BillingState.Ready
        }
    }

    /** Re-reads active subscriptions from Play and updates the cached entitlement. */
    private suspend fun syncPurchases(): SyncOutcome {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val (result, purchases) = suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
            client.queryPurchasesAsync(params) { billingResult, list ->
                if (continuation.isActive) continuation.resume(billingResult to list)
            }
        }
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases failed: ${result.responseCode}")
            return SyncOutcome.ERROR // keep the cached entitlement (e.g. offline)
        }
        var outcome = SyncOutcome.NONE
        for (purchase in purchases) {
            when (handlePurchase(purchase, updateCacheWhenInactive = false)) {
                SyncOutcome.ACTIVE -> outcome = SyncOutcome.ACTIVE
                SyncOutcome.PENDING -> if (outcome != SyncOutcome.ACTIVE) outcome = SyncOutcome.PENDING
                else -> Unit
            }
        }
        if (outcome != SyncOutcome.ACTIVE) subscriptions.setPremium(false)
        return outcome
    }

    private suspend fun handlePurchase(purchase: Purchase, updateCacheWhenInactive: Boolean = true): SyncOutcome {
        if (BillingConfig.PREMIUM_SUBSCRIPTION_ID !in purchase.products) return SyncOutcome.NONE
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!PurchaseVerifier.isValid(BillingConfig.PLAY_LICENSE_KEY, purchase.originalJson, purchase.signature)) {
                    Log.w(TAG, "Rejected purchase with an invalid signature")
                    return SyncOutcome.NONE
                }
                if (!purchase.isAcknowledged) acknowledge(purchase)
                // New purchase: remember the plan the user picked. Periodic sync: keep the cached plan.
                subscriptions.setPremium(true, if (updateCacheWhenInactive) launchedBasePlanId else null)
                SyncOutcome.ACTIVE
            }
            Purchase.PurchaseState.PENDING -> SyncOutcome.PENDING
            else -> {
                if (updateCacheWhenInactive) subscriptions.setPremium(false)
                SyncOutcome.NONE
            }
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val result = suspendCancellableCoroutine<BillingResult> { continuation ->
            client.acknowledgePurchase(params) { billingResult ->
                if (continuation.isActive) continuation.resume(billingResult)
            }
        }
        // If this fails, the next sync tries again (Play allows 3 days).
        if (result.responseCode != BillingResponseCode.OK) Log.w(TAG, "acknowledge failed: ${result.responseCode}")
    }

    private fun okResult(): BillingResult = BillingResult.newBuilder().setResponseCode(BillingResponseCode.OK).build()

    private fun BillingResult.userMessage(): String = when (responseCode) {
        BillingResponseCode.BILLING_UNAVAILABLE ->
            "Google Play purchases aren't available on this device. Make sure the Play Store is installed and you're signed in."
        BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.SERVICE_DISCONNECTED, BillingResponseCode.NETWORK_ERROR ->
            "Couldn't reach Google Play. Check your connection and try again."
        BillingResponseCode.ITEM_UNAVAILABLE -> "This plan isn't available right now."
        BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Please update the Google Play Store app and try again."
        BillingResponseCode.DEVELOPER_ERROR -> "Purchases aren't set up correctly yet. Please try again later."
        else -> "Something went wrong with Google Play. Please try again."
    }

    private companion object {
        const val TAG = "PlayBilling"
        const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
