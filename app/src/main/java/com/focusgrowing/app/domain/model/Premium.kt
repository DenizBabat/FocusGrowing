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

enum class BillingPeriod { MONTHLY, YEARLY, OTHER }

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
    data object Cancelled : PurchaseResult
    data class Failure(val message: String) : PurchaseResult
}

object PremiumLimits {
    const val FREE_CUSTOM_BACKGROUNDS = 3
}
