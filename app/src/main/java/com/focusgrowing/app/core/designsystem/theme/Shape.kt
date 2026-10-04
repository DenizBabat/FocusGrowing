package com.focusgrowing.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii and spacing — change here to make the whole UI rounder, tighter, etc. */
val FocusMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Immutable
data class FocusSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** Horizontal padding of every screen. */
    val screen: Dp = 20.dp,
    /** Minimum touch target (accessibility). */
    val touchTarget: Dp = 48.dp,
)

@Immutable
data class FocusDimens(
    val buttonHeight: Dp = 54.dp,
    val progressHeight: Dp = 8.dp,
    val iconBadge: Dp = 40.dp,
    val iconBadgeLarge: Dp = 56.dp,
    val cardElevation: Dp = 0.dp,
    val timerRing: Dp = 250.dp,
    val timerStroke: Dp = 10.dp,
)
