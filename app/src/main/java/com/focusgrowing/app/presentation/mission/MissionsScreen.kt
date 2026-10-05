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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
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
        ScreenTitle(stringResource(R.string.mission_list_title)) {
            CircleIconButton(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.mission_list_new),
                onClick = onCreateMission,
                container = FocusTheme.colors.xp,
                tint = FocusTheme.colors.onSecondary,
                size = 44.dp,
            )
        }
        FocusChipRow(
            options = filters.map { stringResource(it.labelRes) },
            selectedIndex = filters.indexOf(state.filter),
            onSelect = { viewModel.setFilter(filters[it]) },
        )
        if (!state.loading && state.missions.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Checklist,
                title = stringResource(if (state.totalCount == 0) R.string.mission_list_empty_title else R.string.mission_list_no_match_title),
                message = stringResource(if (state.totalCount == 0) R.string.mission_list_empty_message else R.string.mission_list_no_match_message),
                action = { if (state.totalCount == 0) FocusButton(stringResource(R.string.mission_list_create), onClick = onCreateMission) },
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
