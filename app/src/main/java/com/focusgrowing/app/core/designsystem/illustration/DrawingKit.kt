package com.focusgrowing.app.core.designsystem.illustration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/*
 * Small vector drawing helpers shared by the built-in backgrounds, the world island and the
 * onboarding illustrations. Everything is drawn in code, so there are no image assets to ship.
 */

internal fun DrawScope.verticalSky(colors: List<Color>) {
    drawRect(Brush.verticalGradient(colors))
}

/** Ridge through (xFraction, yFraction) points, filled down to [bottomY] (fraction of height). */
internal fun DrawScope.ridge(points: List<Pair<Float, Float>>, color: Color, bottomY: Float = 1f) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h * bottomY)
        points.forEach { (x, y) -> lineTo(x * w, y * h) }
        lineTo(w, h * bottomY)
        close()
    }
    drawPath(path, color)
}

internal fun DrawScope.ridgeGradient(points: List<Pair<Float, Float>>, top: Color, bottom: Color, bottomY: Float = 1f) {
    val w = size.width
    val h = size.height
    val minY = (points.minOfOrNull { it.second } ?: 0f) * h
    val path = Path().apply {
        moveTo(0f, h * bottomY)
        points.forEach { (x, y) -> lineTo(x * w, y * h) }
        lineTo(w, h * bottomY)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(top, bottom), startY = minY, endY = h * bottomY))
}

/** Snow cap: a small triangle on top of a peak. */
internal fun DrawScope.snowCap(peakX: Float, peakY: Float, width: Float, color: Color) {
    val path = Path().apply {
        moveTo(peakX, peakY)
        lineTo(peakX - width / 2f, peakY + width * 0.55f)
        lineTo(peakX - width * 0.15f, peakY + width * 0.42f)
        lineTo(peakX, peakY + width * 0.6f)
        lineTo(peakX + width * 0.18f, peakY + width * 0.4f)
        lineTo(peakX + width / 2f, peakY + width * 0.55f)
        close()
    }
    drawPath(path, color)
}

internal fun DrawScope.pine(x: Float, baseY: Float, height: Float, color: Color, trunk: Color? = null) {
    val width = height * 0.46f
    trunk?.let {
        drawRect(it, topLeft = Offset(x - width * 0.07f, baseY - height * 0.14f), size = Size(width * 0.14f, height * 0.14f))
    }
    val tiers = 3
    for (i in 0 until tiers) {
        val tierTop = baseY - height + i * height * 0.24f
        val tierBottom = baseY - height * 0.12f - (tiers - 1 - i) * height * 0.18f
        val tierHalf = width / 2f * (0.55f + i * 0.22f)
        val path = Path().apply {
            moveTo(x, tierTop)
            lineTo(x - tierHalf, tierBottom)
            lineTo(x + tierHalf, tierBottom)
            close()
        }
        drawPath(path, color)
    }
}

internal fun DrawScope.roundTree(x: Float, baseY: Float, height: Float, foliage: Color, dark: Color, trunk: Color) {
    val r = height * 0.32f
    drawRect(trunk, topLeft = Offset(x - height * 0.05f, baseY - height * 0.4f), size = Size(height * 0.1f, height * 0.4f))
    drawCircle(dark, radius = r, center = Offset(x + r * 0.25f, baseY - height * 0.62f))
    drawCircle(foliage, radius = r, center = Offset(x - r * 0.15f, baseY - height * 0.68f))
    drawCircle(foliage, radius = r * 0.75f, center = Offset(x + r * 0.35f, baseY - height * 0.82f))
}

internal fun DrawScope.forestLine(
    baseY: Float,
    count: Int,
    minHeight: Float,
    maxHeight: Float,
    color: Color,
    seed: Int,
    fromX: Float = 0f,
    toX: Float = size.width,
) {
    val random = Random(seed)
    val step = (toX - fromX) / count
    for (i in 0..count) {
        val x = fromX + i * step + random.nextFloat() * step * 0.6f
        val h = minHeight + random.nextFloat() * (maxHeight - minHeight)
        pine(x, baseY + random.nextFloat() * 4f, h, color)
    }
}

internal fun DrawScope.cloud(center: Offset, width: Float, color: Color) {
    val r = width / 4f
    drawCircle(color, r, Offset(center.x - r * 1.2f, center.y + r * 0.2f))
    drawCircle(color, r * 1.35f, Offset(center.x, center.y - r * 0.2f))
    drawCircle(color, r * 1.05f, Offset(center.x + r * 1.3f, center.y + r * 0.15f))
    drawRect(color, topLeft = Offset(center.x - r * 1.2f, center.y), size = Size(r * 2.5f, r * 1.2f))
}

internal fun DrawScope.stars(count: Int, seed: Int, color: Color, maxY: Float = 1f) {
    val random = Random(seed)
    repeat(count) {
        val alpha = 0.35f + random.nextFloat() * 0.65f
        drawCircle(
            color.copy(alpha = alpha),
            radius = 0.6f + random.nextFloat() * 1.6f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * maxY),
        )
    }
}

internal fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
    drawCircle(
        Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/** Horizontal light streaks on water. */
internal fun DrawScope.waterStreaks(topY: Float, bottomY: Float, color: Color, seed: Int) {
    val random = Random(seed)
    repeat(14) {
        val y = topY + random.nextFloat() * (bottomY - topY)
        val len = size.width * (0.05f + random.nextFloat() * 0.18f)
        val x = random.nextFloat() * (size.width - len)
        drawLine(color, Offset(x, y), Offset(x + len, y), strokeWidth = 1.5f + random.nextFloat() * 1.5f)
    }
}

internal fun DrawScope.skyline(
    baseY: Float,
    minHeight: Float,
    maxHeight: Float,
    color: Color,
    windowColor: Color?,
    seed: Int,
) {
    val random = Random(seed)
    var x = -10f
    while (x < size.width) {
        val w = size.width * (0.06f + random.nextFloat() * 0.08f)
        val h = minHeight + random.nextFloat() * (maxHeight - minHeight)
        drawRect(color, topLeft = Offset(x, baseY - h), size = Size(w, h + 2f))
        if (windowColor != null) {
            val cols = (w / 9f).toInt().coerceAtLeast(1)
            val rows = (h / 12f).toInt()
            for (r in 1 until rows) for (c in 0 until cols) {
                if (random.nextFloat() < 0.35f) {
                    drawRect(
                        windowColor.copy(alpha = 0.5f + random.nextFloat() * 0.5f),
                        topLeft = Offset(x + 3f + c * 9f, baseY - h + r * 12f),
                        size = Size(4f, 5f),
                    )
                }
            }
        }
        x += w + random.nextFloat() * 4f
    }
}

internal fun DrawScope.wave(yFraction: Float, amplitude: Float, frequency: Float, phase: Float, color: Color) {
    val w = size.width
    val h = size.height
    val baseY = h * yFraction
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, baseY)
        val steps = 40
        for (i in 0..steps) {
            val x = w * i / steps
            val y = baseY + amplitude * kotlin.math.sin((i.toFloat() / steps) * frequency * 2f * Math.PI.toFloat() + phase)
            lineTo(x, y)
        }
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}
