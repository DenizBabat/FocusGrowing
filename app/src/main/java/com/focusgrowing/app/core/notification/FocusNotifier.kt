package com.focusgrowing.app.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusgrowing.app.MainActivity
import com.focusgrowing.app.R
import com.focusgrowing.app.core.timer.TimerAlarmReceiver
import com.focusgrowing.app.domain.logic.TimerCalculator
import com.focusgrowing.app.domain.model.SessionReward
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All system notifications live here. Never crashes when the permission is missing:
 * notifications are simply skipped (the in-app inbox still records the events).
 * Mission titles are shown only on the device and never logged.
 */
@Singleton
class FocusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        val timer = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_timer_desc)
            setShowBadge(false)
        }
        val events = NotificationChannel(
            CHANNEL_EVENTS,
            context.getString(R.string.channel_events_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_events_desc)
        }
        system.createNotificationChannels(listOf(timer, events))
    }

    fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun showRunning(state: TimerState, missionTitle: String?) {
        val title = when (state.sessionType) {
            SessionType.FOCUS -> "Focusing"
            SessionType.SHORT_BREAK -> "Short break"
            SessionType.LONG_BREAK -> "Long break"
        }
        val builder = baseBuilder(CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(missionTitle ?: "Stay focused, you can do it!")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setWhen(state.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(0, "Pause", actionIntent(TimerAlarmReceiver.ACTION_PAUSE, 1))
        post(ID_TIMER, builder)
    }

    fun showPaused(state: TimerState, missionTitle: String?) {
        val left = TimerCalculator.formatMmSs(state.remainingWhenPausedMillis)
        val builder = baseBuilder(CHANNEL_TIMER)
            .setContentTitle("Paused · $left left")
            .setContentText(missionTitle ?: "Tap to continue your session")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, "Resume", actionIntent(TimerAlarmReceiver.ACTION_RESUME, 2))
        post(ID_TIMER, builder)
    }

    fun cancelRunning() {
        manager.cancel(ID_TIMER)
    }

    fun showSessionFinished(reward: SessionReward, withSound: Boolean) {
        val builder = if (reward.sessionType == SessionType.FOCUS) {
            val text = buildString {
                append("You completed a ${reward.focusMinutes} minute focus session.")
                if (reward.xpEarned > 0) append(" +${reward.xpEarned} World XP")
            }
            baseBuilder(CHANNEL_EVENTS)
                .setContentTitle(if (reward.missionCompleted) "🎉 Mission completed!" else "🎉 Focus session completed")
                .setContentText(reward.missionTitle?.let { "$it · $text" } ?: text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reward.missionTitle?.let { "Mission: $it\n$text" } ?: text))
        } else {
            baseBuilder(CHANNEL_EVENTS)
                .setContentTitle("☕ Break finished")
                .setContentText("Ready for another focus session?")
        }
        builder.setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(!withSound)
        post(ID_EVENT, builder)
    }

    private fun baseBuilder(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_leaf)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentIntent(openAppIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_FOCUS, true)
        }
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @SuppressLint("MissingPermission") // checked in canPost()
    private fun post(id: Int, builder: NotificationCompat.Builder) {
        if (!canPost()) return
        try {
            manager.notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call — ignore.
        }
    }

    companion object {
        const val CHANNEL_TIMER = "focus_timer"
        const val CHANNEL_EVENTS = "session_events"
        private const val ID_TIMER = 1001
        private const val ID_EVENT = 1002
    }
}
