package com.focusgrowing.app.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.component.PriorityDot
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.Mission

@Composable
fun MissionCard(mission: Mission, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (accent, accentContainer) = if (mission.isCompleted) {
        FocusTheme.colors.success to FocusTheme.colors.primaryContainer
    } else {
        accentFor(mission.id)
    }
    FocusCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(FocusTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = if (mission.isCompleted) Icons.Rounded.Check else mission.category.icon(),
                tint = if (mission.isCompleted) FocusTheme.colors.onPrimary else accent,
                container = if (mission.isCompleted) accent else accentContainer,
            )
            Spacer(Modifier.width(FocusTheme.spacing.md))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    mission.title,
                    style = FocusTheme.typography.titleSmall,
                    color = FocusTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pluralStringResource(R.plurals.mission_pomodoro_count, mission.estimatedPomodoros, mission.estimatedPomodoros),
                    style = FocusTheme.typography.bodySmall,
                    color = FocusTheme.colors.onSurfaceVariant,
                )
            }
            PriorityDot(mission.priority.color())
        }
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusProgressBar(
                mission.progress,
                color = if (mission.isCompleted) FocusTheme.colors.success else accent,
                modifier = Modifier.weight(1f),
                height = 6.dp,
            )
            Spacer(Modifier.width(FocusTheme.spacing.md))
            Text(
                "${mission.completedPomodoros}/${mission.estimatedPomodoros}",
                style = FocusTheme.typography.labelMedium,
                color = FocusTheme.colors.onSurfaceVariant,
            )
        }
    }
}
