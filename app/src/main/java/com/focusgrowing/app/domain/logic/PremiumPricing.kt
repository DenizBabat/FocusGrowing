package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PremiumOffer
import kotlin.math.roundToInt

/** Pure helpers for showing subscription prices. No Android / Play classes here, so they are unit-testable. */
object PremiumPricing {

    fun periodOf(isoBillingPeriod: String?): BillingPeriod {
        val p = IsoPeriod.parse(isoBillingPeriod) ?: return BillingPeriod.OTHER
        return when {
            p.years == 1 && p.months == 0 && p.weeks == 0 && p.days == 0 -> BillingPeriod.YEARLY
            p.years == 0 && p.months == 12 && p.weeks == 0 && p.days == 0 -> BillingPeriod.YEARLY
            p.years == 0 && p.months == 1 && p.weeks == 0 && p.days == 0 -> BillingPeriod.MONTHLY
            else -> BillingPeriod.OTHER
        }
    }

    /** How much cheaper the yearly plan is than paying monthly for 12 months, in whole percent. */
    fun yearlySavingsPercent(monthlyMicros: Long, yearlyMicros: Long): Int? {
        if (monthlyMicros <= 0 || yearlyMicros <= 0) return null
        val fullYear = monthlyMicros * 12.0
        val percent = ((1.0 - yearlyMicros / fullYear) * 100).roundToInt()
        return percent.takeIf { it in 1..95 }
    }

    fun monthlyEquivalentMicros(yearlyMicros: Long): Long = yearlyMicros / 12

    /**
     * Picks what to show for each base plan: an offer with a free trial when Play says the user
     * is eligible for one, otherwise the plain base plan. Result is sorted monthly → yearly.
     */
    fun chooseOffers(candidates: List<PremiumOffer>): List<PremiumOffer> =
        candidates
            .groupBy { it.basePlanId }
            .mapNotNull { (_, offers) ->
                offers.firstOrNull { it.freeTrial != null } ?: offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
            }
            .sortedBy {
                when (it.period) {
                    BillingPeriod.MONTHLY -> 0
                    BillingPeriod.YEARLY -> 1
                    BillingPeriod.OTHER -> 2
                }
            }
}
