package com.focusgrowing.app.presentation.focus

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusButtonStyle
import com.focusgrowing.app.core.designsystem.component.SettingsToggleRow
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.image.BackgroundImageView
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.presentation.app.LocalHapticsEnabled
import com.focusgrowing.app.presentation.common.displayLabel
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Lock

@Composable
fun FocusScreen(
    onBack: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    fun tap() {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var showOptions by remember { mutableStateOf(false) }
    var showMissionPicker by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    // Ask for the notification permission (Android 13+) the first time a session starts.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.start() // start regardless of the answer; notifications are optional
    }
    val startWithPermission = {
        tap()
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else viewModel.start()
    }

    // Keep the screen awake while a session is running.
    val view = LocalView.current
    DisposableEffect(state.isRunning) {
        view.keepScreenOn = state.isRunning
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize().background(colors.scrim)) {
        BackgroundImageView(
            background = state.background,
            modifier = Modifier.fillMaxSize(),
            blurRadius = if (state.isPremium) state.screen.blurRadius.dp else 0.dp,
            onLoadError = { imageError = true },
        )
        // Overlay keeps the timer readable on bright photos; gradient adds depth at top & bottom.
        Box(Modifier.fillMaxSize().background(colors.scrim.copy(alpha = state.screen.overlayAlpha)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to colors.scrim.copy(alpha = 0.35f),
                    0.25f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to colors.scrim.copy(alpha = 0.55f),
                ),
            ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = colors.onScrim)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenBackgrounds) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = stringResource(R.string.focus_a11y_change_background), tint = colors.onScrim)
                }
                IconButton(onClick = { showOptions = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.focus_a11y_options), tint = colors.onScrim)
                }
            }

            if (state.screen.showClock) {
                Text(state.clockText, style = FocusTheme.extraTypography.clock, color = colors.onScrim)
                Spacer(Modifier.height(spacing.sm))
            }
            // The user's own text wins; otherwise one of the built-in (translated) texts.
            val motivation = state.motivation.ifBlank { state.motivationRes?.let { stringResource(it) }.orEmpty() }
            if (state.screen.showMotivation && motivation.isNotBlank()) {
                Text(
                    motivation,
                    style = FocusTheme.typography.labelLarge,
                    color = colors.onSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = spacing.lg, vertical = spacing.sm),
                )
            }
            if (imageError) {
                Spacer(Modifier.height(spacing.sm))
                Text(
                    stringResource(R.string.focus_background_unavailable),
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onScrim,
                )
            }

            Spacer(Modifier.weight(1f))
            TimerRing(state)
            Spacer(Modifier.height(spacing.lg))
            if (state.screen.showMissionTitle) {
                Text(
                    state.mission?.title ?: stringResource(R.string.focus_free_focus),
                    style = FocusTheme.typography.titleMedium,
                    color = colors.onScrim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (state.screen.showSessionDots) {
                Spacer(Modifier.height(spacing.sm))
                SessionDots(done = state.sessionIndex - (if (state.isFinished && state.sessionType == SessionType.FOCUS) 0 else 1), total = state.sessionsPerCycle)
            }
            Spacer(Modifier.weight(1f))

            when {
                state.isFinished -> FinishedActions(
                    state = state,
                    onStartBreak = { tap(); viewModel.startBreak() },
                    onNextFocus = { tap(); viewModel.startNextFocus() },
                    onDone = { viewModel.reset() },
                )
                else -> ControlsRow(
                    state = state,
                    onPrimary = {
                        when {
                            state.isRunning -> { tap(); viewModel.pause() }
                            state.isPaused -> { tap(); viewModel.resume() }
                            else -> startWithPermission()
                        }
                    },
                    onLeft = { if (state.isActive) confirmCancel = true else showMissionPicker = true },
                    onRight = {
                        if (state.isActive) {
                            if (state.earlyFinishCounts) { tap(); viewModel.finish() } else confirmFinish = true
                        } else {
                            onOpenBackgrounds()
                        }
                    },
                )
            }
            Spacer(Modifier.height(spacing.lg))
            BottomChip(
                icon = Icons.Rounded.Wallpaper,
                text = state.background?.displayLabel() ?: stringResource(R.string.focus_background),
                onClick = onOpenBackgrounds,
            )
            Spacer(Modifier.height(spacing.lg))
        }
    }

    if (showOptions) {
        FocusOptionsSheet(
            state = state,
            onDismiss = { showOptions = false },
            onChange = viewModel::updateScreenSettings,
            onOpenPremium = { showOptions = false; onOpenPremium() },
            canUse = viewModel::canUse,
        )
    }
    if (showMissionPicker) {
        MissionPickerSheet(
            state = state,
            onDismiss = { showMissionPicker = false },
            onSelect = { viewModel.selectMission(it); showMissionPicker = false },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.focus_finish_dialog_title)) },
            text = { Text(stringResource(R.string.focus_finish_dialog_text)) },
            confirmButton = { TextButton(onClick = { confirmFinish = false; viewModel.finish() }) { Text(stringResource(R.string.focus_finish_dialog_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.focus_keep_going)) } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.focus_stop_dialog_title)) },
            text = { Text(stringResource(R.string.focus_stop_dialog_text)) },
            confirmButton = { TextButton(onClick = { confirmCancel = false; viewModel.cancel() }) { Text(stringResource(R.string.focus_stop_dialog_confirm), color = FocusTheme.colors.error) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.focus_keep_going)) } },
        )
    }
}

@Composable
private fun TimerRing(state: FocusUiState) {
    val colors = FocusTheme.colors
    val size = FocusTheme.dimens.timerRing
    val stroke = FocusTheme.dimens.timerStroke
    val label = when {
        state.isFinished && state.sessionType == SessionType.FOCUS -> stringResource(R.string.focus_label_well_done)
        state.isFinished -> stringResource(R.string.focus_label_break_over)
        state.sessionType == SessionType.SHORT_BREAK -> stringResource(R.string.focus_label_short_break)
        state.sessionType == SessionType.LONG_BREAK -> stringResource(R.string.focus_label_long_break)
        state.isPaused -> stringResource(R.string.focus_label_paused)
        else -> stringResource(R.string.focus_label_focus)
    }
    val ringDescription = stringResource(R.string.focus_a11y_time_remaining, label, state.timeText)
    val ringColor = if (state.sessionType.isBreak) colors.info else colors.primaryGradientStart
    Box(
        Modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = ringDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawCircle(colors.scrim.copy(alpha = 0.28f), radius = this.size.minDimension / 2)
            drawCircle(colors.onScrim.copy(alpha = 0.35f), radius = this.size.minDimension / 2, style = Stroke(width = 1.5.dp.toPx()))
            drawArc(
                color = colors.onScrim.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            val sweep = 360f * state.progress.coerceIn(0f, 1f)
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            val angle = Math.toRadians((sweep - 90f).toDouble())
            val r = arcSize.width / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            drawCircle(
                colors.onScrim,
                radius = strokePx * 0.7f,
                center = Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = FocusTheme.typography.titleMedium, color = colors.onScrim)
            Text(state.timeText, style = FocusTheme.extraTypography.timer, color = colors.onScrim)
            if (state.sessionType == SessionType.FOCUS) {
                Text("${state.sessionIndex} / ${state.sessionsPerCycle}", style = FocusTheme.typography.labelMedium, color = colors.onScrim.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun SessionDots(done: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i < done) FocusTheme.colors.onScrim else FocusTheme.colors.onScrim.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun ControlsRow(state: FocusUiState, onPrimary: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit) {
    val colors = FocusTheme.colors
    val primaryDescription = when {
        state.isRunning -> stringResource(R.string.focus_a11y_pause)
        state.isPaused -> stringResource(R.string.focus_a11y_resume)
        else -> stringResource(R.string.focus_a11y_start)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        GlassButton(
            icon = if (state.isActive) Icons.Rounded.Close else Icons.Rounded.Checklist,
            description = if (state.isActive) stringResource(R.string.focus_a11y_stop_session) else stringResource(R.string.focus_a11y_choose_mission),
            onClick = onLeft,
        )
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primaryGradientStart, colors.primaryGradientEnd)))
                .border(3.dp, colors.onScrim.copy(alpha = 0.6f), CircleShape)
                .clickable(role = Role.Button, onClick = onPrimary)
                .semantics { contentDescription = primaryDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(42.dp),
            )
        }
        GlassButton(
            icon = if (state.isActive) Icons.Rounded.Check else Icons.Rounded.Wallpaper,
            description = if (state.isActive) stringResource(R.string.focus_a11y_finish_session) else stringResource(R.string.focus_background),
            onClick = onRight,
        )
    }
}

@Composable
private fun GlassButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.onScrim.copy(alpha = 0.18f))
            .border(1.dp, colors.onScrim.copy(alpha = 0.45f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onScrim)
    }
}

@Composable
private fun FinishedActions(state: FocusUiState, onStartBreak: () -> Unit, onNextFocus: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.sessionType == SessionType.FOCUS) {
            val minutes = state.pomodoro.minutesFor(state.nextBreak)
            val label = stringResource(
                if (state.nextBreak == SessionType.LONG_BREAK) R.string.focus_start_long_break else R.string.focus_start_break,
                minutes,
            )
            FocusButton(label, onClick = onStartBreak, leadingIcon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(FocusTheme.spacing.sm))
            FocusButton(stringResource(R.string.focus_skip_break), onClick = onNextFocus, style = FocusButtonStyle.Soft, leadingIcon = Icons.Rounded.SkipNext, modifier = Modifier.fillMaxWidth())
        } else {
            FocusButton(stringResource(R.string.focus_start_next_focus), onClick = onNextFocus, leadingIcon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onDone) { Text(stringResource(R.string.focus_done_for_now), color = FocusTheme.colors.onScrim) }
    }
}

@Composable
private fun BottomChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FocusTheme.shapes.large)
            .background(colors.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary)
        Spacer(Modifier.width(FocusTheme.spacing.md))
        Text(text, style = FocusTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusOptionsSheet(
    state: FocusUiState,
    onDismiss: () -> Unit,
    onChange: ((com.focusgrowing.app.domain.model.FocusScreenSettings) -> com.focusgrowing.app.domain.model.FocusScreenSettings) -> Unit,
    onOpenPremium: () -> Unit,
    canUse: (PremiumFeature) -> Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screen = state.screen
    val colors = FocusTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = FocusTheme.spacing.xl)) {
            Text(
                stringResource(R.string.focus_options_title),
                style = FocusTheme.typography.titleLarge,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = FocusTheme.spacing.lg),
            )
            SettingsToggleRow(Icons.Rounded.AccessTime, stringResource(R.string.focus_option_show_clock), screen.showClock, { v -> onChange { it.copy(showClock = v) } })
            SettingsToggleRow(Icons.Rounded.FormatQuote, stringResource(R.string.focus_option_motivation), screen.showMotivation, { v -> onChange { it.copy(showMotivation = v) } })
            SettingsToggleRow(Icons.Rounded.Flag, stringResource(R.string.focus_option_mission_title), screen.showMissionTitle, { v -> onChange { it.copy(showMissionTitle = v) } })
            SettingsToggleRow(Icons.Rounded.MoreHoriz, stringResource(R.string.focus_option_session_dots), screen.showSessionDots, { v -> onChange { it.copy(showSessionDots = v) } })

            SliderRow(
                title = stringResource(R.string.focus_option_overlay),
                value = screen.overlayAlpha,
                range = 0f..0.9f,
                valueText = stringResource(R.string.format_percent, (screen.overlayAlpha * 100).toInt()),
                onValueChange = { v -> onChange { it.copy(overlayAlpha = v) } },
            )
            if (canUse(PremiumFeature.BACKGROUND_BLUR)) {
                SliderRow(
                    title = stringResource(R.string.focus_option_blur),
                    value = screen.blurRadius,
                    range = 0f..25f,
                    valueText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "${screen.blurRadius.toInt()} dp" else "Android 12+",
                    onValueChange = { v -> onChange { it.copy(blurRadius = v) } },
                )
            } else {
                LockedRow(stringResource(R.string.focus_option_blur), onOpenPremium)
            }
            if (canUse(PremiumFeature.CUSTOM_MOTIVATION)) {
                var text by remember { mutableStateOf(screen.customMotivation.orEmpty()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(60); onChange { s -> s.copy(customMotivation = text) } },
                    label = { Text(stringResource(R.string.focus_option_custom_motivation_label)) },
                    singleLine = true,
                    colors = focusTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
                )
            } else {
                LockedRow(stringResource(R.string.focus_option_custom_motivation_locked), onOpenPremium)
            }
        }
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onValueChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm)) {
        Row {
            Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
            Text(valueText, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = FocusTheme.colors.primary,
                activeTrackColor = FocusTheme.colors.primary,
                inactiveTrackColor = FocusTheme.colors.progressTrack,
            ),
        )
    }
}

@Composable
private fun LockedRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.common_premium), tint = FocusTheme.colors.premium, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.common_premium), style = FocusTheme.typography.labelMedium, color = FocusTheme.colors.premium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionPickerSheet(state: FocusUiState, onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    val colors = FocusTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Text(
            stringResource(R.string.focus_picker_title),
            style = FocusTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
        )
        LazyColumn(Modifier.padding(bottom = FocusTheme.spacing.xl)) {
            item {
                PickerRow(stringResource(R.string.focus_picker_free_focus), selected = state.mission == null) { onSelect(null) }
            }
            items(state.activeMissions, key = { it.id }) { m ->
                PickerRow("${m.title} · ${m.completedPomodoros}/${m.estimatedPomodoros}", selected = state.mission?.id == m.id) { onSelect(m.id) }
            }
        }
    }
}

@Composable
private fun PickerRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (selected) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.focus_a11y_selected), tint = FocusTheme.colors.primary)
    }
}
