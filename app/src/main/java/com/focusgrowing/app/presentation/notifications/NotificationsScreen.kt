package com.focusgrowing.app.presentation.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.EmptyState
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusChipRow
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.illustration.LandscapeBackdrop
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.presentation.common.UiFormat

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenMission: (Long) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = NotificationFilter.entries
    LaunchedEffect(Unit) { viewModel.markAllRead() }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth()) {
            LandscapeBackdrop(Modifier.matchParentSize(), showLake = false)
            Column(Modifier.statusBarsPadding().padding(bottom = FocusTheme.spacing.lg)) {
                FocusTopBar(title = null, onBack = onBack) {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearAll) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all", tint = FocusTheme.colors.onBackground)
                        }
                    }
                }
                Text(
                    "Notifications",
                    style = FocusTheme.typography.headlineMedium,
                    color = FocusTheme.colors.onBackground,
                    modifier = Modifier.padding(horizontal = FocusTheme.spacing.screen),
                )
                Text(
                    "Stay focused, stay on track 🌱",
                    style = FocusTheme.typography.bodyMedium,
                    color = FocusTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FocusTheme.spacing.screen),
                )
            }
        }
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        FocusChipRow(
            options = filters.map { it.label },
            selectedIndex = filters.indexOf(state.filter),
            onSelect = { viewModel.setFilter(filters[it]) },
        )
        if (!state.loading && state.items.isEmpty()) {
            EmptyState(Icons.Rounded.Notifications, "No notifications yet", "Complete focus sessions and missions to see your progress here.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = FocusTheme.spacing.screen, vertical = FocusTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm),
            ) {
                items(state.items, key = { it.id }) { n ->
                    NotificationRow(n, onClick = n.missionId?.let { id -> { onOpenMission(id) } })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: (() -> Unit)?) {
    val colors = FocusTheme.colors
    val (icon, tint, container) = when (n.type) {
        NotificationType.MISSION -> Triple(Icons.Rounded.CheckCircle, colors.success, colors.primaryContainer)
        NotificationType.FOCUS -> Triple(Icons.Rounded.Star, colors.xp, colors.xpContainer)
        NotificationType.WORLD -> Triple(Icons.Rounded.Eco, colors.success, colors.primaryContainer)
        NotificationType.STREAK -> Triple(Icons.Rounded.LocalFireDepartment, colors.streak, colors.streakContainer)
        NotificationType.SYSTEM -> Triple(Icons.Rounded.Info, colors.info, colors.infoContainer)
    }
    FocusCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(FocusTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, container, size = 44.dp)
            Spacer(Modifier.width(FocusTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(n.title, style = FocusTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
                    Text(UiFormat.relative(n.createdAt), style = FocusTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text(n.message, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                n.detail?.let { Text(it, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
            }
        }
    }
}
