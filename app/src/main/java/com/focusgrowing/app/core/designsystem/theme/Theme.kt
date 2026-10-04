package com.focusgrowing.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalFocusColors = staticCompositionLocalOf { FocusPalettes.Default.light }
val LocalFocusSpacing = staticCompositionLocalOf { FocusSpacing() }
val LocalFocusDimens = staticCompositionLocalOf { FocusDimens() }
val LocalFocusExtraTypography = staticCompositionLocalOf { DefaultExtraTypography }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * Root theme of the app.
 *
 * Every screen reads design values through [FocusTheme] (or [MaterialTheme] for standard roles),
 * so swapping [palette] re-colors the whole app — including illustrations.
 *
 * @param dynamicColor Android 12+ wallpaper colors for the brand roles. Gamification colors
 *                     (XP, streak, charts, illustrations) always come from [palette].
 * @param textScale    Extra multiplier on top of the system font size (Settings → Text size).
 */
@Composable
fun FocusTheme(
    palette: FocusPalette = FocusPalettes.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    textScale: Float = 1f,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseTokens = if (darkTheme) palette.dark else palette.light

    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme: ColorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> baseTokens.toColorScheme(darkTheme)
    }
    val tokens = remember(baseTokens, colorScheme, useDynamic) {
        if (useDynamic) baseTokens.withBrandFrom(colorScheme) else baseTokens
    }

    val density = LocalDensity.current
    val scaledDensity = remember(density, textScale) {
        Density(density.density, density.fontScale * textScale)
    }

    CompositionLocalProvider(
        LocalFocusColors provides tokens,
        LocalFocusSpacing provides FocusSpacing(),
        LocalFocusDimens provides FocusDimens(),
        LocalFocusExtraTypography provides DefaultExtraTypography,
        LocalReduceMotion provides reduceMotion,
        LocalIsDarkTheme provides darkTheme,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FocusMaterialTypography,
            shapes = FocusMaterialShapes,
            content = content,
        )
    }
}

/** Convenient accessors: `FocusTheme.colors.xp`, `FocusTheme.spacing.lg`, ... */
object FocusTheme {
    val colors: FocusColorTokens
        @Composable @ReadOnlyComposable get() = LocalFocusColors.current
    val spacing: FocusSpacing
        @Composable @ReadOnlyComposable get() = LocalFocusSpacing.current
    val dimens: FocusDimens
        @Composable @ReadOnlyComposable get() = LocalFocusDimens.current
    val typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography
    val extraTypography: FocusExtraTypography
        @Composable @ReadOnlyComposable get() = LocalFocusExtraTypography.current
    val shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDarkTheme.current
}

private fun FocusColorTokens.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = error,
        onError = onError,
        errorContainer = dangerContainer,
        onErrorContainer = onSurface,
        outline = outline,
        outlineVariant = cardBorder,
        scrim = scrim,
        surfaceBright = surface,
        surfaceDim = surfaceVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
    )
}

private fun FocusColorTokens.withBrandFrom(scheme: ColorScheme) = copy(
    primary = scheme.primary,
    onPrimary = scheme.onPrimary,
    primaryContainer = scheme.primaryContainer,
    onPrimaryContainer = scheme.onPrimaryContainer,
    primaryGradientStart = scheme.primary.copy(alpha = 0.85f),
    primaryGradientEnd = scheme.primary,
    background = scheme.background,
    onBackground = scheme.onBackground,
    surface = scheme.surface,
    onSurface = scheme.onSurface,
    onSurfaceVariant = scheme.onSurfaceVariant,
    chartPrimary = scheme.primary,
)
