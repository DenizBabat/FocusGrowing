package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.PremiumPricing
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PlanChangeMode
import com.focusgrowing.app.domain.model.PremiumOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremiumPricingTest {

    private fun offer(basePlan: String, offerId: String?, period: BillingPeriod, micros: Long, trial: IsoPeriod? = null) =
        PremiumOffer("premium", basePlan, offerId, "token-$basePlan-$offerId", period, "x", micros, "TRY", trial)

    @Test
    fun `parses play billing periods`() {
        assertEquals(IsoPeriod(months = 1), IsoPeriod.parse("P1M"))
        assertEquals(IsoPeriod(years = 1), IsoPeriod.parse("P1Y"))
        assertEquals(IsoPeriod(days = 7), IsoPeriod.parse("P7D"))
        assertEquals(IsoPeriod(weeks = 1), IsoPeriod.parse("P1W"))
        assertNull(IsoPeriod.parse("P0D"))
        assertNull(IsoPeriod.parse("garbage"))
        assertNull(IsoPeriod.parse(null))
    }

    @Test
    fun `maps billing period to plan type`() {
        assertEquals(BillingPeriod.MONTHLY, PremiumPricing.periodOf("P1M"))
        assertEquals(BillingPeriod.YEARLY, PremiumPricing.periodOf("P1Y"))
        assertEquals(BillingPeriod.YEARLY, PremiumPricing.periodOf("P12M"))
        assertEquals(BillingPeriod.OTHER, PremiumPricing.periodOf("P3M"))
    }

    @Test
    fun `yearly savings compared to twelve months`() {
        assertEquals(33, PremiumPricing.yearlySavingsPercent(49_990_000, 399_990_000))
        assertNull(PremiumPricing.yearlySavingsPercent(10_000_000, 130_000_000)) // yearly more expensive
        assertNull(PremiumPricing.yearlySavingsPercent(0, 100))
        assertEquals(33_332_500, PremiumPricing.monthlyEquivalentMicros(399_990_000))
    }

    @Test
    fun `prefers eligible free trial offer and sorts monthly first`() {
        val chosen = PremiumPricing.chooseOffers(
            listOf(
                offer("yearly", null, BillingPeriod.YEARLY, 399_990_000),
                offer("yearly", "trial", BillingPeriod.YEARLY, 399_990_000, IsoPeriod(days = 7)),
                offer("monthly", null, BillingPeriod.MONTHLY, 49_990_000),
                offer("monthly", "intro", BillingPeriod.MONTHLY, 49_990_000),
            ),
        )
        assertEquals(listOf("monthly", "yearly"), chosen.map { it.basePlanId })
        assertEquals(null, chosen[0].offerId) // plain base plan, not the intro offer
        assertEquals("trial", chosen[1].offerId)
    }

    @Test
    fun `plan change rules`() {
        // Longer period: switch now. Shorter period: wait until the paid year ends. Same plan: nothing.
        assertEquals(PlanChangeMode.IMMEDIATE, PremiumPricing.planChangeMode(BillingPeriod.MONTHLY, BillingPeriod.YEARLY))
        assertEquals(PlanChangeMode.AT_NEXT_RENEWAL, PremiumPricing.planChangeMode(BillingPeriod.YEARLY, BillingPeriod.MONTHLY))
        assertNull(PremiumPricing.planChangeMode(BillingPeriod.YEARLY, BillingPeriod.YEARLY))
        assertNull(PremiumPricing.planChangeMode(BillingPeriod.MONTHLY, BillingPeriod.MONTHLY))
    }

    @Test
    fun `two products each keep their own offer`() {
        val monthly = PremiumOffer("premium_monthly", "monthly", null, "t1", BillingPeriod.MONTHLY, "x", 49_990_000, "TRY", null)
        val yearly = PremiumOffer("premium_yearly", "yearly", null, "t2", BillingPeriod.YEARLY, "x", 399_990_000, "TRY", null)
        val yearlyTrial = yearly.copy(offerId = "trial", offerToken = "t3", freeTrial = IsoPeriod(days = 7))
        val chosen = PremiumPricing.chooseOffers(listOf(yearly, yearlyTrial, monthly))
        assertEquals(listOf("premium_monthly", "premium_yearly"), chosen.map { it.productId })
        assertEquals("t3", chosen[1].offerToken)
    }

    @Test
    fun `lifetime is a separate purchase, never a plan change`() {
        assertNull(PremiumPricing.planChangeMode(BillingPeriod.MONTHLY, BillingPeriod.LIFETIME))
        assertNull(PremiumPricing.planChangeMode(BillingPeriod.YEARLY, BillingPeriod.LIFETIME))
        assertNull(PremiumPricing.planChangeMode(BillingPeriod.LIFETIME, BillingPeriod.MONTHLY))
    }

    @Test
    fun `lifetime break-even in years of the yearly plan`() {
        assertEquals(3, PremiumPricing.lifetimeBreakEvenYears(399_990_000, 999_990_000))
        assertEquals(1, PremiumPricing.lifetimeBreakEvenYears(399_990_000, 399_990_000))
        assertNull(PremiumPricing.lifetimeBreakEvenYears(0, 999_990_000))
    }

    @Test
    fun `lifetime offer is listed after the subscriptions`() {
        val lifetime = PremiumOffer("premium_lifetime", "", null, "t", BillingPeriod.LIFETIME, "x", 999_990_000, "TRY", null)
        val monthly = PremiumOffer("premium_monthly", "monthly", null, "t1", BillingPeriod.MONTHLY, "x", 49_990_000, "TRY", null)
        val yearly = PremiumOffer("premium_yearly", "yearly", null, "t2", BillingPeriod.YEARLY, "x", 399_990_000, "TRY", null)
        val chosen = PremiumPricing.chooseOffers(listOf(lifetime, yearly, monthly))
        assertEquals(listOf(BillingPeriod.MONTHLY, BillingPeriod.YEARLY, BillingPeriod.LIFETIME), chosen.map { it.period })
    }
}
