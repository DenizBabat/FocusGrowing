package com.focusgrowing.app.presentation.mission

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.CircleIconButton
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusChip
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.usecase.SaveMissionUseCase
import com.focusgrowing.app.presentation.common.label

@Composable
fun MissionEditorScreen(onDone: () -> Unit, viewModel: MissionEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        FocusTopBar(title = stringResource(if (state.isNew) R.string.mission_editor_title_new else R.string.mission_editor_title_edit), onBack = onDone)
        if (!state.loading) EditorBody(state, viewModel, onDone)
    }
}

@Composable
private fun ColumnScope.EditorBody(state: MissionEditorUiState, viewModel: MissionEditorViewModel, onDone: () -> Unit) {
    val spacing = FocusTheme.spacing
    val colors = FocusTheme.colors
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen),
    ) {
        OutlinedTextField(
            value = state.draft.title,
            onValueChange = viewModel::setTitle,
            label = { Text(stringResource(R.string.mission_editor_title_label)) },
            singleLine = true,
            isError = state.error != null && state.draft.title.isBlank(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.md))
        OutlinedTextField(
            value = state.draft.description,
            onValueChange = viewModel::setDescription,
            label = { Text(stringResource(R.string.mission_editor_description_label)) },
            minLines = 2,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.xl))
        Label(stringResource(R.string.mission_editor_estimate_label))
        FocusCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Rounded.Remove, stringResource(R.string.mission_editor_decrease), onClick = { viewModel.changeEstimate(-1) }, size = 40.dp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.draft.estimatedPomodoros}", style = FocusTheme.typography.headlineMedium, color = colors.onSurface)
                    Text(pluralStringResource(R.plurals.mission_editor_pomodoros_unit, state.draft.estimatedPomodoros), style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                CircleIconButton(Icons.Rounded.Add, stringResource(R.string.mission_editor_increase), onClick = { viewModel.changeEstimate(1) }, size = 40.dp)
            }
        }

        Spacer(Modifier.height(spacing.xl))
        Label(stringResource(R.string.mission_priority))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MissionPriority.entries.forEach { p ->
                FocusChip(p.label(), selected = state.draft.priority == p, onClick = { viewModel.setPriority(p) }, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(spacing.xl))
        Label(stringResource(R.string.mission_editor_category_label))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MissionCategory.entries.forEach { c ->
                FocusChip(c.label(), selected = state.draft.category == c, onClick = { viewModel.setCategory(c) })
            }
        }

        if (state.isNew) {
            Spacer(Modifier.height(spacing.xl))
            Label(stringResource(R.string.mission_editor_checklist_label))
            state.draft.subTasks.forEachIndexed { index, title ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("•  $title", style = FocusTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeSubTask(index) }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.mission_step_remove), tint = colors.outline)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.newSubTask,
                    onValueChange = viewModel::setNewSubTask,
                    placeholder = { Text(stringResource(R.string.mission_step_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addSubTask() }),
                    colors = focusTextFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(spacing.sm))
                CircleIconButton(Icons.Rounded.Add, stringResource(R.string.mission_step_add), onClick = viewModel::addSubTask, size = 44.dp)
            }
        }
        Spacer(Modifier.height(spacing.xl))
    }
    Column(Modifier.padding(horizontal = spacing.screen, vertical = spacing.md).navigationBarsPadding()) {
        state.error?.let {
            Text(it.message(), style = FocusTheme.typography.bodySmall, color = colors.error)
            Spacer(Modifier.height(spacing.sm))
        }
        FocusButton(
            stringResource(if (state.isNew) R.string.mission_editor_create else R.string.mission_editor_save_changes),
            onClick = { viewModel.save(onDone) },
            loading = state.saving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MissionEditorError.message(): String = when (this) {
    MissionEditorError.NOT_FOUND -> stringResource(R.string.mission_not_found)
    MissionEditorError.EMPTY_TITLE -> stringResource(R.string.mission_error_empty_title)
    MissionEditorError.TITLE_TOO_LONG -> stringResource(R.string.mission_error_title_too_long)
    MissionEditorError.INVALID_ESTIMATE -> stringResource(R.string.mission_error_invalid_estimate, SaveMissionUseCase.MAX_ESTIMATE)
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = FocusTheme.typography.titleSmall,
        color = FocusTheme.colors.onBackground,
        modifier = Modifier.padding(bottom = FocusTheme.spacing.sm),
    )
}
