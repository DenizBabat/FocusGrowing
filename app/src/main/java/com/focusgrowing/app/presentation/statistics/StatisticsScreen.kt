package com.focusgrowing.app.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusChipRow
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.component.PremiumLockedCard
import com.focusgrowing.app.core.designsystem.component.ScreenTitle
import com.focusgrowing.app.core.designsystem.component.SectionHeader
import com.focusgrowing.app.core.designsystem.component.StatTile
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.Insight
import com.focusgrowing.app.domain.model.InsightKind
import com.focusgrowing.app.domain.model.StatsRange
import com.focusgrowing.app.presentation.common.UiFormat

@Composable
fun StatisticsScreen(onOpenPremium: () -> Unit, viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val stats = state.stats
    val ranges = StatsRange.entries

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenTitle("Statistics", subtitle = "Understand your focus patterns")
        FocusChipRow(
            options = ranges.map { it.label },
            selectedIndex = ranges.indexOf(state.range),
            lockedIndices = if (state.isPremium) emptySet() else ranges.withIndex().filter { it.value.isPremium }.map { it.index }.toSet(),
            onSelect = { index -> if (!viewModel.selectRange(ranges[index])) onOpenPremium() },
        )
        Column(Modifier.padding(horizontal = spacing.screen, vertical = spacing.lg)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                StatTile(
                    Icons.Rounded.Timer, "Focus Time", UiFormat.duration(stats.totalFocusSeconds),
                    colors.info, colors.infoContainer, Modifier.weight(1f),
                    footer = { ChangeLabel(stats.focusChangePercent) },
                )
                StatTile(
                    Icons.Rounded.CheckCircle, "Sessions", stats.sessionCount.toString(),
                    colors.accentPurple, colors.accentPurpleContainer, Modifier.weight(1f),
                    footer = { ChangeLabel(stats.sessionChangePercent) },
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                StatTile(Icons.Rounded.Today, "Today", UiFormat.duration(stats.todayFocusSeconds), colors.success, colors.primaryContainer, Modifier.weight(1f))
                StatTile(Icons.Rounded.CalendarMonth, "This Week", UiFormat.duration(stats.weekFocusSeconds), colors.info, colors.infoContainer, Modifier.weight(1f))
            }
            Spacer(Modifier.height(spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                StatTile(Icons.Rounded.EventAvailable, "Missions", stats.completedMissions.toString(), colors.xp, colors.xpContainer, Modifier.weight(1f))
                StatTile(Icons.Rounded.LocalFireDepartment, "Streak", "${state.streak.currentDays} days", colors.streak, colors.streakContainer, Modifier.weight(1f))
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Text("Focus Time Trend", style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                Spacer(Modifier.height(spacing.md))
                FocusBarChart(stats.buckets)
            }

            Spacer(Modifier.height(spacing.lg))
            if (state.isPremium) {
                FocusCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Top Focus Hours", style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                    Spacer(Modifier.height(spacing.sm))
                    if (stats.topHourSlots.isEmpty()) {
                        Text("Complete a few sessions to see your best hours.", style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    } else {
                        TopHoursList(stats.topHourSlots)
                    }
                }
                Spacer(Modifier.height(spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    StatTile(Icons.Rounded.Schedule, "Avg. Session", UiFormat.duration(stats.averageSessionSeconds), colors.info, colors.infoContainer, Modifier.weight(1f))
                    StatTile(Icons.AutoMirrored.Rounded.TrendingUp, "Completion", "${(stats.completionRate * 100).toInt()}%", colors.success, colors.primaryContainer, Modifier.weight(1f))
                }
                Spacer(Modifier.height(spacing.sm))
                val bestSlot = stats.topHourSlots.firstOrNull()
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    StatTile(
                        Icons.Rounded.WbSunny, "Best Time",
                        bestSlot?.let { UiFormat.hourSlot(it.startHour, it.endHour) } ?: "—",
                        colors.xp, colors.xpContainer, Modifier.weight(1f),
                    )
                    StatTile(
                        Icons.Rounded.Insights, "Estimation",
                        stats.estimationErrorPercent?.let { if (it >= 0) "+$it%" else "$it%" } ?: "—",
                        colors.accentPurple, colors.accentPurpleContainer, Modifier.weight(1f),
                    )
                }
            } else {
                PremiumLockedCard(
                    title = "Advanced Statistics",
                    description = "Best focus hours, average session, completion rate, estimation accuracy and monthly/yearly trends.",
                    onUnlock = onOpenPremium,
                )
            }

            Spacer(Modifier.height(spacing.xl))
            SectionHeader("Insights")
            Spacer(Modifier.height(spacing.sm))
            state.insights.forEach { insight ->
                InsightCard(insight, locked = insight.isPremium && !state.isPremium, onUnlock = onOpenPremium)
                Spacer(Modifier.height(spacing.sm))
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }
}

@Composable
private fun ChangeLabel(percent: Int?) {
    val text = UiFormat.percentChange(percent) ?: return
    val color = if ((percent ?: 0) >= 0) FocusTheme.colors.success else FocusTheme.colors.danger
    Text(text, style = FocusTheme.typography.labelMedium, color = color)
}

@Composable
private fun InsightCard(insight: Insight, locked: Boolean, onUnlock: () -> Unit) {
    val colors = FocusTheme.colors
    FocusCard(modifier = Modifier.fillMaxWidth(), onClick = if (locked) onUnlock else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(insight.kind.icon(), colors.info, colors.infoContainer)
            Spacer(Modifier.width(FocusTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    if (locked) "Premium insight" else insight.title,
                    style = FocusTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Text(
                    if (locked) "Unlock Premium to see this personal insight about your focus." else insight.message,
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (locked) Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = colors.premium, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun InsightKind.icon(): ImageVector = when (this) {
    InsightKind.TIME_OF_DAY -> Icons.Rounded.WbSunny
    InsightKind.SESSION_LENGTH -> Icons.Rounded.Timer
    InsightKind.ESTIMATION -> Icons.Rounded.Insights
    InsightKind.CONSISTENCY -> Icons.Rounded.LocalFireDepartment
    InsightKind.INTERRUPTIONS -> Icons.Rounded.Psychology
    InsightKind.BEST_DAY -> Icons.Rounded.CalendarMonth
    InsightKind.GETTING_STARTED -> Icons.Rounded.AutoAwesome
}
