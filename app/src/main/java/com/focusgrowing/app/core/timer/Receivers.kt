package com.focusgrowing.app.core.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Session end alarm + Pause/Resume actions from the ongoing notification. */
@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var timerManager: FocusTimerManager

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                when (intent.action) {
                    ACTION_TIMER_END -> timerManager.onTimeElapsed()
                    ACTION_PAUSE -> timerManager.pause()
                    ACTION_RESUME -> timerManager.resume()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TIMER_END = "com.focusgrowing.app.action.TIMER_END"
        const val ACTION_PAUSE = "com.focusgrowing.app.action.PAUSE"
        const val ACTION_RESUME = "com.focusgrowing.app.action.RESUME"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/** Alarms are cleared on reboot; re-arm a running session. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var timerManager: FocusTimerManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) timerManager.reconcile()
    }
}
