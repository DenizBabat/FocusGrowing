package com.focusgrowing.app.presentation.common

import android.content.res.Resources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.locale.nameRes
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundScene
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.model.WorldItemType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object UiFormat {
    /** 3780 → "1h 3m", 1500 → "25m", 0 → "0m" (units are translated). */
    @Composable
    @ReadOnlyComposable
    fun duration(seconds: Long): String {
        val totalMinutes = seconds / 60
        val h = (totalMinutes / 60).toInt()
        val m = (totalMinutes % 60).toInt()
        return when {
            h > 0 && m > 0 -> stringResource(R.string.format_duration_hours_minutes, h, m)
            h > 0 -> stringResource(R.string.format_duration_hours, h)
            else -> stringResource(R.string.format_duration_minutes, m)
        }
    }

    /** Same as [duration] for places where a Composable can't be called (pass LocalContext.current.resources). */
    fun duration(seconds: Long, resources: Resources): String {
        val totalMinutes = seconds / 60
        val h = (totalMinutes / 60).toInt()
        val m = (totalMinutes % 60).toInt()
        return when {
            h > 0 && m > 0 -> resources.getString(R.string.format_duration_hours_minutes, h, m)
            h > 0 -> resources.getString(R.string.format_duration_hours, h)
            else -> resources.getString(R.string.format_duration_minutes, m)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun greeting(hour: Int): String = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            in 18..22 -> R.string.greeting_evening
            else -> R.string.greeting_hello
        },
    )

    /** Medium date in the app's language, e.g. "5 Oct 2026". */
    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** "10:24", "Yesterday", "3 days ago" */
    @Composable
    @ReadOnlyComposable
    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val then = Instant.ofEpochMilli(millis).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), today)
        return when {
            days <= 0L -> then.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            days == 1L -> stringResource(R.string.format_yesterday)
            days < 7L -> pluralStringResource(R.plurals.format_days_ago, days.toInt(), days.toInt())
            else -> date(millis)
        }
    }

    fun hourSlot(start: Int, end: Int): String = "%02d:00 - %02d:00".format(start, end % 24)

    fun percentChange(value: Int?): String? = value?.let { if (it >= 0) "↑ $it%" else "↓ ${-it}%" }
}

@Composable
@ReadOnlyComposable
fun MissionPriority.label(): String = stringResource(nameRes())

@Composable
@ReadOnlyComposable
fun MissionPriority.color(): Color = when (this) {
    MissionPriority.LOW -> FocusTheme.colors.priorityLow
    MissionPriority.MEDIUM -> FocusTheme.colors.priorityMedium
    MissionPriority.HIGH -> FocusTheme.colors.priorityHigh
}

@Composable
@ReadOnlyComposable
fun MissionCategory.label(): String = stringResource(nameRes())

@Composable
@ReadOnlyComposable
fun WorldItemType.label(): String = stringResource(nameRes())

@Composable
@ReadOnlyComposable
fun BackgroundScene.label(): String = stringResource(nameRes())

/** Built-in backgrounds have translated names; the user's own photos keep the name they were saved with. */
@Composable
@ReadOnlyComposable
fun BackgroundImage.displayLabel(): String {
    val builtIn = scene
    return if (builtIn != null) stringResource(builtIn.nameRes()) else name
}

fun MissionCategory.icon(): ImageVector = when (this) {
    MissionCategory.GENERAL -> Icons.Rounded.Flag
    MissionCategory.DEVELOPMENT -> Icons.Rounded.Code
    MissionCategory.DESIGN -> Icons.Rounded.Brush
    MissionCategory.STUDY -> Icons.Rounded.School
    MissionCategory.READING -> Icons.AutoMirrored.Rounded.MenuBook
    MissionCategory.WORK -> Icons.Rounded.Work
    MissionCategory.HEALTH -> Icons.Rounded.FitnessCenter
    MissionCategory.PERSONAL -> Icons.Rounded.Person
}

/** Rotating accent colors for mission cards (like the design: orange, blue, purple, green). */
@Composable
@ReadOnlyComposable
fun accentFor(index: Long): Pair<Color, Color> {
    val c = FocusTheme.colors
    return when ((index % 4).toInt()) {
        0 -> c.streak to c.streakContainer
        1 -> c.info to c.infoContainer
        2 -> c.accentPurple to c.accentPurpleContainer
        else -> c.success to c.primaryContainer
    }
}
