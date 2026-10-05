package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.PaletteTrial

/** Who may use which color palette: free palettes, Premium, or a 24-hour rewarded-ad trial. */
object PaletteAccess {
    /** How long one rewarded ad unlocks a premium palette. */
    const val TRIAL_DURATION_MILLIS: Long = 24L * 60 * 60 * 1000

    /** Small tolerance for clock corrections; a clock set back further than this ends the trial. */
    private const val CLOCK_TOLERANCE_MILLIS: Long = 5L * 60 * 1000
    private const val HOUR_MILLIS: Long = 60L * 60 * 1000

    fun newTrial(paletteId: String, now: Long): PaletteTrial =
        PaletteTrial(paletteId = paletteId, grantedAtMillis = now, expiresAtMillis = now + TRIAL_DURATION_MILLIS)

    fun isTrialActive(trial: PaletteTrial?, now: Long): Boolean =
        trial != null && now < trial.expiresAtMillis && now >= trial.grantedAtMillis - CLOCK_TOLERANCE_MILLIS

    fun isTrialActiveFor(trial: PaletteTrial?, paletteId: String, now: Long): Boolean =
        trial?.paletteId == paletteId && isTrialActive(trial, now)

    fun canUse(paletteId: String, paletteIsPremium: Boolean, isPremium: Boolean, trial: PaletteTrial?, now: Long): Boolean =
        !paletteIsPremium || isPremium || isTrialActiveFor(trial, paletteId, now)

    /** Whole hours left, rounded up (23 h 10 min → 24). 0 when the trial is over. */
    fun hoursLeft(trial: PaletteTrial?, now: Long): Int {
        if (trial == null || !isTrialActive(trial, now)) return 0
        val remaining = trial.expiresAtMillis - now
        return ((remaining + HOUR_MILLIS - 1) / HOUR_MILLIS).toInt()
    }
}
