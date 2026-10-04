package com.focusgrowing.app.core.billing

import android.app.Activity
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.PremiumOffer
import com.focusgrowing.app.domain.model.PurchaseResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The store. Real implementation: [PlayBillingManager]. Debug builds use [FakePurchaseManager].
 * Purchases complete asynchronously (Play shows its own sheet), so results arrive on [purchaseResults].
 */
interface PurchaseManager {
    val state: StateFlow<BillingState>
    val offers: StateFlow<List<PremiumOffer>>
    val purchaseResults: SharedFlow<PurchaseResult>

    /** True for the simulated debug store. */
    val isTestMode: Boolean

    /** Connects and syncs purchases in the background. Safe to call many times. */
    fun refreshAsync()

    /** Reloads offers and purchases; returns once done. */
    suspend fun refresh()

    /**
     * Opens the Google Play purchase sheet. Returns a result only if the sheet could not be
     * shown; otherwise the outcome is emitted on [purchaseResults].
     */
    fun launchPurchase(activity: Activity, offer: PremiumOffer): PurchaseResult?

    /** Asks Play for existing purchases (new phone, reinstall). */
    suspend fun restore(): PurchaseResult

    /** Google Play page where the user can cancel or change the subscription. */
    fun manageSubscriptionUrl(): String

    /** Debug only: forget the simulated purchase. */
    suspend fun resetTestPurchase() {}
}
