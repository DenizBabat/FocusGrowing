package com.focusgrowing.app.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.BuildConfig
import com.focusgrowing.app.core.designsystem.component.CircleIconButton
import com.focusgrowing.app.core.designsystem.component.FocusChip
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.SettingsGroup
import com.focusgrowing.app.core.designsystem.component.SettingsNavRow
import com.focusgrowing.app.core.designsystem.component.SettingsToggleRow
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.theme.FocusPalette
import com.focusgrowing.app.core.designsystem.theme.FocusPalettes
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.TextScale
import com.focusgrowing.app.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs
    val spacing = FocusTheme.spacing
    val context = LocalContext.current
    var exactAlarms by remember { mutableStateOf(true) }
    var customFocusDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        exactAlarms = viewModel.canScheduleExactAlarms()
        onPauseOrDispose { }
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Settings page not available on this device.
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        FocusTopBar(title = "Settings", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            // Appearance -------------------------------------------------------------------------
            SettingsGroup("Appearance") {
                ChoiceRow("Theme", ThemeMode.entries.map { it.label() }, ThemeMode.entries.indexOf(prefs.appearance.themeMode)) { i ->
                    viewModel.updateAppearance { it.copy(themeMode = ThemeMode.entries[i]) }
                }
                PaletteRow(
                    selectedId = prefs.appearance.paletteId,
                    canUsePremium = viewModel.canUse(PremiumFeature.PREMIUM_PALETTES),
                    onSelect = { palette -> viewModel.updateAppearance { it.copy(paletteId = palette.id) } },
                    onLocked = onOpenPremium,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsToggleRow(
                        Icons.Rounded.ColorLens, "Dynamic colors", prefs.appearance.dynamicColor,
                        { v -> viewModel.updateAppearance { it.copy(dynamicColor = v) } },
                        subtitle = "Use your wallpaper colors",
                    )
                }
                ChoiceRow("Text size", TextScale.entries.map { it.label() }, TextScale.entries.indexOf(prefs.appearance.textScale)) { i ->
                    viewModel.updateAppearance { it.copy(textScale = TextScale.entries[i]) }
                }
                SettingsToggleRow(
                    Icons.Rounded.Animation, "Reduce animations", prefs.appearance.reduceMotion,
                    { v -> viewModel.updateAppearance { it.copy(reduceMotion = v) } },
                )
            }

            // Timer ------------------------------------------------------------------------------
            SettingsGroup("Timer") {
                val focus = prefs.pomodoro.focusMinutes
                val presets = PomodoroSettings.FocusPresets
                Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = FocusTheme.colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(spacing.md))
                        Text("Focus", style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
                        Text("$focus min", style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.primary)
                    }
                    Spacer(Modifier.height(spacing.sm))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        presets.forEach { m ->
                            FocusChip("$m", selected = focus == m, onClick = { viewModel.updatePomodoro { it.copy(focusMinutes = m) } })
                        }
                        FocusChip("Custom", selected = focus !in presets, onClick = { customFocusDialog = true })
                    }
                }
                StepperRow("Short break", prefs.pomodoro.shortBreakMinutes, "min", 1..60) { v -> viewModel.updatePomodoro { it.copy(shortBreakMinutes = v) } }
                StepperRow("Long break", prefs.pomodoro.longBreakMinutes, "min", 1..90) { v -> viewModel.updatePomodoro { it.copy(longBreakMinutes = v) } }
                StepperRow("Long break every", prefs.pomodoro.longBreakInterval, "sessions", 2..12) { v -> viewModel.updatePomodoro { it.copy(longBreakInterval = v) } }
            }

            // Focus screen -----------------------------------------------------------------------
            SettingsGroup("Focus screen") {
                SettingsNavRow(Icons.Rounded.Wallpaper, "Background", onClick = onOpenBackgrounds, value = state.backgroundName)
                SettingsToggleRow(Icons.Rounded.AccessTime, "Show clock", prefs.focusScreen.showClock, { v -> viewModel.updateFocusScreen { it.copy(showClock = v) } })
                SettingsToggleRow(Icons.Rounded.FormatQuote, "Motivational text", prefs.focusScreen.showMotivation, { v -> viewModel.updateFocusScreen { it.copy(showMotivation = v) } })
                Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)) {
                    Row {
                        Text("Background overlay", style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
                        Text("${(prefs.focusScreen.overlayAlpha * 100).toInt()}%", style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
                    }
                    Slider(
                        value = prefs.focusScreen.overlayAlpha,
                        onValueChange = { v -> viewModel.updateFocusScreen { it.copy(overlayAlpha = v) } },
                        valueRange = 0f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = FocusTheme.colors.primary,
                            activeTrackColor = FocusTheme.colors.primary,
                            inactiveTrackColor = FocusTheme.colors.progressTrack,
                        ),
                    )
                }
            }

            // Notifications ----------------------------------------------------------------------
            SettingsGroup("Notifications") {
                SettingsToggleRow(
                    Icons.Rounded.NotificationsActive, "Session alerts", prefs.notifications.sessionAlerts,
                    { v -> viewModel.updateNotifications { it.copy(sessionAlerts = v) } },
                    subtitle = "When a focus session or break ends",
                )
                SettingsToggleRow(
                    Icons.Rounded.Timer, "Timer in notification bar", prefs.notifications.ongoingTimerNotification,
                    { v -> viewModel.updateNotifications { it.copy(ongoingTimerNotification = v) } },
                )
                if (!exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsNavRow(
                        Icons.Rounded.Alarm, "Allow exact alarms",
                        onClick = {
                            open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        },
                        subtitle = "Sessions end exactly on time, even when the phone sleeps",
                    )
                }
                SettingsNavRow(
                    Icons.Rounded.NotificationsActive, "System notification settings",
                    onClick = {
                        open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                )
            }

            // Sounds -----------------------------------------------------------------------------
            SettingsGroup("Sounds & Vibration") {
                SettingsToggleRow(
                    Icons.AutoMirrored.Rounded.VolumeUp, "Alert sound & vibration", prefs.notifications.alertSoundAndVibration,
                    { v -> viewModel.updateNotifications { it.copy(alertSoundAndVibration = v) } },
                )
                SettingsToggleRow(
                    Icons.Rounded.TouchApp, "Haptic feedback", prefs.notifications.hapticFeedback,
                    { v -> viewModel.updateNotifications { it.copy(hapticFeedback = v) } },
                )
            }

            SettingsGroup("About") {
                SettingsNavRow(Icons.Rounded.Info, "Version", onClick = {}, value = BuildConfig.VERSION_NAME)
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }

    if (customFocusDialog) {
        var text by remember { mutableStateOf(prefs.pomodoro.focusMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { customFocusDialog = false },
            title = { Text("Custom focus length") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text("Minutes (${PomodoroSettings.MIN_MINUTES}–${PomodoroSettings.MAX_MINUTES})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = focusTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { m -> viewModel.updatePomodoro { it.copy(focusMinutes = m) } }
                    customFocusDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { customFocusDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm)) {
        Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            options.forEachIndexed { i, label ->
                FocusChip(label, selected = i == selected, onClick = { onSelect(i) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PaletteRow(selectedId: String, canUsePremium: Boolean, onSelect: (FocusPalette) -> Unit, onLocked: () -> Unit) {
    val dark = FocusTheme.isDark
    Column(Modifier.padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm)) {
        Text("Color palette", style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.lg)) {
            FocusPalettes.all.forEach { palette ->
                val tokens = if (dark) palette.dark else palette.light
                val locked = palette.isPremium && !canUsePremium
                val selected = palette.id == selectedId
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(tokens.primaryGradientStart, tokens.primaryGradientEnd)))
                            .border(if (selected) 3.dp else 1.dp, if (selected) FocusTheme.colors.onSurface else FocusTheme.colors.cardBorder, CircleShape)
                            .clickable(role = Role.RadioButton) { if (locked) onLocked() else onSelect(palette) }
                            .semantics { contentDescription = palette.displayName + if (locked) ", premium" else "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            locked -> Icon(Icons.Rounded.Lock, contentDescription = null, tint = tokens.onPrimary, modifier = Modifier.size(16.dp))
                            selected -> Icon(Icons.Rounded.Check, contentDescription = null, tint = tokens.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(palette.displayName, style = FocusTheme.typography.labelSmall, color = FocusTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StepperRow(title: String, value: Int, unit: String, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
        CircleIconButton(
            Icons.Rounded.Remove, "Decrease $title",
            onClick = { onChange((value - 1).coerceIn(range)) }, size = 34.dp, enabled = value > range.first,
        )
        Text(
            "$value $unit",
            style = FocusTheme.typography.bodyMedium,
            color = FocusTheme.colors.onSurface,
            modifier = Modifier.width(92.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        CircleIconButton(
            Icons.Rounded.Add, "Increase $title",
            onClick = { onChange((value + 1).coerceIn(range)) }, size = 34.dp, enabled = value < range.last,
        )
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun TextScale.label() = when (this) {
    TextScale.SMALL -> "Small"
    TextScale.MEDIUM -> "Medium"
    TextScale.LARGE -> "Large"
}
