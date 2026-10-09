package com.focusgrowing.app.core.ads

import com.focusgrowing.app.BuildConfig

/**
 * Ad unit IDs. Paste your own from AdMob (Apps → Ad units) before publishing; see docs/ads/ADS_SETUP.md.
 *
 * While a field is empty, and in every debuggable build (debug, playTest), Google's public TEST units
 * are used. Never tap your own real ads while developing: AdMob may suspend the account.
 *
 * The AdMob APP ID (with a "~") is separate: res/values/ads.xml.
 */
object AdsConfig {
    /** Native ad on the "focus completed" screen. Format in AdMob: Native advanced. */
    private const val NATIVE_SESSION_END = "ca-app-pub-8216171271125899/4512411837"

    /** Rewarded ad that unlocks a premium color palette for 24 hours. Format in AdMob: Rewarded. */
    private const val REWARDED_PALETTE = "ca-app-pub-8216171271125899/9984978792"

    private const val TEST_NATIVE = "ca-app-pub-8216171271125899/4512411837"
    private const val TEST_REWARDED = "ca-app-pub-8216171271125899/9984978792"

    val nativeSessionEndUnitId: String
        get() = if (BuildConfig.DEBUG || NATIVE_SESSION_END.isBlank()) TEST_NATIVE else NATIVE_SESSION_END

    val rewardedPaletteUnitId: String
        get() = if (BuildConfig.DEBUG || REWARDED_PALETTE.isBlank()) TEST_REWARDED else REWARDED_PALETTE

    /** A loaded ad is thrown away after this long (Google expires them after one hour). */
    const val AD_MAX_AGE_MILLIS: Long = 50L * 60 * 1000
}
