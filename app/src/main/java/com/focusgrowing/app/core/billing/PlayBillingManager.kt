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
import com.focusgrowing.app.domain.model.ActiveSubscription
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PlanChangeMode
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
 * Google Play Billing for Premium: two subscriptions and a one-time lifetime product ([BillingProducts]).
 *
 * Flow:
 * 1. [refresh] connects, loads all products (localized prices) and the user's purchases.
 * 2. [launchPurchase] opens Play's sheet. For a subscriber it becomes a plan change:
 *    monthly → yearly immediately (CHARGE_FULL_PRICE), yearly → monthly at the next renewal (DEFERRED).
 * 3. Play calls [onPurchasesUpdated]. Every purchase is signature-checked, acknowledged
 *    (otherwise Play refunds it after 3 days) and saved to [SubscriptionRepository],
 *    which drives all Premium checks in the app.
 * 4. On every app start / return to foreground purchases are re-read, so renewals, cancellations,
 *    refunds, plan switches and expiries are picked up automatically. If Play can't be reached,
 *    the cached state is kept.
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

    /** Every offer Play returned (the UI only sees the chosen one per product). */
    @Volatile
    private var allOffers: List<PremiumOffer> = emptyList()

    /** Purchase token of the active subscription; needed to replace it. Never logged or shown. */
    @Volatile
    private var activePurchaseToken: String? = null

    /** What the purchase sheet that is currently open is doing. */
    @Volatile
    private var pendingChange: PendingChange? = null

    private data class PendingChange(val targetProductId: String, val mode: PlanChangeMode?)

    private val _state = MutableStateFlow<BillingState>(BillingState.Loading)
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _offers = MutableStateFlow<List<PremiumOffer>>(emptyList())
    override val offers: StateFlow<List<PremiumOffer>> = _offers.asStateFlow()

    private val _activeSubscription = MutableStateFlow<ActiveSubscription?>(null)
    override val activeSubscription: StateFlow<ActiveSubscription?> = _activeSubscription.asStateFlow()

    private val _ownsLifetime = MutableStateFlow(false)
    override val ownsLifetime: StateFlow<Boolean> = _ownsLifetime.asStateFlow()

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

        if (_ownsLifetime.value) return PurchaseResult.Failure("You already own lifetime Premium.")
        if (offer.period == BillingPeriod.LIFETIME) return launchLifetimePurchase(activity, details, offer)

        val current = _activeSubscription.value
        val oldToken = activePurchaseToken
        val changeMode = current?.let { PremiumPricing.planChangeMode(it.period, offer.period) }
        if (current != null && changeMode == null) return PurchaseResult.Failure("You already have this plan.")

        // A plan change always uses the plain base plan (free trials are for new subscribers).
        val offerToUse = if (current != null) {
            allOffers.firstOrNull { it.productId == offer.productId && it.offerId == null } ?: offer
        } else {
            offer
        }

        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToUse.offerToken)
                        .build(),
                ),
            )

        if (current != null && changeMode != null) {
            if (oldToken == null) {
                refreshAsync()
                return PurchaseResult.Failure("Couldn't read your current subscription. Please try again in a moment.")
            }
            flow.setSubscriptionUpdateParams(replacementParams(oldToken, changeMode))
        }

        pendingChange = PendingChange(offer.productId, changeMode)
        return client.launchBillingFlow(activity, flow.build()).toLaunchResult()
    }

    /**
     * One-time purchase: Premium forever. It is independent of any subscription — an existing
     * subscription keeps renewing until the user cancels it in Google Play (the UI reminds them).
     */
    private fun launchLifetimePurchase(activity: Activity, details: ProductDetails, offer: PremiumOffer): PurchaseResult? {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (offer.offerToken.isNotEmpty()) product.setOfferToken(offer.offerToken)
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product.build())).build()
        pendingChange = PendingChange(offer.productId, null)
        return client.launchBillingFlow(activity, flow).toLaunchResult()
    }

    private fun BillingResult.toLaunchResult(): PurchaseResult? = when (responseCode) {
        BillingResponseCode.OK -> null // Play sheet is showing; the outcome arrives in onPurchasesUpdated.
        BillingResponseCode.ITEM_ALREADY_OWNED -> {
            refreshAsync()
            PurchaseResult.Success
        }
        BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
        else -> PurchaseResult.Failure(userMessage())
    }

    /**
     * Uses the classic replacement API: it is marked deprecated in favour of product-level
     * replacement params (meant for subscriptions with add-ons) but is fully supported for
     * swapping one subscription for another.
     */
    @Suppress("DEPRECATION")
    private fun replacementParams(oldPurchaseToken: String, mode: PlanChangeMode): BillingFlowParams.SubscriptionUpdateParams =
        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(oldPurchaseToken)
            .setSubscriptionReplacementMode(
                when (mode) {
                    PlanChangeMode.IMMEDIATE -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
                    PlanChangeMode.AT_NEXT_RENEWAL -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
                },
            )
            .build()

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

    override fun manageSubscriptionUrl(): String {
        val productId = _activeSubscription.value?.productId
        return if (productId != null) {
            "https://play.google.com/store/account/subscriptions?sku=$productId&package=${context.packageName}"
        } else {
            "https://play.google.com/store/account/subscriptions"
        }
    }

    // ---------------------------------------------------------------------------------------
    // Play callbacks
    // ---------------------------------------------------------------------------------------

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            val change = pendingChange
            pendingChange = null
            val outcome: PurchaseResult = when (result.responseCode) {
                BillingResponseCode.OK -> {
                    // Acknowledge whatever Play handed us, then read the real state back from Play.
                    purchases.orEmpty().forEach { acknowledgeIfNeeded(it) }
                    val sync = syncPurchases()
                    when {
                        sync == SyncOutcome.ACTIVE && change?.mode == PlanChangeMode.AT_NEXT_RENEWAL -> {
                            // Deferred switch: the current plan stays until the paid period ends.
                            if (_activeSubscription.value?.productId != change.targetProductId) {
                                subscriptions.setScheduledPlan(change.targetProductId)
                            }
                            PurchaseResult.ChangeScheduled
                        }
                        sync == SyncOutcome.ACTIVE -> PurchaseResult.Success
                        sync == SyncOutcome.PENDING -> PurchaseResult.Pending
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

    private suspend fun queryDetails(productIds: List<String>, type: String): Pair<BillingResult, List<ProductDetails>> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
                },
            )
            .build()
        return suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (continuation.isActive) continuation.resume(billingResult to queryResult.productDetailsList)
            }
        }
    }

    /** Subscriptions and one-time products have to be asked for separately. */
    private suspend fun loadOffers() {
        val (subsResult, subsDetails) = queryDetails(BillingProducts.subscriptions, BillingClient.ProductType.SUBS)
        val (oneTimeResult, oneTimeDetails) = queryDetails(listOf(BillingProducts.LIFETIME), BillingClient.ProductType.INAPP)

        if (subsResult.responseCode != BillingResponseCode.OK && oneTimeResult.responseCode != BillingResponseCode.OK) {
            _state.value = BillingState.Unavailable(subsResult.userMessage())
            return
        }
        val subs = if (subsResult.responseCode == BillingResponseCode.OK) subsDetails else emptyList()
        val oneTime = if (oneTimeResult.responseCode == BillingResponseCode.OK) oneTimeDetails else emptyList()

        productDetails.clear()
        (subs + oneTime).forEach { productDetails[it.productId] = it }

        val subscriptionOffers = subs.flatMap { details ->
            details.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
                val phases = offer.pricingPhases.pricingPhaseList
                val recurring = phases.lastOrNull() ?: return@mapNotNull null
                val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
                // Trust the product id for the plan type; fall back to the billing period Play reports.
                val period = BillingProducts.periodOf(details.productId)
                    .takeIf { it != BillingPeriod.OTHER }
                    ?: PremiumPricing.periodOf(recurring.billingPeriod)
                PremiumOffer(
                    productId = details.productId,
                    basePlanId = offer.basePlanId,
                    offerId = offer.offerId,
                    offerToken = offer.offerToken,
                    period = period,
                    formattedPrice = recurring.formattedPrice,
                    priceMicros = recurring.priceAmountMicros,
                    currencyCode = recurring.priceCurrencyCode,
                    freeTrial = trialPhase?.let { IsoPeriod.parse(it.billingPeriod) },
                )
            }
        }
        val lifetimeOffers = oneTime.mapNotNull { details ->
            val price = details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: details.oneTimePurchaseOfferDetails
                ?: return@mapNotNull null
            PremiumOffer(
                productId = details.productId,
                basePlanId = "",
                offerId = null,
                offerToken = price.offerToken.orEmpty(),
                period = BillingPeriod.LIFETIME,
                formattedPrice = price.formattedPrice,
                priceMicros = price.priceAmountMicros,
                currencyCode = price.priceCurrencyCode,
                freeTrial = null,
            )
        }

        allOffers = subscriptionOffers + lifetimeOffers
        _offers.value = PremiumPricing.chooseOffers(allOffers)
        _state.value = if (_offers.value.isEmpty()) {
            BillingState.Unavailable("Premium isn't available in your country or on this Google Play account yet.")
        } else {
            BillingState.Ready
        }
    }

    private suspend fun queryPurchases(type: String): Pair<BillingResult, List<Purchase>> {
        val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(params) { billingResult, list ->
                if (continuation.isActive) continuation.resume(billingResult to list)
            }
        }
    }

    private fun Purchase.isValidAndPaid(): Boolean =
        purchaseState == Purchase.PurchaseState.PURCHASED &&
            PurchaseVerifier.isValid(BillingConfig.PLAY_LICENSE_KEY, originalJson, signature)

    /** Re-reads the lifetime purchase and subscriptions from Play and updates the cached entitlement. */
    private suspend fun syncPurchases(): SyncOutcome {
        val (subsResult, subsPurchases) = queryPurchases(BillingClient.ProductType.SUBS)
        val (oneTimeResult, oneTimePurchases) = queryPurchases(BillingClient.ProductType.INAPP)
        if (subsResult.responseCode != BillingResponseCode.OK || oneTimeResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases failed: ${subsResult.responseCode} / ${oneTimeResult.responseCode}")
            return SyncOutcome.ERROR // keep the cached entitlement (e.g. offline)
        }

        val premiumSubs = subsPurchases.filter { purchase -> purchase.products.any(BillingProducts::isSubscription) }
        val lifetimePurchases = oneTimePurchases.filter { BillingProducts.LIFETIME in it.products }

        // Subscription (may exist next to a lifetime purchase until the user cancels it).
        val validSubs = premiumSubs.filter { it.isValidAndPaid() }
        validSubs.forEach { acknowledgeIfNeeded(it) }
        // Normally there is exactly one. If both exist for a moment during a switch, prefer yearly.
        val activeSub = validSubs.firstOrNull { BillingProducts.YEARLY in it.products } ?: validSubs.firstOrNull()
        if (activeSub != null) {
            val productId = activeSub.products.first(BillingProducts::isSubscription)
            activePurchaseToken = activeSub.purchaseToken
            _activeSubscription.value = ActiveSubscription(productId, BillingProducts.periodOf(productId), activeSub.isAutoRenewing)
        } else {
            activePurchaseToken = null
            _activeSubscription.value = null
        }

        // Lifetime: a non-consumable one-time product. It is never consumed, only acknowledged.
        val lifetime = lifetimePurchases.firstOrNull { it.isValidAndPaid() }
        lifetime?.let { acknowledgeIfNeeded(it) }
        _ownsLifetime.value = lifetime != null

        return when {
            lifetime != null -> {
                subscriptions.setPremium(true, BillingProducts.LIFETIME)
                SyncOutcome.ACTIVE
            }
            activeSub != null -> {
                subscriptions.setPremium(true, activeSub.products.first(BillingProducts::isSubscription))
                SyncOutcome.ACTIVE
            }
            else -> {
                subscriptions.setPremium(false)
                val pending = (premiumSubs + lifetimePurchases).any { it.purchaseState == Purchase.PurchaseState.PENDING }
                if (pending) SyncOutcome.PENDING else SyncOutcome.NONE
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        if (!PurchaseVerifier.isValid(BillingConfig.PLAY_LICENSE_KEY, purchase.originalJson, purchase.signature)) {
            Log.w(TAG, "Rejected purchase with an invalid signature")
            return
        }
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
