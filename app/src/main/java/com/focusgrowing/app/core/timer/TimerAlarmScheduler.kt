package com.focusgrowing.app.core.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wakes the app when a session ends, even if the process was killed.
 * No foreground service is needed because the timer itself is timestamp based.
 * Uses an exact alarm when the user allows it, otherwise an inexact one
 * (the app still reconciles the moment it is opened).
 */
@Singleton
class TimerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    fun schedule(triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        val intent = pendingIntent()
        try {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
    }

    fun cancel() {
        alarmManager?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).setAction(TimerAlarmReceiver.ACTION_TIMER_END)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 42
    }
}
