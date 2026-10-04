package com.focusgrowing.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A complete palette = light tokens + dark tokens.
 *
 * HOW TO CHANGE THE APP COLORS
 * 1. Quick re-skin: edit the hex values in [MintSwatch] below.
 * 2. New palette: copy `mintMeadow`, change values (or use `.copy(...)` like [ocean]),
 *    then add it to [FocusPalettes.all]. It automatically shows up in
 *    Settings → Appearance → Color palette.
 * 3. New default: change [FocusPalettes.Default].
 */
@Immutable
data class FocusPalette(
    val id: String,
    val displayName: String,
    val light: FocusColorTokens,
    val dark: FocusColorTokens,
    /** Premium palettes are listed but locked for free users. */
    val isPremium: Boolean = false,
)

/** Raw swatches taken from the design mockups. Only palette files reference these. */
private object MintSwatch {
    val Mint50 = Color(0xFFEFF8F4)
    val Mint100 = Color(0xFFDDF1E9)
    val Mint200 = Color(0xFFBDE5D7)
    val Mint400 = Color(0xFF6CC7AE)
    val Mint500 = Color(0xFF4DB89C)
    val Mint600 = Color(0xFF3FA88D)
    val Mint700 = Color(0xFF2E8B74)
    val Mint900 = Color(0xFF123F35)

    val Ink900 = Color(0xFF1E3446)
    val Ink600 = Color(0xFF5E7280)
    val Ink400 = Color(0xFF93A3AD)

    val Cream = Color(0xFFF7FAF6)
    val White = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5EDE9)
    val Track = Color(0xFFE4EFEA)

    val Gold = Color(0xFFF4B43B)
    val GoldSoft = Color(0xFFFFF1D2)
    val Orange = Color(0xFFF08A3A)
    val OrangeSoft = Color(0xFFFFE7D6)
    val Blue = Color(0xFF4A94DC)
    val BlueSoft = Color(0xFFE1EFFC)
    val Purple = Color(0xFF9A7ED9)
    val PurpleSoft = Color(0xFFEFE8FB)
    val Coral = Color(0xFFF2877E)
    val CoralSoft = Color(0xFFFDE6E3)
    val Red = Color(0xFFD9534F)

    // Dark mode
    val Night900 = Color(0xFF0F1A19)
    val Night800 = Color(0xFF162422)
    val Night700 = Color(0xFF1E302D)
    val Night600 = Color(0xFF2A3F3B)
    val NightText = Color(0xFFE5F0ED)
    val NightTextMuted = Color(0xFF9DB2AD)
}

private val lightIllustration = IllustrationColors(
    skyTop = Color(0xFFBFE3F2),
    skyBottom = Color(0xFFEAF6F4),
    sun = Color(0xFFFFD36B),
    cloud = Color(0xFFFFFFFF),
    mountainFar = Color(0xFFA9C7DA),
    mountainNear = Color(0xFF7FA9BF),
    snow = Color(0xFFF4F8FB),
    water = Color(0xFF7CC6DA),
    waterDeep = Color(0xFF4FA3BE),
    grass = Color(0xFF8CCB6E),
    grassDark = Color(0xFF5FA54E),
    foliage = Color(0xFF4E9A5A),
    foliageDark = Color(0xFF2F7447),
    trunk = Color(0xFF8A5A3C),
    soil = Color(0xFFB9825A),
    soilDark = Color(0xFF8C5B3C),
    roof = Color(0xFFD9674E),
    wall = Color(0xFFF6E7CF),
    window = Color(0xFF7FB7D6),
    flower = Color(0xFFF49AC1),
    flowerAlt = Color(0xFFFFD36B),
    rock = Color(0xFFB8BEC2),
    paper = Color(0xFFFFFFFF),
    paperLine = Color(0xFFB9DCCF),
)

private val darkIllustration = lightIllustration.copy(
    skyTop = Color(0xFF1B3246),
    skyBottom = Color(0xFF2A4A55),
    sun = Color(0xFFF3E3A8),
    cloud = Color(0xFF3A5563),
    mountainFar = Color(0xFF3D5A6C),
    mountainNear = Color(0xFF2C4757),
    snow = Color(0xFFB9C9D3),
    water = Color(0xFF2F6F84),
    waterDeep = Color(0xFF214F60),
    paper = Color(0xFF22332F),
    paperLine = Color(0xFF3F6B5E),
)

private val mintLight = FocusColorTokens(
    primary = MintSwatch.Mint600,
    onPrimary = MintSwatch.White,
    primaryContainer = MintSwatch.Mint100,
    onPrimaryContainer = MintSwatch.Mint900,
    primaryGradientStart = MintSwatch.Mint400,
    primaryGradientEnd = MintSwatch.Mint600,
    secondary = MintSwatch.Gold,
    onSecondary = MintSwatch.Ink900,
    secondaryContainer = MintSwatch.GoldSoft,
    onSecondaryContainer = Color(0xFF5A3F00),
    tertiary = MintSwatch.Purple,
    onTertiary = MintSwatch.White,
    tertiaryContainer = MintSwatch.PurpleSoft,
    onTertiaryContainer = Color(0xFF3B2A66),
    background = MintSwatch.Cream,
    onBackground = MintSwatch.Ink900,
    surface = MintSwatch.White,
    onSurface = MintSwatch.Ink900,
    surfaceVariant = MintSwatch.Mint50,
    onSurfaceVariant = MintSwatch.Ink600,
    surfaceMuted = Color(0xFFEFF4F1),
    cardBorder = MintSwatch.Border,
    outline = MintSwatch.Ink400,
    divider = MintSwatch.Border,
    scrim = Color(0xFF0B1A1F),
    onScrim = MintSwatch.White,
    error = MintSwatch.Red,
    onError = MintSwatch.White,
    success = MintSwatch.Mint500,
    xp = MintSwatch.Gold,
    xpContainer = MintSwatch.GoldSoft,
    streak = MintSwatch.Orange,
    streakContainer = MintSwatch.OrangeSoft,
    info = MintSwatch.Blue,
    infoContainer = MintSwatch.BlueSoft,
    accentPurple = MintSwatch.Purple,
    accentPurpleContainer = MintSwatch.PurpleSoft,
    premium = Color(0xFFF6C24A),
    premiumContainer = MintSwatch.GoldSoft,
    onPremium = Color(0xFF4A3500),
    danger = MintSwatch.Coral,
    dangerContainer = MintSwatch.CoralSoft,
    progressTrack = MintSwatch.Track,
    priorityHigh = Color(0xFFF07A5A),
    priorityMedium = MintSwatch.Gold,
    priorityLow = MintSwatch.Mint400,
    chartPrimary = MintSwatch.Mint500,
    chartSecondary = MintSwatch.Blue,
    chartTertiary = MintSwatch.Purple,
    illustration = lightIllustration,
)

private val mintDark = mintLight.copy(
    primary = Color(0xFF5FD0B3),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF1F4A40),
    onPrimaryContainer = MintSwatch.Mint100,
    primaryGradientStart = Color(0xFF5FD0B3),
    primaryGradientEnd = Color(0xFF3FA88D),
    secondaryContainer = Color(0xFF4A3A12),
    onSecondaryContainer = MintSwatch.GoldSoft,
    tertiaryContainer = Color(0xFF3A2F55),
    onTertiaryContainer = MintSwatch.PurpleSoft,
    background = MintSwatch.Night900,
    onBackground = MintSwatch.NightText,
    surface = MintSwatch.Night800,
    onSurface = MintSwatch.NightText,
    surfaceVariant = MintSwatch.Night700,
    onSurfaceVariant = MintSwatch.NightTextMuted,
    surfaceMuted = MintSwatch.Night700,
    cardBorder = MintSwatch.Night600,
    outline = Color(0xFF6C817C),
    divider = MintSwatch.Night600,
    xpContainer = Color(0xFF4A3A12),
    streakContainer = Color(0xFF4A2C14),
    infoContainer = Color(0xFF173650),
    accentPurpleContainer = Color(0xFF3A2F55),
    premiumContainer = Color(0xFF4A3A12),
    dangerContainer = Color(0xFF4F2622),
    progressTrack = MintSwatch.Night600,
    illustration = darkIllustration,
)

/** Example palette built by overriding only the brand roles. */
private val oceanLight = mintLight.copy(
    primary = Color(0xFF3B82C4),
    primaryContainer = Color(0xFFDCEBFA),
    onPrimaryContainer = Color(0xFF0D3050),
    primaryGradientStart = Color(0xFF62A3DE),
    primaryGradientEnd = Color(0xFF3B82C4),
    success = Color(0xFF3B9E8C),
    chartPrimary = Color(0xFF3B82C4),
    chartSecondary = Color(0xFF4DB89C),
    priorityLow = Color(0xFF62A3DE),
    background = Color(0xFFF6F9FC),
    surfaceVariant = Color(0xFFEEF4FA),
    surfaceMuted = Color(0xFFECF2F8),
    progressTrack = Color(0xFFE2ECF6),
    cardBorder = Color(0xFFE2EAF2),
)
private val oceanDark = mintDark.copy(
    primary = Color(0xFF7DB8EE),
    onPrimary = Color(0xFF002F52),
    primaryContainer = Color(0xFF1C3D5E),
    primaryGradientStart = Color(0xFF7DB8EE),
    primaryGradientEnd = Color(0xFF3B82C4),
    chartPrimary = Color(0xFF7DB8EE),
    background = Color(0xFF0E1620),
    surface = Color(0xFF15202C),
    surfaceVariant = Color(0xFF1D2B3A),
    surfaceMuted = Color(0xFF1D2B3A),
    cardBorder = Color(0xFF283A4D),
)

private val sunsetLight = mintLight.copy(
    primary = Color(0xFFE0714F),
    primaryContainer = Color(0xFFFFE3D8),
    onPrimaryContainer = Color(0xFF5A1F0C),
    primaryGradientStart = Color(0xFFF29A6E),
    primaryGradientEnd = Color(0xFFE0714F),
    chartPrimary = Color(0xFFE0714F),
    priorityLow = Color(0xFFF29A6E),
    background = Color(0xFFFCF8F5),
    surfaceVariant = Color(0xFFFBF0EA),
    surfaceMuted = Color(0xFFF7EEE9),
    progressTrack = Color(0xFFF5E6DE),
    cardBorder = Color(0xFFF1E4DC),
)
private val sunsetDark = mintDark.copy(
    primary = Color(0xFFF4A07F),
    onPrimary = Color(0xFF5A1F0C),
    primaryContainer = Color(0xFF5A2E1F),
    primaryGradientStart = Color(0xFFF4A07F),
    primaryGradientEnd = Color(0xFFE0714F),
    chartPrimary = Color(0xFFF4A07F),
    background = Color(0xFF1A1210),
    surface = Color(0xFF241916),
    surfaceVariant = Color(0xFF33231E),
    surfaceMuted = Color(0xFF33231E),
    cardBorder = Color(0xFF3F2C26),
)

private val lavenderLight = mintLight.copy(
    primary = Color(0xFF7D68C9),
    primaryContainer = Color(0xFFEAE4FA),
    onPrimaryContainer = Color(0xFF2B1D5C),
    primaryGradientStart = Color(0xFFA08CE3),
    primaryGradientEnd = Color(0xFF7D68C9),
    chartPrimary = Color(0xFF7D68C9),
    chartTertiary = Color(0xFF4DB89C),
    priorityLow = Color(0xFFA08CE3),
    background = Color(0xFFF9F8FC),
    surfaceVariant = Color(0xFFF2EFFA),
    surfaceMuted = Color(0xFFEFECF7),
    progressTrack = Color(0xFFE9E4F5),
    cardBorder = Color(0xFFE8E3F2),
)
private val lavenderDark = mintDark.copy(
    primary = Color(0xFFB9A8F2),
    onPrimary = Color(0xFF2B1D5C),
    primaryContainer = Color(0xFF3A2F62),
    primaryGradientStart = Color(0xFFB9A8F2),
    primaryGradientEnd = Color(0xFF7D68C9),
    chartPrimary = Color(0xFFB9A8F2),
    background = Color(0xFF13111B),
    surface = Color(0xFF1C1926),
    surfaceVariant = Color(0xFF282337),
    surfaceMuted = Color(0xFF282337),
    cardBorder = Color(0xFF342E47),
)

object FocusPalettes {
    val MintMeadow = FocusPalette("mint_meadow", "Mint Meadow", mintLight, mintDark)
    val Ocean = FocusPalette("ocean", "Ocean", oceanLight, oceanDark, isPremium = true)
    val Sunset = FocusPalette("sunset", "Sunset", sunsetLight, sunsetDark, isPremium = true)
    val Lavender = FocusPalette("lavender", "Lavender", lavenderLight, lavenderDark, isPremium = true)

    /** The palette used on first launch and whenever a stored id is unknown. */
    val Default: FocusPalette = MintMeadow

    val all: List<FocusPalette> = listOf(MintMeadow, Ocean, Sunset, Lavender)

    fun byId(id: String?): FocusPalette = all.firstOrNull { it.id == id } ?: Default
}
