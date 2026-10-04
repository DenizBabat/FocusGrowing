package com.focusgrowing.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Single place to change the app font.
 * To use a custom font (the mockups look like Nunito / Quicksand):
 * 1. Put the .ttf files in app/src/main/res/font (e.g. nunito_regular.ttf, nunito_bold.ttf).
 * 2. Replace the line below with:
 *    FontFamily(Font(R.font.nunito_regular), Font(R.font.nunito_bold, FontWeight.Bold), ...)
 */
val AppFontFamily: FontFamily = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = letterSpacing.sp,
)

val FocusMaterialTypography = Typography(
    displayLarge = style(52, 60, FontWeight.Light),
    displayMedium = style(40, 48, FontWeight.Normal),
    displaySmall = style(34, 40, FontWeight.SemiBold),
    headlineLarge = style(30, 38, FontWeight.Bold),
    headlineMedium = style(26, 34, FontWeight.Bold),
    headlineSmall = style(22, 30, FontWeight.Bold),
    titleLarge = style(20, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(15, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 14, FontWeight.Medium, 0.2),
)

/** App-specific text styles that Material does not define. */
@Immutable
data class FocusExtraTypography(
    val timer: TextStyle,
    val clock: TextStyle,
    val statValue: TextStyle,
    val quote: TextStyle,
)

val DefaultExtraTypography = FocusExtraTypography(
    timer = style(64, 70, FontWeight.Light, -1.0),
    clock = style(18, 22, FontWeight.Medium),
    statValue = style(22, 28, FontWeight.Bold),
    quote = style(14, 20, FontWeight.Normal).copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
)
