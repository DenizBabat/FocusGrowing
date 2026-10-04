package com.focusgrowing.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Role-based color tokens for ONE mode (light or dark).
 *
 * Screens never use raw `Color(0xFF...)` values. They read these roles through
 * `FocusTheme.colors` (or `MaterialTheme.colorScheme` for standard Material roles).
 * To re-skin the app, create a new [FocusPalette] in Palettes.kt — no screen code changes.
 */
@Immutable
data class FocusColorTokens(
    // Brand
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    /** Two stops used for pill buttons and highlighted surfaces. */
    val primaryGradientStart: Color,
    val primaryGradientEnd: Color,

    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Surfaces & text
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    /** Very soft tint used behind grouped content (chips, segmented controls). */
    val surfaceMuted: Color,
    val cardBorder: Color,
    val outline: Color,
    val divider: Color,
    /** Dim layer drawn over photos (Focus screen). Alpha is applied at draw time. */
    val scrim: Color,
    val onScrim: Color,

    // Feedback & gamification
    val error: Color,
    val onError: Color,
    val success: Color,
    val xp: Color,
    val xpContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val accentPurple: Color,
    val accentPurpleContainer: Color,
    val premium: Color,
    val premiumContainer: Color,
    val onPremium: Color,
    val danger: Color,
    val dangerContainer: Color,

    // Progress, priorities, charts
    val progressTrack: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,

    /** Colors used by the hand-drawn world / onboarding illustrations. */
    val illustration: IllustrationColors,
)

@Immutable
data class IllustrationColors(
    val skyTop: Color,
    val skyBottom: Color,
    val sun: Color,
    val cloud: Color,
    val mountainFar: Color,
    val mountainNear: Color,
    val snow: Color,
    val water: Color,
    val waterDeep: Color,
    val grass: Color,
    val grassDark: Color,
    val foliage: Color,
    val foliageDark: Color,
    val trunk: Color,
    val soil: Color,
    val soilDark: Color,
    val roof: Color,
    val wall: Color,
    val window: Color,
    val flower: Color,
    val flowerAlt: Color,
    val rock: Color,
    val paper: Color,
    val paperLine: Color,
)
