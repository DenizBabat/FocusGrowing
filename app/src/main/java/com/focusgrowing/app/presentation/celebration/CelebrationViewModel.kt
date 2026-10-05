package com.focusgrowing.app.presentation.celebration

import androidx.lifecycle.ViewModel
import com.focusgrowing.app.core.ads.AdsManager
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CelebrationViewModel @Inject constructor(
    private val queue: CelebrationQueue,
    private val ads: AdsManager,
) : ViewModel() {
    val current: StateFlow<Celebration?> = queue.current

    /** Native ad for the "focus completed" card; null for Premium users or when no ad is available. */
    val sessionEndAd: StateFlow<NativeAd?> = ads.sessionEndAd

    private var adShown = false

    fun dismiss() = queue.dismissCurrent()

    fun onAdShown() {
        adShown = true
    }

    override fun onCleared() {
        // Each ad is shown on one celebration only; the next one is loaded in the background.
        if (adShown) ads.onSessionEndAdShown()
    }
}
