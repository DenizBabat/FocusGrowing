package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.PaletteAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteAccessTest {
    private val hour = 60L * 60 * 1000
    private val start = 1_000_000_000L
    private val trial = PaletteAccess.newTrial("ocean", start)

    @Test
    fun freePaletteIsAlwaysUsable() {
        assertTrue(PaletteAccess.canUse("mint_meadow", paletteIsPremium = false, isPremium = false, trial = null, now = start))
    }

    @Test
    fun premiumPaletteNeedsPremiumOrTrial() {
        assertFalse(PaletteAccess.canUse("ocean", paletteIsPremium = true, isPremium = false, trial = null, now = start))
        assertTrue(PaletteAccess.canUse("ocean", paletteIsPremium = true, isPremium = true, trial = null, now = start))
        assertTrue(PaletteAccess.canUse("ocean", paletteIsPremium = true, isPremium = false, trial = trial, now = start + hour))
    }

    @Test
    fun trialUnlocksOnlyItsOwnPalette() {
        assertFalse(PaletteAccess.canUse("sunset", paletteIsPremium = true, isPremium = false, trial = trial, now = start + hour))
    }

    @Test
    fun trialLastsExactly24Hours() {
        assertTrue(PaletteAccess.isTrialActive(trial, start + 24 * hour - 1))
        assertFalse(PaletteAccess.isTrialActive(trial, start + 24 * hour))
        assertFalse(PaletteAccess.canUse("ocean", paletteIsPremium = true, isPremium = false, trial = trial, now = start + 25 * hour))
    }

    @Test
    fun settingTheClockBackEndsTheTrial() {
        assertTrue(PaletteAccess.isTrialActive(trial, start - 60_000)) // small correction is tolerated
        assertFalse(PaletteAccess.isTrialActive(trial, start - 2 * hour))
    }

    @Test
    fun hoursLeftRoundsUp() {
        assertEquals(24, PaletteAccess.hoursLeft(trial, start))
        assertEquals(24, PaletteAccess.hoursLeft(trial, start + 10 * 60_000))
        assertEquals(1, PaletteAccess.hoursLeft(trial, start + 23 * hour + 30 * 60_000))
        assertEquals(0, PaletteAccess.hoursLeft(trial, start + 24 * hour))
        assertEquals(0, PaletteAccess.hoursLeft(null, start))
    }
}
