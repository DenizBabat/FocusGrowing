package com.focusgrowing.app.domain.model

enum class PremiumFeature {
    UNLIMITED_BACKGROUNDS,
    ADVANCED_CROP,
    BACKGROUND_BLUR,
    CUSTOM_MOTIVATION,
    ADVANCED_STATISTICS,
    ADVANCED_INSIGHTS,
    PREMIUM_PALETTES,
    PREMIUM_WORLD_ITEMS,
    STATISTICS_EXPORT,
}

/** LIFETIME is a one-time purchase (no renewal); the others are subscriptions. */
enum class BillingPeriod { MONTHLY, YEARLY, LIFETIME, OTHER }

/** An ISO-8601 period as used by Google Play ("P1M", "P1Y", "P7D", "P1W"). */
data class IsoPeriod(val years: Int = 0, val months: Int = 0, val weeks: Int = 0, val days: Int = 0) {

    val approxDays: Int get() = years * 365 + months * 30 + weeks * 7 + days

    companion object {
        private val Pattern = Regex("""^P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?$""")

        fun parse(value: String?): IsoPeriod? {
            if (value.isNullOrBlank()) return null
            val match = Pattern.matchEntire(value.trim().uppercase()) ?: return null
            val (y, m, w, d) = match.destructured
            val period = IsoPeriod(y.toIntOrNull() ?: 0, m.toIntOrNull() ?: 0, w.toIntOrNull() ?: 0, d.toIntOrNull() ?: 0)
            return if (period.approxDays == 0) null else period
        }
    }
}

/**
 * One way to buy Premium, as returned by Google Play (prices are already localized by Play).
 * [offerToken] is opaque data the store needs to start the purchase.
 */
data class PremiumOffer(
    val productId: String,
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val period: BillingPeriod,
    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    /** Free trial length when this offer starts with one (Play only returns offers the user is eligible for). */
    val freeTrial: IsoPeriod?,
) {
    val id: String get() = "$productId:$basePlanId:${offerId.orEmpty()}"
}

/** The subscription the user currently owns, as reported by Google Play. */
data class ActiveSubscription(
    val productId: String,
    val period: BillingPeriod,
    /** False when the user cancelled: Premium stays active until the paid period ends, then stops. */
    val isAutoRenewing: Boolean,
)

/** How Google Play applies a switch between plans. */
enum class PlanChangeMode {
    /** Switch now and charge the new plan's full price; unused time of the old plan is credited as extra days. */
    IMMEDIATE,
    /** Keep the current plan until the paid period ends, then start the new plan. */
    AT_NEXT_RENEWAL,
}

/** Loading state of the store connection / product catalogue. */
sealed interface BillingState {
    data object Loading : BillingState
    data object Ready : BillingState
    data class Unavailable(val reason: String) : BillingState
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    /** Payment accepted by Play but not finished yet (e.g. cash / bank transfer). */
    data object Pending : PurchaseResult
    /** A plan change was accepted and takes effect when the current period ends. */
    data object ChangeScheduled : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failure(val message: String) : PurchaseResult
}

object PremiumLimits {
    const val FREE_CUSTOM_BACKGROUNDS = 3
}
