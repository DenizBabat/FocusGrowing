package com.focusgrowing.app.presentation.mission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.EmptyState
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusButtonStyle
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.Pill
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.illustration.ForestFooter
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.SubTask
import com.focusgrowing.app.presentation.common.UiFormat
import com.focusgrowing.app.presentation.common.color
import com.focusgrowing.app.presentation.common.label

@Composable
fun MissionDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStartFocus: (Long) -> Unit,
    viewModel: MissionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val newSubTask by viewModel.newSubTask.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        FocusTopBar(onBack = onBack) {
            val loaded = state as? MissionDetailUiState.Loaded
            if (loaded != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = FocusTheme.colors.onBackground)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MenuItem("Edit", Icons.Rounded.Edit) { menuOpen = false; onEdit(loaded.mission.id) }
                        if (loaded.mission.isCompleted) {
                            MenuItem("Reopen", Icons.Rounded.Replay) { menuOpen = false; viewModel.reopen() }
                        } else {
                            MenuItem("Mark as completed", Icons.Rounded.CheckCircle) { menuOpen = false; viewModel.complete() }
                        }
                        MenuItem("Archive", Icons.Rounded.Archive) { menuOpen = false; viewModel.archive(onBack) }
                        MenuItem("Delete", Icons.Rounded.Delete) { menuOpen = false; confirmDelete = true }
                    }
                }
            }
        }
        when (val s = state) {
            MissionDetailUiState.Loading -> Unit
            MissionDetailUiState.NotFound -> EmptyState(
                icon = Icons.Rounded.Info,
                title = "Mission not found",
                message = "It may have been deleted.",
                action = { FocusButton("Go back", onClick = onBack) },
            )
            is MissionDetailUiState.Loaded -> MissionDetailContent(
                mission = s.mission,
                newSubTask = newSubTask,
                onNewSubTaskChange = viewModel::onNewSubTaskChange,
                onAddSubTask = viewModel::addSubTask,
                onToggleSubTask = viewModel::toggleSubTask,
                onDeleteSubTask = viewModel::deleteSubTask,
                onStartFocus = { onStartFocus(s.mission.id) },
                onReopen = viewModel::reopen,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete mission?") },
            text = { Text("The mission and its checklist will be removed. Your focus history and World XP are kept.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) {
                    Text("Delete", color = FocusTheme.colors.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, contentDescription = null) }, onClick = onClick)
}

@Composable
private fun MissionDetailContent(
    mission: Mission,
    newSubTask: String,
    onNewSubTaskChange: (String) -> Unit,
    onAddSubTask: () -> Unit,
    onToggleSubTask: (Long, Boolean) -> Unit,
    onDeleteSubTask: (Long) -> Unit,
    onStartFocus: () -> Unit,
    onReopen: () -> Unit,
) {
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screen)
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mission.title, style = FocusTheme.typography.headlineSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(spacing.sm))
            Pill(mission.priority.label(), container = mission.priority.color().copy(alpha = 0.18f), content = mission.priority.color())
        }
        mission.description?.let {
            Spacer(Modifier.height(spacing.sm))
            Text(it, style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        Spacer(Modifier.height(spacing.lg))
        InfoRow(Icons.Rounded.Flag, "Priority", mission.priority.label(), mission.priority.color())
        InfoRow(Icons.Rounded.Timer, "Pomodoros", "${mission.completedPomodoros} / ${mission.estimatedPomodoros}", colors.primary)
        Spacer(Modifier.height(spacing.sm))
        FocusProgressBar(mission.progress)

        Spacer(Modifier.height(spacing.xl))
        FocusCard(contentPadding = PaddingValues(vertical = spacing.sm)) {
            Text(
                "Checklist",
                style = FocusTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
            if (mission.subTasks.isEmpty()) {
                Text(
                    "Break the mission into small steps.",
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.lg),
                )
            }
            mission.subTasks.forEach { task -> SubTaskRow(task, onToggleSubTask, onDeleteSubTask) }
            Row(
                Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newSubTask,
                    onValueChange = onNewSubTaskChange,
                    placeholder = { Text("Add a step") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddSubTask() }),
                    colors = focusTextFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAddSubTask, enabled = newSubTask.isNotBlank()) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add step", tint = colors.primary)
                }
            }
        }

        Spacer(Modifier.height(spacing.xl))
        if (mission.status == MissionStatus.COMPLETED) {
            FocusCard(color = colors.primaryContainer, border = false) {
                Text("Mission completed 🎉", style = FocusTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                mission.completedAt?.let {
                    Text("Completed on ${UiFormat.date(it)}", style = FocusTheme.typography.bodySmall, color = colors.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(spacing.md))
            FocusButton("Reopen mission", onClick = onReopen, style = FocusButtonStyle.Soft, leadingIcon = Icons.Rounded.Replay, modifier = Modifier.fillMaxWidth())
        } else {
            if (mission.completedPomodoros >= mission.estimatedPomodoros && mission.subTasks.any { !it.isDone }) {
                Text(
                    "Estimate reached — finish the remaining steps or mark the mission as completed.",
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(spacing.sm))
            }
            FocusButton("Start Focus", onClick = onStartFocus, leadingIcon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(spacing.xl))
        FocusCard {
            InfoRow(Icons.Rounded.CalendarToday, "Created", UiFormat.date(mission.createdAt), colors.onSurfaceVariant)
            InfoRow(Icons.Rounded.Info, "Status", mission.status.readable(), colors.onSurfaceVariant)
        }
        ForestFooter(Modifier.fillMaxWidth().height(90.dp))
    }
}

@Composable
private fun SubTaskRow(task: SubTask, onToggle: (Long, Boolean) -> Unit, onDelete: (Long) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(task.id, !task.isDone) }
            .heightIn(min = 48.dp)
            .padding(horizontal = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle(task.id, it) },
            colors = CheckboxDefaults.colors(checkedColor = FocusTheme.colors.primary),
        )
        Text(
            task.title,
            style = FocusTheme.typography.bodyMedium,
            color = if (task.isDone) FocusTheme.colors.onSurfaceVariant else FocusTheme.colors.onSurface,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onDelete(task.id) }) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove step", tint = FocusTheme.colors.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, tint: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(FocusTheme.spacing.sm))
        Text(label, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurface)
    }
}

private fun MissionStatus.readable(): String = when (this) {
    MissionStatus.TODO -> "To do"
    MissionStatus.IN_PROGRESS -> "In progress"
    MissionStatus.COMPLETED -> "Completed"
    MissionStatus.ARCHIVED -> "Archived"
}
