package com.focusgrowing.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.component.FocusTextButton
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.component.SectionHeader
import com.focusgrowing.app.core.designsystem.component.StatTile
import com.focusgrowing.app.core.designsystem.illustration.LandscapeBackdrop
import com.focusgrowing.app.core.designsystem.illustration.WorldIsland
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.presentation.common.UiFormat
import com.focusgrowing.app.presentation.common.accentFor
import com.focusgrowing.app.presentation.common.icon

@Composable
fun HomeScreen(
    onStartFocus: (missionId: Long?) -> Unit,
    onOpenMission: (Long) -> Unit,
    onViewAllMissions: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreateMission: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // World header: sky, island and greeting
        Box(Modifier.fillMaxWidth()) {
            LandscapeBackdrop(Modifier.matchParentSize(), showLake = true)
            Column(Modifier.statusBarsPadding().padding(horizontal = spacing.screen, vertical = spacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        val greeting = UiFormat.greeting(state.hour)
                        Text(
                            if (state.userName.isBlank()) "$greeting!" else "$greeting, ${state.userName}!",
                            style = FocusTheme.typography.titleLarge,
                            color = colors.onBackground,
                        )
                        Text("Small steps create big changes.", style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(badge = { if (state.unreadNotifications > 0) Badge { Text(state.unreadNotifications.coerceAtMost(9).toString()) } }) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = colors.onBackground)
                        }
                    }
                    WorldLevelChip(state.world.level)
                }
                WorldIsland(
                    items = state.worldItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.45f)
                        .padding(horizontal = spacing.xl),
                )
            }
        }

        Column(Modifier.padding(horizontal = spacing.screen).offset(y = (-12).dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                StatTile(
                    Icons.Rounded.Timer, "Focus Time", UiFormat.duration(state.totalFocusSeconds),
                    colors.info, colors.infoContainer, Modifier.weight(1f),
                )
                StatTile(
                    Icons.Rounded.CheckCircle, "Completed", state.completedMissions.toString(),
                    colors.success, colors.primaryContainer, Modifier.weight(1f),
                )
                StatTile(
                    Icons.Rounded.LocalFireDepartment, "Streak", "${state.streak.currentDays} days",
                    colors.streak, colors.streakContainer, Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(contentPadding = PaddingValues(spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("World Level ${state.world.level}", style = FocusTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
                    Text(
                        "${state.world.xpIntoLevel} / ${state.world.xpForNextLevel} XP",
                        style = FocusTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(spacing.sm))
                FocusProgressBar(state.world.progress, color = colors.xp)
            }

            Spacer(Modifier.height(spacing.xl))
            SectionHeader("Today's Mission", action = "View all", onAction = onViewAllMissions)
            Spacer(Modifier.height(spacing.sm))
            val mission = state.currentMission
            if (mission != null) {
                val (accent, accentContainer) = accentFor(mission.id)
                FocusCard(onClick = { onOpenMission(mission.id) }, contentPadding = PaddingValues(spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(mission.category.icon(), accent, accentContainer)
                        Spacer(Modifier.width(spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(mission.title, style = FocusTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${mission.completedPomodoros}/${mission.estimatedPomodoros} Pomodoros",
                                style = FocusTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text("${(mission.progress * 100).toInt()}%", style = FocusTheme.typography.labelLarge, color = accent)
                    }
                    Spacer(Modifier.height(spacing.sm))
                    FocusProgressBar(mission.progress, color = accent, height = 6.dp)
                }
            } else {
                FocusCard(onClick = onCreateMission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Add, colors.primary, colors.primaryContainer)
                        Spacer(Modifier.width(spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text("Create your first mission", style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                            Text("Give your focus a goal.", style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.lg))
            val timerLabel = when (state.timer.phase) {
                TimerPhase.RUNNING -> "Return to Focus"
                TimerPhase.PAUSED -> "Resume Focus"
                else -> "Start Focus"
            }
            FocusButton(
                timerLabel,
                onClick = { onStartFocus(if (state.timer.isActive) state.timer.missionId else mission?.id) },
                leadingIcon = Icons.Rounded.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            if (mission == null) {
                FocusTextButton("Focus without a mission", onClick = { onStartFocus(null) }, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun WorldLevelChip(level: Int) {
    FocusCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Text("World Level", style = FocusTheme.typography.labelSmall, color = FocusTheme.colors.onSurfaceVariant)
        Text(level.toString(), style = FocusTheme.typography.titleLarge, color = FocusTheme.colors.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
