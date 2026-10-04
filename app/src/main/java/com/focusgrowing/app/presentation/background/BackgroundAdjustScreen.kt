package com.focusgrowing.app.presentation.background

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.EmptyState
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusChip
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.image.BackgroundImageView

/**
 * Camera/Gallery → Preview → Adjust → Save (spec §17).
 * The preview frame uses the phone's own aspect ratio, so what you see is exactly what the
 * Focus screen will show. Free: position presets + drag. Premium: pinch-to-zoom & zoom slider.
 */
@Composable
fun BackgroundAdjustScreen(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: BackgroundAdjustViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val background = state.background
    val isCustom = background != null && !background.isDefault

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        FocusTopBar(title = if (isCustom) "Adjust background" else "Preview", onBack = onBack) {
            if (isCustom) {
                IconButton(onClick = viewModel::reset) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset position", tint = colors.onBackground)
                }
            }
        }
        if (state.notFound) {
            EmptyState(Icons.Rounded.Lock, "Background not available", "It may have been removed.", action = { FocusButton("Go back", onClick = onBack) })
        } else if (background != null) {

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(0.62f)) {
                // Same aspect ratio as this device's screen.
                val screenRatio = with(androidx.compose.ui.platform.LocalConfiguration.current) {
                    screenWidthDp.toFloat() / screenHeightDp.toFloat().coerceAtLeast(1f)
                }
                val frameWidth = maxWidth
                val frameHeight = frameWidth / screenRatio
                Box(
                    Modifier
                        .width(frameWidth)
                        .height(frameHeight)
                        .clip(FocusTheme.shapes.extraLarge)
                        .background(colors.scrim)
                        .then(
                            if (isCustom) {
                                Modifier
                                    .pointerInput(frameWidth) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            viewModel.drag(pan.x / size.width, pan.y / size.height)
                                            if (zoom != 1f) viewModel.zoomBy(zoom)
                                        }
                                    }
                                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { viewModel.reset() }) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    BackgroundImageView(
                        background = background,
                        modifier = Modifier.fillMaxSize(),
                        alignXOverride = state.alignX,
                        alignYOverride = state.alignY,
                        zoomOverride = state.zoom,
                    )
                    Box(Modifier.fillMaxSize().background(colors.scrim.copy(alpha = state.overlayAlpha)))
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Focus", style = FocusTheme.typography.labelMedium, color = colors.onScrim)
                        Text("25:00", style = FocusTheme.typography.displaySmall, color = colors.onScrim)
                    }
                }
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                if (isCustom) "Drag to position · double-tap to reset" else "Built-in backgrounds always fit your screen perfectly.",
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (isCustom) {
                Spacer(Modifier.height(spacing.lg))
                Text("Position", style = FocusTheme.typography.titleSmall, color = colors.onBackground, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(spacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    FocusChip("Top", selected = state.alignY == -1f && state.alignX == 0f, onClick = { viewModel.preset(0f, -1f) }, modifier = Modifier.weight(1f))
                    FocusChip("Center", selected = state.alignY == 0f && state.alignX == 0f, onClick = { viewModel.preset(0f, 0f) }, modifier = Modifier.weight(1f))
                    FocusChip("Bottom", selected = state.alignY == 1f && state.alignX == 0f, onClick = { viewModel.preset(0f, 1f) }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(spacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    FocusChip("Left", selected = state.alignX == -1f && state.alignY == 0f, onClick = { viewModel.preset(-1f, 0f) }, modifier = Modifier.weight(1f))
                    FocusChip("Right", selected = state.alignX == 1f && state.alignY == 0f, onClick = { viewModel.preset(1f, 0f) }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(spacing.lg))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom", style = FocusTheme.typography.titleSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (!state.canZoom) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.premium)
                        Spacer(Modifier.width(4.dp))
                        Text("Premium", style = FocusTheme.typography.labelMedium, color = colors.premium, modifier = Modifier.padding(end = 4.dp))
                    } else {
                        Text("%.1fx".format(state.zoom), style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
                if (state.canZoom) {
                    Slider(
                        value = state.zoom,
                        onValueChange = viewModel::setZoom,
                        valueRange = 1f..3f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.progressTrack),
                    )
                } else {
                    FocusChip("Unlock advanced crop editor", selected = false, locked = true, onClick = onOpenPremium, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(spacing.lg))
            Row(Modifier.fillMaxWidth()) {
                Text("Darken", style = FocusTheme.typography.titleSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
                Text("${(state.overlayAlpha * 100).toInt()}%", style = FocusTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            Slider(
                value = state.overlayAlpha,
                onValueChange = viewModel::setOverlay,
                valueRange = 0f..0.9f,
                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.progressTrack),
            )
            Spacer(Modifier.height(spacing.lg))
        }
        FocusButton(
            "Use this background",
            onClick = { viewModel.save(onBack) },
            leadingIcon = Icons.Rounded.Check,
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screen, vertical = spacing.md),
        )
        }
    }
}
