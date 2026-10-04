package com.focusgrowing.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.focusgrowing.app.domain.model.BackgroundScene

/**
 * Built-in Focus backgrounds. They are artwork (content), so their colors intentionally live
 * here instead of in the theme — a sunset should stay orange whatever palette the user picks.
 * Drawn as vectors: sharp on every screen size, fills any aspect ratio, 0 KB of image assets.
 */
@Composable
fun SceneBackground(scene: BackgroundScene, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawScene(scene) }
}

fun DrawScope.drawScene(scene: BackgroundScene) {
    when (scene) {
        BackgroundScene.MOUNTAIN_LAKE -> mountainLake()
        BackgroundScene.FOREST_MORNING -> forestMorning()
        BackgroundScene.SUNSET_HILLS -> sunsetHills()
        BackgroundScene.CITY_DUSK -> city(night = false)
        BackgroundScene.CITY_NIGHT -> city(night = true)
        BackgroundScene.ABSTRACT_BLOBS -> abstractBlobs()
        BackgroundScene.AURORA -> aurora()
        BackgroundScene.NIGHT_SKY -> nightSky()
        BackgroundScene.MINIMAL_WAVES -> minimalWaves()
        BackgroundScene.DEEP_DARK -> deepDark()
    }
}

private fun DrawScope.mountainLake() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFF8EC9EC), Color(0xFFC9E7F3), Color(0xFFEFF7F2)))
    glow(Offset(w * 0.72f, h * 0.2f), w * 0.35f, Color(0x66FFF3C4))
    drawCircle(Color(0xFFFFF2C2), radius = w * 0.07f, center = Offset(w * 0.72f, h * 0.2f))
    cloud(Offset(w * 0.22f, h * 0.14f), w * 0.3f, Color(0xCCFFFFFF))
    cloud(Offset(w * 0.8f, h * 0.33f), w * 0.22f, Color(0x99FFFFFF))

    val far = listOf(0f to 0.5f, 0.12f to 0.4f, 0.24f to 0.47f, 0.4f to 0.3f, 0.52f to 0.42f, 0.66f to 0.34f, 0.8f to 0.45f, 1f to 0.38f)
    ridgeGradient(far, Color(0xFF9DBCD3), Color(0xFFBFD6E3), bottomY = 0.62f)
    snowCap(w * 0.4f, h * 0.3f, w * 0.12f, Color(0xFFF4F8FB))
    snowCap(w * 0.66f, h * 0.34f, w * 0.09f, Color(0xFFF4F8FB))
    val near = listOf(0f to 0.52f, 0.18f to 0.46f, 0.34f to 0.56f, 0.55f to 0.5f, 0.75f to 0.58f, 0.9f to 0.5f, 1f to 0.54f)
    ridgeGradient(near, Color(0xFF6F9AB3), Color(0xFF8DB3C4), bottomY = 0.62f)
    forestLine(h * 0.625f, 26, h * 0.035f, h * 0.075f, Color(0xFF3F7C5A), seed = 3)

    // Lake
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF86C9DC), Color(0xFF4F9FBC)), startY = h * 0.62f, endY = h * 0.86f),
        topLeft = Offset(0f, h * 0.62f),
        size = Size(w, h * 0.26f),
    )
    waterStreaks(h * 0.64f, h * 0.84f, Color(0x55FFFFFF), seed = 9)

    // Foreground shore
    val shore = listOf(0f to 0.8f, 0.2f to 0.84f, 0.45f to 0.88f, 0.7f to 0.86f, 1f to 0.8f)
    ridgeGradient(shore, Color(0xFF7DBA62), Color(0xFF4F8E46))
    pine(w * 0.08f, h * 0.9f, h * 0.2f, Color(0xFF2E6B45))
    pine(w * 0.17f, h * 0.92f, h * 0.14f, Color(0xFF3A7A4F))
    pine(w * 0.9f, h * 0.9f, h * 0.22f, Color(0xFF2E6B45))
    pine(w * 0.8f, h * 0.93f, h * 0.13f, Color(0xFF3A7A4F))
}

private fun DrawScope.forestMorning() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFFF6E9B8), Color(0xFFE3F0D0), Color(0xFFCFE5C9)))
    glow(Offset(w * 0.3f, h * 0.28f), w * 0.5f, Color(0x80FFF6D0))
    drawCircle(Color(0xFFFFF4C9), radius = w * 0.09f, center = Offset(w * 0.3f, h * 0.28f))
    val layers = listOf(
        Triple(0.46f, Color(0xFFA9CFA4), 11),
        Triple(0.58f, Color(0xFF7DB582), 21),
        Triple(0.72f, Color(0xFF4F9463), 31),
        Triple(0.88f, Color(0xFF2F6E4A), 41),
    )
    layers.forEachIndexed { index, (y, color, seed) ->
        val scale = 0.05f + index * 0.035f
        forestLine(h * y, 18 - index * 3, h * scale, h * scale * 1.8f, color, seed)
        drawRect(color, topLeft = Offset(0f, h * y), size = Size(w, h * (1f - y)))
    }
}

private fun DrawScope.sunsetHills() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFF6B4C9A), Color(0xFFE0708A), Color(0xFFF7A767), Color(0xFFFCD38D)))
    glow(Offset(w * 0.5f, h * 0.55f), w * 0.6f, Color(0x88FFD89A))
    drawCircle(Color(0xFFFFE3A8), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.56f))
    ridge(listOf(0f to 0.58f, 0.2f to 0.52f, 0.45f to 0.6f, 0.7f to 0.5f, 1f to 0.57f), Color(0xFFB0587A))
    ridge(listOf(0f to 0.68f, 0.3f to 0.62f, 0.6f to 0.7f, 0.85f to 0.63f, 1f to 0.66f), Color(0xFF7E3F6E))
    ridge(listOf(0f to 0.8f, 0.25f to 0.74f, 0.55f to 0.82f, 1f to 0.76f), Color(0xFF4E2A55))
    forestLine(h * 0.8f, 14, h * 0.04f, h * 0.09f, Color(0xFF3A1F42), seed = 5)
    drawRect(Color(0xFF3A1F42), topLeft = Offset(0f, h * 0.8f), size = Size(w, h * 0.2f))
}

private fun DrawScope.city(night: Boolean) {
    val w = size.width
    val h = size.height
    if (night) {
        verticalSky(listOf(Color(0xFF0B1330), Color(0xFF1B2A55), Color(0xFF2E3D6B)))
        stars(90, seed = 7, color = Color.White, maxY = 0.55f)
        drawCircle(Color(0xFFF5EFD6), radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.16f))
        drawCircle(Color(0xFF1B2A55), radius = w * 0.05f, center = Offset(w * 0.8f, h * 0.15f))
    } else {
        verticalSky(listOf(Color(0xFF3F3A78), Color(0xFFB7678F), Color(0xFFF2A26B)))
        glow(Offset(w * 0.5f, h * 0.62f), w * 0.55f, Color(0x77FFC98A))
    }
    val farColor = if (night) Color(0xFF26335E) else Color(0xFF6D4A7E)
    val nearColor = if (night) Color(0xFF121A36) else Color(0xFF3A2650)
    val windows = Color(0xFFFFD98A)
    skyline(h * 0.78f, h * 0.18f, h * 0.38f, farColor, if (night) windows.copy(alpha = 0.6f) else null, seed = 13)
    skyline(h * 0.86f, h * 0.12f, h * 0.3f, nearColor, windows, seed = 17)
    drawRect(nearColor, topLeft = Offset(0f, h * 0.86f), size = Size(w, h * 0.14f))
}

private fun DrawScope.abstractBlobs() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFFF5EFE8), Color(0xFFEDE6F3)))
    glow(Offset(w * 0.2f, h * 0.25f), w * 0.6f, Color(0xAAF6B8C5))
    glow(Offset(w * 0.85f, h * 0.4f), w * 0.55f, Color(0xAAB8D8F6))
    glow(Offset(w * 0.4f, h * 0.75f), w * 0.65f, Color(0xAABDE8D2))
    glow(Offset(w * 0.9f, h * 0.9f), w * 0.4f, Color(0x99FFE0A8))
    drawCircle(Color(0x33FFFFFF), radius = w * 0.3f, center = Offset(w * 0.6f, h * 0.2f), style = Stroke(width = 2f))
    drawCircle(Color(0x33FFFFFF), radius = w * 0.45f, center = Offset(w * 0.3f, h * 0.7f), style = Stroke(width = 2f))
}

private fun DrawScope.aurora() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFF051824), Color(0xFF0B2B3A), Color(0xFF123D45)))
    stars(120, seed = 21, color = Color.White, maxY = 0.8f)
    val bands = listOf(Color(0x5543F2B0) to 0.3f, Color(0x4460C8F0) to 0.38f, Color(0x44A77BF0) to 0.46f)
    bands.forEachIndexed { i, (color, y) ->
        for (k in 0 until 6) {
            wave(y + k * 0.012f, h * 0.04f, 1.3f + i * 0.3f, i * 1.7f + k * 0.2f, color.copy(alpha = color.alpha * (1f - k * 0.15f)))
        }
    }
    drawRect(
        Brush.verticalGradient(listOf(Color(0x00051824), Color(0xFF051824)), startY = h * 0.55f, endY = h),
        topLeft = Offset(0f, h * 0.55f),
        size = Size(w, h * 0.45f),
    )
    forestLine(h * 0.97f, 20, h * 0.05f, h * 0.11f, Color(0xFF02101A), seed = 23)
}

private fun DrawScope.nightSky() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFF070B1F), Color(0xFF151A3D), Color(0xFF241C4A)))
    glow(Offset(w * 0.3f, h * 0.45f), w * 0.7f, Color(0x444F3C9C))
    glow(Offset(w * 0.8f, h * 0.7f), w * 0.5f, Color(0x33C0508F))
    stars(220, seed = 31, color = Color.White)
    val planet = Offset(w * 0.68f, h * 0.3f)
    drawCircle(Brush.linearGradient(listOf(Color(0xFFF2B880), Color(0xFFB0567E)), start = planet - Offset(60f, 60f), end = planet + Offset(60f, 60f)), radius = w * 0.12f, center = planet)
    drawOval(Color(0x88F7D9B0), topLeft = Offset(planet.x - w * 0.22f, planet.y - w * 0.035f), size = Size(w * 0.44f, w * 0.07f), style = Stroke(width = 4f))
}

private fun DrawScope.minimalWaves() {
    verticalSky(listOf(Color(0xFFF4F1EA), Color(0xFFE6F1EC)))
    wave(0.55f, size.height * 0.025f, 1.2f, 0.3f, Color(0x33A7CFC0))
    wave(0.65f, size.height * 0.03f, 1.0f, 1.4f, Color(0x4497C3B3))
    wave(0.75f, size.height * 0.025f, 1.4f, 2.2f, Color(0x5585B6A4))
    wave(0.86f, size.height * 0.02f, 1.1f, 0.8f, Color(0x6670A791))
}

private fun DrawScope.deepDark() {
    val w = size.width
    val h = size.height
    verticalSky(listOf(Color(0xFF0C0F12), Color(0xFF12181C), Color(0xFF0A0D10)))
    glow(Offset(w * 0.5f, h * 0.35f), w * 0.8f, Color(0x2230A58A))
    glow(Offset(w * 0.2f, h * 0.9f), w * 0.5f, Color(0x1A5A6CFF))
}
