package com.focusgrowing.app.core.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class RewardedResult {
    /** The user watched the ad to the end: grant the reward. */
    EARNED,
    /** The user closed the ad early: no reward. */
    DISMISSED,
    /** No ad could be loaded or shown (offline, no ad available, consent not collected yet). */
    UNAVAILABLE,
}

/**
 * All advertising in the app goes through this class.
 *
 * Placements
 *  - [sessionEndAd]: one native card on the "focus completed" screen.
 *  - [showRewarded]: a video the user chooses to watch to unlock a premium palette for 24 hours.
 *
 * Rules
 *  - Premium users never see ads; the ad SDK isn't even started for them.
 *  - Nothing is requested before the consent step has finished ([ConsentManager]).
 *  - No ads on the timer screen, no full-screen ads the user didn't ask for.
 *
 * Every field below is only touched on the main thread.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consent: ConsentManager,
    private val subscriptions: SubscriptionRepository,
    private val premium: PremiumManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var startRequested = false
    private var initStarted = false
    private var initialized = false

    private val nativeAd = MutableStateFlow<NativeAd?>(null)
    private var nativeLoading = false
    private var nativeLoadedAt = 0L

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedLoadedAt = 0L
    private val rewardedWaiters = mutableListOf<(RewardedAd?) -> Unit>()

    /** The ad for the "focus completed" screen, or null (Premium, not loaded yet, no ad available). */
    val sessionEndAd: StateFlow<NativeAd?> =
        combine(nativeAd, premium.premiumState) { ad, isPremium -> if (isPremium) null else ad }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** Call once from the main activity. Collects consent if needed, then starts the ad SDK. */
    fun start(activity: Activity) {
        scope.launch(Dispatchers.Main.immediate) {
            // Read the stored value, not the in-memory default: paying users get no consent prompt and no ad SDK.
            if (subscriptions.isPremium.first()) return@launch
            if (startRequested) return@launch
            startRequested = true
            // Consent from an earlier launch is still valid: start right away.
            if (consent.canRequestAds) initialize()
            if (activity.isFinishing || activity.isDestroyed) {
                startRequested = false
                return@launch
            }
            consent.gather(activity) {
                if (consent.canRequestAds) initialize()
            }
        }
    }

    /** Called when the app returns to the foreground: replaces expired ads, retries failed loads. */
    fun onAppForeground() {
        scope.launch(Dispatchers.Main.immediate) {
            preloadNative()
            preloadRewarded()
        }
    }

    private fun initialize() {
        if (initStarted) return
        initStarted = true
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
            withContext(Dispatchers.Main) {
                initialized = true
                preloadNative()
                preloadRewarded()
            }
        }
    }

    private fun isPremium(): Boolean = premium.premiumState.value

    private fun isFresh(loadedAt: Long): Boolean =
        SystemClock.elapsedRealtime() - loadedAt < AdsConfig.AD_MAX_AGE_MILLIS

    // ---------------------------------------------------------------------------------------
    // Native ad (focus completed screen)
    // ---------------------------------------------------------------------------------------

    private fun preloadNative() {
        if (!initialized || nativeLoading || isPremium()) return
        if (nativeAd.value != null && isFresh(nativeLoadedAt)) return
        nativeLoading = true
        val loader = AdLoader.Builder(context, AdsConfig.nativeSessionEndUnitId)
            .forNativeAd { ad ->
                nativeLoading = false
                val old = nativeAd.value
                nativeLoadedAt = SystemClock.elapsedRealtime()
                nativeAd.value = ad
                old?.let { destroyLater(it) }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // No retry loop: the next attempt happens when the app comes to the foreground
                    // or after the next celebration screen.
                    nativeLoading = false
                }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    /** The celebration screen closed after showing the ad: drop it and load the next one. */
    fun onSessionEndAdShown() {
        scope.launch(Dispatchers.Main.immediate) {
            val shown = nativeAd.value
            if (shown != null) {
                nativeAd.value = null
                destroyLater(shown)
            }
            preloadNative()
        }
    }

    /** Destroys an ad after its view has left the screen. */
    private fun destroyLater(ad: NativeAd) {
        scope.launch(Dispatchers.Main) {
            delay(1_500)
            ad.destroy()
        }
    }

    // ---------------------------------------------------------------------------------------
    // Rewarded ad (24-hour palette)
    // ---------------------------------------------------------------------------------------

    private fun preloadRewarded() {
        if (!initialized || isPremium()) return
        if (rewardedAd != null && isFresh(rewardedLoadedAt)) return
        loadRewarded(null)
    }

    private fun loadRewarded(waiter: ((RewardedAd?) -> Unit)?) {
        if (waiter != null) rewardedWaiters += waiter
        if (rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            context,
            AdsConfig.rewardedPaletteUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedLoadedAt = SystemClock.elapsedRealtime()
                    if (rewardedWaiters.isEmpty()) {
                        rewardedAd = ad
                    } else {
                        // Someone is waiting to watch it: hand it over instead of caching it.
                        rewardedAd = null
                        deliverRewarded(ad)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                    deliverRewarded(null)
                }
            },
        )
    }

    private fun deliverRewarded(ad: RewardedAd?) {
        val waiters = rewardedWaiters.toList()
        rewardedWaiters.clear()
        // Only the first waiter can show the ad; the others are told none is available.
        waiters.forEachIndexed { index, waiter -> waiter(if (index == 0) ad else null) }
    }

    /**
     * Shows a rewarded ad. Must be called on the main thread (a click handler).
     * [onResult] runs on the main thread exactly once.
     */
    fun showRewarded(activity: Activity, onResult: (RewardedResult) -> Unit) {
        if (!initialized) {
            // Consent wasn't collected (for example the first launch was offline). Try again for next time.
            startRequested = false
            start(activity)
            onResult(RewardedResult.UNAVAILABLE)
            return
        }
        val cached = rewardedAd?.takeIf { isFresh(rewardedLoadedAt) }
        if (cached != null) {
            rewardedAd = null
            present(activity, cached, onResult)
            return
        }
        rewardedAd = null
        loadRewarded { loaded ->
            if (loaded != null && !activity.isFinishing && !activity.isDestroyed) {
                present(activity, loaded, onResult)
            } else {
                onResult(RewardedResult.UNAVAILABLE)
            }
        }
    }

    private fun present(activity: Activity, ad: RewardedAd, onResult: (RewardedResult) -> Unit) {
        var reported = false
        fun report(result: RewardedResult) {
            if (reported) return
            reported = true
            onResult(result)
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                report(RewardedResult.DISMISSED) // ignored when the reward was already reported
                preloadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                report(RewardedResult.UNAVAILABLE)
                preloadRewarded()
            }
        }
        // The reward is granted the moment Google reports it, so it isn't lost if the app is closed right after.
        ad.show(activity) { _ -> report(RewardedResult.EARNED) }
    }
}
