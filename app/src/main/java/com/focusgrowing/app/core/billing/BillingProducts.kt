package com.focusgrowing.app.core.billing

import com.focusgrowing.app.domain.model.BillingPeriod

/**
 * The products sold in Google Play. The ids must match Play Console exactly.
 *
 * - [MONTHLY], [YEARLY]: Monetize with Play → Products → Subscriptions
 * - [LIFETIME]: Monetize with Play → Products → One-time products (bought once, kept forever)
 *
 * Two separate subscription products (instead of one product with two base plans) let the app always
 * know which plan the user owns — Google Play tells the app the product id of a purchase, but not its base plan.
 *
 * (The licensing key stays in BillingConfig.PLAY_LICENSE_KEY.)
 */
object BillingProducts {
    const val MONTHLY = "premium_monthly"
    const val YEARLY = "premium_yearly"
    const val LIFETIME = "premium_lifetime"

    val subscriptions: List<String> = listOf(MONTHLY, YEARLY)
    val all: List<String> = subscriptions + LIFETIME

    fun isSubscription(productId: String): Boolean = productId in subscriptions

    fun periodOf(productId: String?): BillingPeriod = when (productId) {
        MONTHLY -> BillingPeriod.MONTHLY
        YEARLY -> BillingPeriod.YEARLY
        LIFETIME -> BillingPeriod.LIFETIME
        else -> BillingPeriod.OTHER
    }
}
