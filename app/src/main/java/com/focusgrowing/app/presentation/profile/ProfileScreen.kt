package com.focusgrowing.app.presentation.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.component.ScreenTitle
import com.focusgrowing.app.core.designsystem.component.SettingsNavRow
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.illustration.LandscapeBackdrop
import com.focusgrowing.app.core.designsystem.illustration.WorldIsland
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.utility.AppConfig

@Composable
fun ProfileScreen(
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val context = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    val noAppFound = stringResource(R.string.profile_no_app_found)

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, noAppFound, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenTitle(stringResource(R.string.profile_title))
        Column(Modifier.padding(horizontal = spacing.screen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(2.dp, colors.primaryContainer, CircleShape)
                        .background(colors.surfaceVariant),
                ) {
                    LandscapeBackdrop(Modifier.matchParentSize(), showLake = false)
                    WorldIsland(state.worldItems, Modifier.matchParentSize().padding(6.dp))
                }
                Spacer(Modifier.width(spacing.lg))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editName = true }) {
                        Text(
                            state.name.ifBlank { stringResource(R.string.profile_add_name) },
                            style = FocusTheme.typography.titleLarge,
                            color = if (state.name.isBlank()) colors.onSurfaceVariant else colors.onBackground,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.profile_edit_name), tint = colors.outline, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(state.titleRes), style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.profile_world_level), style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Text(state.world.level.toString(), style = FocusTheme.typography.headlineSmall, color = colors.onSurface)
                    }
                    Text(stringResource(R.string.profile_xp_progress, state.world.xpIntoLevel, state.world.xpForNextLevel), style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(spacing.sm))
                FocusProgressBar(state.world.progress)
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = spacing.xs)) {
                SettingsNavRow(
                    Icons.Rounded.WorkspacePremium,
                    stringResource(R.string.common_premium),
                    onClick = onOpenPremium,
                    subtitle = if (state.isPremium) stringResource(R.string.profile_premium_active) else stringResource(R.string.profile_premium_upgrade),
                    iconTint = colors.premium,
                    iconContainer = colors.premiumContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Notifications,
                    stringResource(R.string.notifications_title),
                    onClick = onOpenNotifications,
                    subtitle = if (state.unread > 0) pluralStringResource(R.plurals.profile_notifications_new, state.unread, state.unread)
                    else stringResource(R.string.profile_notifications_subtitle),
                    iconTint = colors.info,
                    iconContainer = colors.infoContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Settings,
                    stringResource(R.string.profile_app_settings),
                    onClick = onOpenSettings,
                    subtitle = stringResource(R.string.profile_app_settings_subtitle),
                    iconTint = colors.primary,
                    iconContainer = colors.primaryContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Wallpaper,
                    stringResource(R.string.profile_background_gallery),
                    onClick = onOpenBackgrounds,
                    subtitle = stringResource(R.string.profile_background_gallery_subtitle),
                    iconTint = colors.accentPurple,
                    iconContainer = colors.accentPurpleContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    stringResource(R.string.profile_help_support),
                    onClick = {
                        open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}")))
                    },
                    subtitle = stringResource(R.string.profile_help_support_subtitle),
                    iconTint = colors.streak,
                    iconContainer = colors.streakContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Policy,
                    stringResource(R.string.profile_privacy_policy),
                    onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL))) },
                    subtitle = stringResource(R.string.profile_privacy_policy_subtitle),
                    iconTint = colors.onSurfaceVariant,
                    iconContainer = colors.surfaceMuted,
                )
            }
            Spacer(Modifier.height(spacing.xl))
        }
    }

    if (editName) {
        var text by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(stringResource(R.string.profile_name_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    singleLine = true,
                    colors = focusTextFieldColors(),
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.setName(text); editName = false }) { Text(stringResource(R.string.common_save)) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = FocusTheme.colors.divider, modifier = Modifier.padding(start = 70.dp))
}
