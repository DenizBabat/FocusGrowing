package com.focusgrowing.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.theme.FocusTheme

// ---------------------------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------------------------

@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(FocusTheme.spacing.lg),
    color: Color = FocusTheme.colors.surface,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = FocusTheme.shapes.large
    val stroke = if (border) BorderStroke(1.dp, FocusTheme.colors.cardBorder) else null
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

enum class FocusButtonStyle { Primary, Premium, Danger, Soft }

@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FocusButtonStyle = FocusButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = FocusTheme.colors
    val (brush, content) = when (style) {
        FocusButtonStyle.Primary -> Brush.horizontalGradient(listOf(colors.primaryGradientStart, colors.primaryGradientEnd)) to colors.onPrimary
        FocusButtonStyle.Premium -> Brush.horizontalGradient(listOf(colors.premium, colors.xp)) to colors.onPremium
        FocusButtonStyle.Danger -> Brush.horizontalGradient(listOf(colors.danger, colors.danger)) to Color.White
        FocusButtonStyle.Soft -> Brush.horizontalGradient(listOf(colors.primaryContainer, colors.primaryContainer)) to colors.onPrimaryContainer
    }
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .heightIn(min = FocusTheme.dimens.buttonHeight)
            .clip(CircleShape)
            .background(brush, alpha = alpha)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = FocusTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.let {
                    Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(FocusTheme.spacing.sm))
                }
                Text(text, style = FocusTheme.typography.labelLarge, color = content.copy(alpha = alpha))
                trailingIcon?.let {
                    Spacer(Modifier.width(FocusTheme.spacing.sm))
                    Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun FocusTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = FocusTheme.colors.onSurfaceVariant) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, style = FocusTheme.typography.labelLarge, color = color)
    }
}

/** Round icon button with a soft background (Focus screen controls, add button). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    container: Color = FocusTheme.colors.primaryContainer,
    tint: Color = FocusTheme.colors.primary,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

// ---------------------------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------------------------

@Composable
fun FocusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = FocusTheme.colors.primary,
    trackColor: Color = FocusTheme.colors.progressTrack,
    height: Dp = FocusTheme.dimens.progressHeight,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (FocusTheme.reduceMotion) 0 else 600),
        label = "progress",
    )
    val percentDescription = stringResource(R.string.a11y_percent, (progress * 100).toInt())
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { contentDescription = percentDescription },
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Headers
// ---------------------------------------------------------------------------------------------

@Composable
fun FocusTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    contentColor: Color = FocusTheme.colors.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = FocusTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = contentColor)
            }
        } else {
            Spacer(Modifier.width(FocusTheme.spacing.md))
        }
        Text(
            text = title.orEmpty(),
            style = FocusTheme.typography.titleLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FocusTheme.spacing.screen, vertical = FocusTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
            subtitle?.let {
                Text(it, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = FocusTheme.typography.titleMedium, color = FocusTheme.colors.onBackground, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = FocusTheme.typography.labelMedium,
                color = FocusTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chips / tabs
// ---------------------------------------------------------------------------------------------

@Composable
fun FocusChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lockedIndices: Set<Int> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(horizontal = FocusTheme.spacing.screen),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm),
    ) {
        options.forEachIndexed { index, label ->
            FocusChip(label, selected = index == selectedIndex, locked = index in lockedIndices, onClick = { onSelect(index) })
        }
    }
}

@Composable
fun FocusChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, locked: Boolean = false) {
    val colors = FocusTheme.colors
    val bg = if (selected) colors.primary else colors.surface
    val fg = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) colors.primary else colors.cardBorder, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = FocusTheme.typography.labelMedium, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        if (locked) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.common_premium), tint = fg, modifier = Modifier.size(12.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------------------------

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = FocusTheme.dimens.iconBadge,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(modifier.size(size).clip(shape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    FocusCard(modifier = modifier, contentPadding = PaddingValues(FocusTheme.spacing.md)) {
        IconBadge(icon, tint, container, size = 34.dp)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(label, style = FocusTheme.typography.labelMedium, color = FocusTheme.colors.onSurfaceVariant, maxLines = 1)
        Text(value, style = FocusTheme.extraTypography.statValue, color = FocusTheme.colors.onSurface, maxLines = 1)
        footer?.invoke()
    }
}

/** Compact vertical stat used on celebration cards (+20 XP / World XP). */
@Composable
fun RewardStat(icon: ImageVector, value: String, label: String, tint: Color, container: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = FocusTheme.spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, tint, container, size = 44.dp)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(value, style = FocusTheme.typography.titleMedium, color = tint, textAlign = TextAlign.Center)
        Text(label, style = FocusTheme.typography.labelSmall, color = FocusTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .heightIn(min = 56.dp)
            .padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = FocusTheme.colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(FocusTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface)
            subtitle?.let { Text(it, style = FocusTheme.typography.bodySmall, color = FocusTheme.colors.onSurfaceVariant) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = FocusTheme.colors.primary,
                checkedThumbColor = FocusTheme.colors.onPrimary,
                uncheckedTrackColor = FocusTheme.colors.progressTrack,
                uncheckedBorderColor = FocusTheme.colors.cardBorder,
            ),
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = FocusTheme.colors.onSurfaceVariant,
    iconContainer: Color? = null,
    locked: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = FocusTheme.spacing.lg, vertical = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconContainer != null) {
            IconBadge(icon, iconTint, iconContainer, size = 38.dp, shape = RoundedCornerShape(12.dp))
        } else {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(FocusTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurface)
            subtitle?.let { Text(it, style = FocusTheme.typography.bodySmall, color = FocusTheme.colors.onSurfaceVariant) }
        }
        if (locked) Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.common_premium), tint = FocusTheme.colors.premium, modifier = Modifier.size(16.dp))
        value?.let { Text(it, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant) }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = FocusTheme.colors.outline)
    }
}

@Composable
fun SettingsGroup(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        title?.let {
            Text(
                it,
                style = FocusTheme.typography.titleSmall,
                color = FocusTheme.colors.onBackground,
                modifier = Modifier.padding(start = FocusTheme.spacing.xs, bottom = FocusTheme.spacing.sm),
            )
        }
        FocusCard(contentPadding = PaddingValues(vertical = FocusTheme.spacing.xs), content = content)
    }
}

/** "Advanced Statistics 🔒 — Understand your focus patterns. [Unlock Premium]" */
@Composable
fun PremiumLockedCard(title: String, description: String, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    FocusCard(modifier = modifier.fillMaxWidth(), color = FocusTheme.colors.premiumContainer.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = FocusTheme.typography.titleMedium, color = FocusTheme.colors.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.common_locked), tint = FocusTheme.colors.premium)
        }
        Spacer(Modifier.height(FocusTheme.spacing.xs))
        Text(description, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.md))
        FocusButton(stringResource(R.string.common_unlock_premium), onClick = onUnlock, style = FocusButtonStyle.Premium, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(FocusTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, FocusTheme.colors.primary, FocusTheme.colors.primaryContainer, size = 64.dp)
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        Text(title, style = FocusTheme.typography.titleMedium, color = FocusTheme.colors.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(FocusTheme.spacing.xs))
        Text(message, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.let {
            Spacer(Modifier.height(FocusTheme.spacing.lg))
            it()
        }
    }
}

@Composable
fun PriorityDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
