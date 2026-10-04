package com.focusgrowing.app.presentation.common

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
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object UiFormat {
    /** 3780 → "1h 3m", 1500 → "25m", 0 → "0m" */
    fun duration(seconds: Long): String {
        val totalMinutes = seconds / 60
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Hello"
    }

    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** "10:24", "Yesterday", "3 days ago" */
    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val then = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), today)
        return when {
            days <= 0L -> then.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            days == 1L -> "Yesterday"
            days < 7L -> "$days days ago"
            else -> date(millis)
        }
    }

    fun hourSlot(start: Int, end: Int): String = "%02d:00 - %02d:00".format(start, end % 24)

    fun percentChange(value: Int?): String? = value?.let { if (it >= 0) "↑ $it%" else "↓ ${-it}%" }
}

fun MissionPriority.label(): String = when (this) {
    MissionPriority.LOW -> "Low"
    MissionPriority.MEDIUM -> "Medium"
    MissionPriority.HIGH -> "High"
}

@Composable
@ReadOnlyComposable
fun MissionPriority.color(): Color = when (this) {
    MissionPriority.LOW -> FocusTheme.colors.priorityLow
    MissionPriority.MEDIUM -> FocusTheme.colors.priorityMedium
    MissionPriority.HIGH -> FocusTheme.colors.priorityHigh
}

fun MissionCategory.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

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
