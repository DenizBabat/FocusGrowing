package com.focusgrowing.app

import android.app.Application
import com.focusgrowing.app.core.billing.PurchaseManager
import com.focusgrowing.app.core.notification.FocusNotifier
import com.focusgrowing.app.core.timer.AppForegroundTracker
import com.focusgrowing.app.core.timer.FocusTimerManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FocusGrowingApplication : Application() {

    @Inject lateinit var notifier: FocusNotifier
    @Inject lateinit var timerManager: FocusTimerManager
    @Inject lateinit var foregroundTracker: AppForegroundTracker
    @Inject lateinit var purchaseManager: PurchaseManager

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        // Every time the app comes to the foreground, finish overdue sessions and re-arm alarms.
        // Also re-check Google Play purchases (renewals, cancellations, refunds, other devices).
        foregroundTracker.start {
            timerManager.reconcile()
            purchaseManager.refreshAsync()
        }
    }
}
