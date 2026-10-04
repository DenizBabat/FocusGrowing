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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenTitle("Profile")
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
                            state.name.ifBlank { "Add your name" },
                            style = FocusTheme.typography.titleLarge,
                            color = if (state.name.isBlank()) colors.onSurfaceVariant else colors.onBackground,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit name", tint = colors.outline, modifier = Modifier.size(16.dp))
                    }
                    Text(state.title, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("World Level", style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Text(state.world.level.toString(), style = FocusTheme.typography.headlineSmall, color = colors.onSurface)
                    }
                    Text("${state.world.xpIntoLevel} / ${state.world.xpForNextLevel} XP", style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(spacing.sm))
                FocusProgressBar(state.world.progress)
            }

            Spacer(Modifier.height(spacing.lg))
            FocusCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = spacing.xs)) {
                SettingsNavRow(
                    Icons.Rounded.WorkspacePremium,
                    "Premium",
                    onClick = onOpenPremium,
                    subtitle = if (state.isPremium) "Active — thank you!" else "Upgrade for more features",
                    iconTint = colors.premium,
                    iconContainer = colors.premiumContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Notifications,
                    "Notifications",
                    onClick = onOpenNotifications,
                    subtitle = if (state.unread > 0) "${state.unread} new" else "Your recent activity",
                    iconTint = colors.info,
                    iconContainer = colors.infoContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Settings,
                    "App Settings",
                    onClick = onOpenSettings,
                    subtitle = "Theme, timer, notifications, sound",
                    iconTint = colors.primary,
                    iconContainer = colors.primaryContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Wallpaper,
                    "Background Gallery",
                    onClick = onOpenBackgrounds,
                    subtitle = "Change your focus background",
                    iconTint = colors.accentPurple,
                    iconContainer = colors.accentPurpleContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    "Help & Support",
                    onClick = {
                        open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}")))
                    },
                    subtitle = "Contact us",
                    iconTint = colors.streak,
                    iconContainer = colors.streakContainer,
                )
                Divider()
                SettingsNavRow(
                    Icons.Rounded.Policy,
                    "Privacy Policy",
                    onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL))) },
                    subtitle = "How we handle your data",
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
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    singleLine = true,
                    colors = focusTextFieldColors(),
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.setName(text); editName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = FocusTheme.colors.divider, modifier = Modifier.padding(start = 70.dp))
}
