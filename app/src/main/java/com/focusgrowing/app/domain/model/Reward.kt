package com.focusgrowing.app.domain.model

/**
 * A premium color palette unlocked for a limited time by watching a rewarded ad.
 * Only one trial exists at a time; a new one replaces the old one.
 */
data class PaletteTrial(
    val paletteId: String,
    val grantedAtMillis: Long,
    val expiresAtMillis: Long,
)
