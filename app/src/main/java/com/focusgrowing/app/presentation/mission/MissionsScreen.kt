package com.focusgrowing.app.presentation.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.CircleIconButton
import com.focusgrowing.app.core.designsystem.component.EmptyState
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusChipRow
import com.focusgrowing.app.core.designsystem.component.ScreenTitle
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.presentation.common.MissionCard

@Composable
fun MissionsScreen(
    onOpenMission: (Long) -> Unit,
    onCreateMission: () -> Unit,
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = MissionFilter.entries

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Missions") {
            CircleIconButton(
                Icons.Rounded.Add,
                contentDescription = "New mission",
                onClick = onCreateMission,
                container = FocusTheme.colors.xp,
                tint = FocusTheme.colors.onSecondary,
                size = 44.dp,
            )
        }
        FocusChipRow(
            options = filters.map { it.label },
            selectedIndex = filters.indexOf(state.filter),
            onSelect = { viewModel.setFilter(filters[it]) },
        )
        if (!state.loading && state.missions.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Checklist,
                title = if (state.totalCount == 0) "No missions yet" else "Nothing here",
                message = if (state.totalCount == 0) "Create a mission and start your first focus session." else "No missions match this filter.",
                action = { if (state.totalCount == 0) FocusButton("Create mission", onClick = onCreateMission) },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = FocusTheme.spacing.screen, vertical = FocusTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(FocusTheme.spacing.md),
            ) {
                items(state.missions, key = { it.id }) { mission ->
                    MissionCard(mission, onClick = { onOpenMission(mission.id) })
                }
            }
        }
    }
}
