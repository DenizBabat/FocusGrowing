package com.focusgrowing.app.domain.repository

import com.focusgrowing.app.domain.model.PremiumFeature
import kotlinx.coroutines.flow.StateFlow

/**
 * Single entry point for "may the user use X?". UI and use cases never read billing
 * state directly.
 *
 * Google Play ─► PurchaseManager (core/billing) ─► SubscriptionRepository ─► PremiumManager ─► UI
 */
interface PremiumManager {
    val premiumState: StateFlow<Boolean>
    fun isPremium(): Boolean = premiumState.value
    fun hasAccess(feature: PremiumFeature): Boolean = premiumState.value
}
