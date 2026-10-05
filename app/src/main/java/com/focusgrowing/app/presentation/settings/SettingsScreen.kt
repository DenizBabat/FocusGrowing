package com.focusgrowing.app.presentation.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.BuildConfig
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.CircleIconButton
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusButtonStyle
import com.focusgrowing.app.core.designsystem.component.FocusChip
import com.focusgrowing.app.core.designsystem.component.FocusTextButton
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
import com.focusgrowing.app.presentation.common.LanguageDialog
import com.focusgrowing.app.presentation.common.currentLanguageLabel
import com.focusgrowing.app.presentation.common.displayLabel

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
    var languageDialog by remember { mutableStateOf(false) }
    // Locked premium palette the user tapped: offers a 24-hour trial for watching an ad.
    var trialOffer by remember { mutableStateOf<FocusPalette?>(null) }

    // The ad was watched and the palette is unlocked: close the offer.
    LaunchedEffect(state.trialPaletteId) {
        if (trialOffer != null && trialOffer?.id == state.trialPaletteId) trialOffer = null
    }

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
        FocusTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            // Appearance -------------------------------------------------------------------------
            SettingsGroup(stringResource(R.string.settings_group_appearance)) {
                SettingsNavRow(
                    Icons.Rounded.Language, stringResource(R.string.language_title),
                    onClick = { languageDialog = true },
                    value = currentLanguageLabel(),
                )
                ChoiceRow(stringResource(R.string.settings_theme_title), ThemeMode.entries.map { it.label() }, ThemeMode.entries.indexOf(prefs.appearance.themeMode)) { i ->
                    viewModel.updateAppearance { it.copy(themeMode = ThemeMode.entries[i]) }
                }
                PaletteRow(
                    selectedId = prefs.appearance.paletteId,
                    canUsePremium = viewModel.canUse(PremiumFeature.PREMIUM_PALETTES),
                    trialPaletteId = state.trialPaletteId,
                    trialHoursLeft = state.trialHoursLeft,
                    onSelect = { palette -> viewModel.updateAppearance { it.copy(paletteId = palette.id) } },
                    onLocked = { palette ->
                        viewModel.clearAdMessage()
                        trialOffer = palette
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsToggleRow(
                        Icons.Rounded.ColorLens, stringResource(R.string.settings_dynamic_colors_title), prefs.appearance.dynamicColor,
                        { v -> viewModel.updateAppearance { it.copy(dynamicColor = v) } },
                        subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                    )
                }
                ChoiceRow(stringResource(R.string.settings_text_size_title), TextScale.entries.map { it.label() }, TextScale.entries.indexOf(prefs.appearance.textScale)) { i ->
                    viewModel.updateAppearance { it.copy(textScale = TextScale.entries[i]) }
                }
                SettingsToggleRow(
                    Icons.Rounded.Animation, stringResource(R.string.settings_reduce_animations), prefs.appearance.reduceMotion,
                    { v -> viewModel.updateAppearance { it.copy(reduceMotion = v) } },
                )
            }

            // Timer ------------------------------------------------------------------------------
            SettingsGroup(stringResource(R.string.settings_group_timer)) {
                val focus = prefs.pomodoro.focusMinutes
                val presets = PomodoroSettings.FocusPresets
                Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = FocusTheme.colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(spacing.md))
                        Text(stringResource(R.string.settings_focus_length), style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.common_minutes_short, focus), style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.primary)
                    }
                    Spacer(Modifier.height(spacing.sm))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        presets.forEach { m ->
                            FocusChip("$m", selected = focus == m, onClick = { viewModel.updatePomodoro { it.copy(focusMinutes = m) } })
                        }
                        FocusChip(stringResource(R.string.settings_focus_custom), selected = focus !in presets, onClick = { customFocusDialog = true })
                    }
                }
                StepperRow(stringResource(R.string.settings_short_break), prefs.pomodoro.shortBreakMinutes, stringResource(R.string.common_minutes_short, prefs.pomodoro.shortBreakMinutes), 1..60) { v -> viewModel.updatePomodoro { it.copy(shortBreakMinutes = v) } }
                StepperRow(stringResource(R.string.settings_long_break), prefs.pomodoro.longBreakMinutes, stringResource(R.string.common_minutes_short, prefs.pomodoro.longBreakMinutes), 1..90) { v -> viewModel.updatePomodoro { it.copy(longBreakMinutes = v) } }
                StepperRow(
                    stringResource(R.string.settings_long_break_every), prefs.pomodoro.longBreakInterval,
                    pluralStringResource(R.plurals.settings_sessions_count, prefs.pomodoro.longBreakInterval, prefs.pomodoro.longBreakInterval), 2..12,
                ) { v -> viewModel.updatePomodoro { it.copy(longBreakInterval = v) } }
            }

            // Focus screen -----------------------------------------------------------------------
            SettingsGroup(stringResource(R.string.settings_group_focus_screen)) {
                SettingsNavRow(Icons.Rounded.Wallpaper, stringResource(R.string.settings_background), onClick = onOpenBackgrounds, value = state.background?.displayLabel().orEmpty())
                SettingsToggleRow(Icons.Rounded.AccessTime, stringResource(R.string.settings_show_clock), prefs.focusScreen.showClock, { v -> viewModel.updateFocusScreen { it.copy(showClock = v) } })
                SettingsToggleRow(Icons.Rounded.FormatQuote, stringResource(R.string.settings_motivational_text), prefs.focusScreen.showMotivation, { v -> viewModel.updateFocusScreen { it.copy(showMotivation = v) } })
                Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)) {
                    Row {
                        Text(stringResource(R.string.settings_background_overlay), style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.format_percent, (prefs.focusScreen.overlayAlpha * 100).toInt()), style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
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
            SettingsGroup(stringResource(R.string.settings_group_notifications)) {
                SettingsToggleRow(
                    Icons.Rounded.NotificationsActive, stringResource(R.string.settings_session_alerts_title), prefs.notifications.sessionAlerts,
                    { v -> viewModel.updateNotifications { it.copy(sessionAlerts = v) } },
                    subtitle = stringResource(R.string.settings_session_alerts_subtitle),
                )
                SettingsToggleRow(
                    Icons.Rounded.Timer, stringResource(R.string.settings_timer_in_notification_bar), prefs.notifications.ongoingTimerNotification,
                    { v -> viewModel.updateNotifications { it.copy(ongoingTimerNotification = v) } },
                )
                if (!exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsNavRow(
                        Icons.Rounded.Alarm, stringResource(R.string.settings_exact_alarms_title),
                        onClick = {
                            open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        },
                        subtitle = stringResource(R.string.settings_exact_alarms_subtitle),
                    )
                }
                SettingsNavRow(
                    Icons.Rounded.NotificationsActive, stringResource(R.string.settings_system_notification_settings),
                    onClick = {
                        open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                )
            }

            // Sounds -----------------------------------------------------------------------------
            SettingsGroup(stringResource(R.string.settings_group_sounds)) {
                SettingsToggleRow(
                    Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.settings_alert_sound_vibration), prefs.notifications.alertSoundAndVibration,
                    { v -> viewModel.updateNotifications { it.copy(alertSoundAndVibration = v) } },
                )
                SettingsToggleRow(
                    Icons.Rounded.TouchApp, stringResource(R.string.settings_haptic_feedback), prefs.notifications.hapticFeedback,
                    { v -> viewModel.updateNotifications { it.copy(hapticFeedback = v) } },
                )
            }

            // Privacy (only where the law requires the ad consent choice to stay changeable) --------
            if (state.showAdPrivacyOptions) {
                SettingsGroup(stringResource(R.string.settings_group_privacy)) {
                    SettingsNavRow(
                        Icons.Rounded.PrivacyTip, stringResource(R.string.settings_ad_privacy_title),
                        onClick = { context.findActivity()?.let(viewModel::showAdPrivacyOptions) },
                        subtitle = stringResource(R.string.settings_ad_privacy_subtitle),
                    )
                }
            }

            SettingsGroup(stringResource(R.string.settings_group_about)) {
                SettingsNavRow(Icons.Rounded.Info, stringResource(R.string.settings_version), onClick = {}, value = BuildConfig.VERSION_NAME)
            }
            Spacer(Modifier.height(spacing.lg))
        }
    }

    trialOffer?.let { palette ->
        PaletteTrialSheet(
            palette = palette,
            loading = state.adInProgress,
            message = state.adMessage,
            onWatchAd = { context.findActivity()?.let { viewModel.watchAdForPalette(it, palette.id, palette.displayName) } },
            onOpenPremium = {
                trialOffer = null
                onOpenPremium()
            },
            onDismiss = { trialOffer = null },
        )
    }

    if (languageDialog) {
        LanguageDialog(onDismiss = { languageDialog = false })
    }

    if (customFocusDialog) {
        var text by remember { mutableStateOf(prefs.pomodoro.focusMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { customFocusDialog = false },
            title = { Text(stringResource(R.string.settings_custom_focus_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.settings_custom_focus_minutes_label, PomodoroSettings.MIN_MINUTES, PomodoroSettings.MAX_MINUTES)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = focusTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { m -> viewModel.updatePomodoro { it.copy(focusMinutes = m) } }
                    customFocusDialog = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { customFocusDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
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
private fun PaletteRow(
    selectedId: String,
    canUsePremium: Boolean,
    trialPaletteId: String?,
    trialHoursLeft: Int,
    onSelect: (FocusPalette) -> Unit,
    onLocked: (FocusPalette) -> Unit,
) {
    val dark = FocusTheme.isDark
    Column(Modifier.padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm)) {
        Text(stringResource(R.string.settings_color_palette), style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.lg)) {
            FocusPalettes.all.forEach { palette ->
                val tokens = if (dark) palette.dark else palette.light
                // Unlocked by Premium, or for 24 hours by a rewarded ad.
                val locked = palette.isPremium && !canUsePremium && palette.id != trialPaletteId
                val selected = palette.id == selectedId
                val description = if (locked) stringResource(R.string.settings_palette_premium_a11y, palette.displayName) else palette.displayName
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(tokens.primaryGradientStart, tokens.primaryGradientEnd)))
                            .border(if (selected) 3.dp else 1.dp, if (selected) FocusTheme.colors.onSurface else FocusTheme.colors.cardBorder, CircleShape)
                            .clickable(role = Role.RadioButton) { if (locked) onLocked(palette) else onSelect(palette) }
                            .semantics { contentDescription = description },
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
        val trialName = FocusPalettes.all.firstOrNull { it.id == trialPaletteId }?.displayName
        if (trialName != null && !canUsePremium) {
            Spacer(Modifier.height(FocusTheme.spacing.sm))
            Text(
                if (trialHoursLeft <= 1) stringResource(R.string.settings_palette_trial_under_hour, trialName)
                else pluralStringResource(R.plurals.settings_palette_trial_hours_left, trialHoursLeft, trialName, trialHoursLeft),
                style = FocusTheme.typography.bodySmall,
                color = FocusTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/** Offer shown when a free user taps a locked premium palette: watch an ad for 24 hours, or go Premium. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteTrialSheet(
    palette: FocusPalette,
    loading: Boolean,
    message: String?,
    onWatchAd: () -> Unit,
    onOpenPremium: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screen)
                .padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.settings_palette_trial_sheet_title, palette.displayName), style = FocusTheme.typography.titleLarge, color = colors.onSurface)
            Text(
                stringResource(R.string.settings_palette_trial_sheet_text),
                style = FocusTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (message != null) {
                Text(message, style = FocusTheme.typography.bodySmall, color = colors.error)
            }
            FocusButton(
                stringResource(R.string.settings_watch_ad),
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Rounded.PlayArrow,
                loading = loading,
            )
            FocusButton(stringResource(R.string.settings_see_premium), onClick = onOpenPremium, modifier = Modifier.fillMaxWidth(), style = FocusButtonStyle.Soft)
            FocusTextButton(stringResource(R.string.settings_not_now), onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun StepperRow(title: String, value: Int, valueText: String, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
        CircleIconButton(
            Icons.Rounded.Remove, stringResource(R.string.settings_stepper_decrease, title),
            onClick = { onChange((value - 1).coerceIn(range)) }, size = 34.dp, enabled = value > range.first,
        )
        Text(
            valueText,
            style = FocusTheme.typography.bodyMedium,
            color = FocusTheme.colors.onSurface,
            modifier = Modifier.width(92.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        CircleIconButton(
            Icons.Rounded.Add, stringResource(R.string.settings_stepper_increase, title),
            onClick = { onChange((value + 1).coerceIn(range)) }, size = 34.dp, enabled = value < range.last,
        )
    }
}

@Composable
private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun TextScale.label() = when (this) {
    TextScale.SMALL -> stringResource(R.string.settings_text_size_small)
    TextScale.MEDIUM -> stringResource(R.string.settings_text_size_medium)
    TextScale.LARGE -> stringResource(R.string.settings_text_size_large)
}
