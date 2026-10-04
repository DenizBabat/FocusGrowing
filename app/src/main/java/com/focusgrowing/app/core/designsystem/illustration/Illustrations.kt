package com.focusgrowing.app.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.designsystem.theme.IllustrationColors
import com.focusgrowing.app.domain.model.WorldItemType
import kotlin.random.Random

/** Theme-aware illustrations: they recolor automatically with the selected palette. */

@Composable
fun LandscapeBackdrop(modifier: Modifier = Modifier, showLake: Boolean = true, showSky: Boolean = true) {
    val c = FocusTheme.colors.illustration
    Canvas(modifier) { drawLandscape(c, showLake, showSky) }
}

internal fun DrawScope.drawLandscape(c: IllustrationColors, showLake: Boolean, showSky: Boolean) {
    val w = size.width
    val h = size.height
    if (showSky) {
        verticalSky(listOf(c.skyTop, c.skyBottom))
        drawCircle(c.sun.copy(alpha = 0.9f), radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.22f))
        cloud(Offset(w * 0.2f, h * 0.2f), w * 0.22f, c.cloud.copy(alpha = 0.8f))
    }
    val far = listOf(0f to 0.62f, 0.15f to 0.45f, 0.3f to 0.55f, 0.48f to 0.32f, 0.62f to 0.5f, 0.8f to 0.38f, 1f to 0.56f)
    ridge(far, c.mountainFar, bottomY = 0.8f)
    snowCap(w * 0.48f, h * 0.32f, w * 0.1f, c.snow)
    snowCap(w * 0.8f, h * 0.38f, w * 0.08f, c.snow)
    ridge(listOf(0f to 0.68f, 0.25f to 0.58f, 0.5f to 0.7f, 0.75f to 0.6f, 1f to 0.7f), c.mountainNear, bottomY = 0.8f)
    forestLine(h * 0.74f, 22, h * 0.05f, h * 0.11f, c.foliageDark, seed = 4)
    if (showLake) {
        drawRect(
            Brush.verticalGradient(listOf(c.water, c.waterDeep), startY = h * 0.74f, endY = h),
            topLeft = Offset(0f, h * 0.74f),
            size = Size(w, h * 0.26f),
        )
        waterStreaks(h * 0.77f, h * 0.97f, c.cloud.copy(alpha = 0.35f), seed = 2)
    } else {
        drawRect(c.grass, topLeft = Offset(0f, h * 0.74f), size = Size(w, h * 0.26f))
    }
    pine(w * 0.06f, h * 1.02f, h * 0.3f, c.foliageDark)
    pine(w * 0.14f, h * 1.02f, h * 0.2f, c.foliage)
    pine(w * 0.94f, h * 1.02f, h * 0.32f, c.foliageDark)
}

/** Only the tree line + hills, used at the bottom of cards and celebration screens. */
@Composable
fun ForestFooter(modifier: Modifier = Modifier) {
    val c = FocusTheme.colors.illustration
    Canvas(modifier) {
        val h = size.height
        ridge(listOf(0f to 0.55f, 0.3f to 0.35f, 0.55f to 0.5f, 0.8f to 0.3f, 1f to 0.45f), c.mountainFar.copy(alpha = 0.6f))
        forestLine(h * 0.9f, 20, h * 0.25f, h * 0.55f, c.foliage.copy(alpha = 0.85f), seed = 8)
        forestLine(h * 1.02f, 14, h * 0.3f, h * 0.7f, c.foliageDark, seed = 12)
    }
}

// ---------------------------------------------------------------------------------------------
// World island
// ---------------------------------------------------------------------------------------------

@Composable
fun WorldIsland(items: Set<WorldItemType>, modifier: Modifier = Modifier) {
    val c = FocusTheme.colors.illustration
    Canvas(modifier) { drawIsland(c, items) }
}

private fun DrawScope.drawIsland(c: IllustrationColors, items: Set<WorldItemType>) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val topY = h * 0.56f
    val rx = w * 0.44f
    val ry = h * 0.15f

    // Cliff
    val cliff = Path().apply {
        moveTo(cx - rx, topY)
        lineTo(cx - rx * 0.8f, topY + ry * 1.6f)
        lineTo(cx - rx * 0.5f, topY + ry * 2.1f)
        lineTo(cx - rx * 0.2f, topY + ry * 2.6f)
        lineTo(cx, h * 0.97f)
        lineTo(cx + rx * 0.25f, topY + ry * 2.5f)
        lineTo(cx + rx * 0.55f, topY + ry * 2.0f)
        lineTo(cx + rx * 0.85f, topY + ry * 1.4f)
        lineTo(cx + rx, topY)
        close()
    }
    drawPath(cliff, Brush.verticalGradient(listOf(c.soil, c.soilDark), startY = topY, endY = h))
    // Rock strata
    val random = Random(5)
    repeat(7) {
        val y = topY + ry * (0.9f + random.nextFloat() * 1.3f)
        val x = cx - rx * 0.6f + random.nextFloat() * rx * 1.2f
        drawLine(c.soilDark, Offset(x, y), Offset(x + rx * 0.18f, y + 2f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
    // Grass top
    drawOval(c.grassDark, topLeft = Offset(cx - rx, topY - ry + ry * 0.25f), size = Size(rx * 2, ry * 2))
    drawOval(
        Brush.verticalGradient(listOf(c.grass, c.grassDark), startY = topY - ry, endY = topY + ry),
        topLeft = Offset(cx - rx, topY - ry),
        size = Size(rx * 2, ry * 2),
    )

    fun at(fx: Float, fy: Float) = Offset(cx + (fx - 0.5f) * rx * 2f, topY + (fy - 0.5f) * ry * 2f)
    val unit = h * 0.12f

    if (WorldItemType.LAKE in items) {
        val p = at(0.7f, 0.62f)
        drawOval(c.waterDeep, topLeft = Offset(p.x - unit * 0.95f, p.y - unit * 0.28f), size = Size(unit * 1.9f, unit * 0.62f))
        drawOval(c.water, topLeft = Offset(p.x - unit * 0.85f, p.y - unit * 0.24f), size = Size(unit * 1.7f, unit * 0.5f))
        if (WorldItemType.WATERFALL in items) {
            val fall = Path().apply {
                moveTo(p.x + unit * 0.25f, p.y + unit * 0.2f)
                lineTo(p.x + unit * 0.55f, p.y + unit * 0.15f)
                lineTo(p.x + unit * 0.5f, h * 0.9f)
                lineTo(p.x + unit * 0.3f, h * 0.9f)
                close()
            }
            drawPath(fall, Brush.verticalGradient(listOf(c.water, c.cloud.copy(alpha = 0.7f))))
        }
        if (WorldItemType.BRIDGE in items) {
            drawArc(c.trunk, 200f, 140f, false, topLeft = Offset(p.x - unit * 0.5f, p.y - unit * 0.3f), size = Size(unit, unit * 0.6f), style = Stroke(width = unit * 0.08f))
        }
    }
    if (WorldItemType.GRASS in items) {
        listOf(0.2f to 0.62f, 0.45f to 0.8f, 0.8f to 0.4f, 0.12f to 0.45f, 0.58f to 0.3f).forEach { (fx, fy) ->
            val p = at(fx, fy)
            for (k in -1..1) drawLine(c.grassDark, p, Offset(p.x + k * unit * 0.08f, p.y - unit * 0.18f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
    if (WorldItemType.GARDEN in items) {
        val p = at(0.28f, 0.66f)
        for (row in 0 until 3) {
            drawRoundRect(c.soilDark, topLeft = Offset(p.x - unit * 0.5f, p.y + row * unit * 0.16f), size = Size(unit, unit * 0.08f), cornerRadius = CornerRadius(4f))
            for (k in 0 until 4) drawCircle(c.foliage, radius = unit * 0.06f, center = Offset(p.x - unit * 0.38f + k * unit * 0.25f, p.y + row * unit * 0.16f))
        }
    }
    if (WorldItemType.ROCKS in items) {
        val p = at(0.86f, 0.62f)
        drawOval(c.rock, topLeft = Offset(p.x - unit * 0.2f, p.y - unit * 0.12f), size = Size(unit * 0.4f, unit * 0.24f))
        drawOval(c.rock.copy(alpha = 0.8f), topLeft = Offset(p.x + unit * 0.1f, p.y - unit * 0.05f), size = Size(unit * 0.25f, unit * 0.16f))
    }

    // Back row (drawn first so front objects overlap)
    if (WorldItemType.PINE in items) {
        listOf(0.16f to 0.42f, 0.25f to 0.3f, 0.78f to 0.28f, 0.88f to 0.4f).forEachIndexed { i, (fx, fy) ->
            val p = at(fx, fy)
            pine(p.x, p.y, unit * (1.1f + (i % 2) * 0.35f), if (i % 2 == 0) c.foliageDark else c.foliage, c.trunk)
        }
    }
    if (WorldItemType.LIGHTHOUSE in items) {
        val p = at(0.95f, 0.45f)
        drawRect(c.wall, topLeft = Offset(p.x - unit * 0.1f, p.y - unit * 1.1f), size = Size(unit * 0.2f, unit * 1.1f))
        drawRect(c.roof, topLeft = Offset(p.x - unit * 0.1f, p.y - unit * 0.75f), size = Size(unit * 0.2f, unit * 0.15f))
        drawCircle(c.sun, radius = unit * 0.1f, center = Offset(p.x, p.y - unit * 1.18f))
    }
    if (WorldItemType.WINDMILL in items) {
        val p = at(0.7f, 0.25f)
        val body = Path().apply {
            moveTo(p.x - unit * 0.18f, p.y)
            lineTo(p.x - unit * 0.1f, p.y - unit * 0.9f)
            lineTo(p.x + unit * 0.1f, p.y - unit * 0.9f)
            lineTo(p.x + unit * 0.18f, p.y)
            close()
        }
        drawPath(body, c.wall)
        val hub = Offset(p.x, p.y - unit * 0.9f)
        for (angle in listOf(20f, 110f, 200f, 290f)) {
            rotate(angle, pivot = hub) {
                drawRoundRect(c.trunk, topLeft = Offset(hub.x - unit * 0.05f, hub.y - unit * 0.6f), size = Size(unit * 0.1f, unit * 0.6f), cornerRadius = CornerRadius(3f))
            }
        }
        drawCircle(c.roof, radius = unit * 0.06f, center = hub)
    }
    if (WorldItemType.CABIN in items) house(at(0.3f, 0.34f), unit * 0.75f, c)
    if (WorldItemType.HOUSE in items) {
        house(at(0.5f, 0.44f), unit * 1.05f, c)
    } else if (WorldItemType.SPROUT in items) {
        val p = at(0.5f, 0.5f)
        drawLine(c.grassDark, p, Offset(p.x, p.y - unit * 0.4f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawOval(c.foliage, topLeft = Offset(p.x - unit * 0.32f, p.y - unit * 0.55f), size = Size(unit * 0.32f, unit * 0.18f))
        drawOval(c.grass, topLeft = Offset(p.x, p.y - unit * 0.62f), size = Size(unit * 0.34f, unit * 0.2f))
    }
    if (WorldItemType.FENCE in items) {
        val start = at(0.36f, 0.64f)
        val end = at(0.62f, 0.66f)
        drawLine(c.trunk, start, end, strokeWidth = 4f)
        drawLine(c.trunk, Offset(start.x, start.y - unit * 0.14f), Offset(end.x, end.y - unit * 0.14f), strokeWidth = 4f)
        val posts = 6
        for (i in 0..posts) {
            val x = start.x + (end.x - start.x) * i / posts
            val y = start.y + (end.y - start.y) * i / posts
            drawLine(c.trunk, Offset(x, y + unit * 0.05f), Offset(x, y - unit * 0.24f), strokeWidth = 5f, cap = StrokeCap.Round)
        }
    }
    if (WorldItemType.BUSH in items) {
        listOf(0.14f to 0.58f, 0.66f to 0.82f, 0.42f to 0.78f).forEach { (fx, fy) ->
            val p = at(fx, fy)
            drawCircle(c.foliageDark, unit * 0.2f, Offset(p.x + unit * 0.08f, p.y))
            drawCircle(c.foliage, unit * 0.2f, Offset(p.x - unit * 0.08f, p.y - unit * 0.04f))
        }
    }
    if (WorldItemType.FLOWERS in items) {
        val random2 = Random(11)
        repeat(12) {
            val p = at(0.15f + random2.nextFloat() * 0.7f, 0.55f + random2.nextFloat() * 0.35f)
            drawCircle(if (it % 2 == 0) c.flower else c.flowerAlt, radius = unit * 0.05f, center = p)
        }
    }
    if (WorldItemType.OAK in items) {
        roundTree(at(0.1f, 0.5f).x, at(0.1f, 0.5f).y, unit * 1.2f, c.foliage, c.foliageDark, c.trunk)
        roundTree(at(0.92f, 0.58f).x, at(0.92f, 0.58f).y, unit * 1.0f, c.foliage, c.foliageDark, c.trunk)
    }
}

private fun DrawScope.house(base: Offset, s: Float, c: IllustrationColors) {
    val wallW = s * 1.0f
    val wallH = s * 0.62f
    drawRect(c.wall, topLeft = Offset(base.x - wallW / 2, base.y - wallH), size = Size(wallW, wallH))
    val roof = Path().apply {
        moveTo(base.x - wallW * 0.62f, base.y - wallH)
        lineTo(base.x, base.y - wallH - s * 0.5f)
        lineTo(base.x + wallW * 0.62f, base.y - wallH)
        close()
    }
    drawPath(roof, c.roof)
    drawRect(c.trunk, topLeft = Offset(base.x - s * 0.09f, base.y - s * 0.32f), size = Size(s * 0.18f, s * 0.32f))
    drawRect(c.window, topLeft = Offset(base.x - wallW * 0.38f, base.y - wallH * 0.75f), size = Size(s * 0.17f, s * 0.15f))
    drawRect(c.window, topLeft = Offset(base.x + wallW * 0.22f, base.y - wallH * 0.75f), size = Size(s * 0.17f, s * 0.15f))
    drawRect(c.soilDark, topLeft = Offset(base.x + wallW * 0.2f, base.y - wallH - s * 0.42f), size = Size(s * 0.1f, s * 0.2f))
}

// ---------------------------------------------------------------------------------------------
// Badges & decorations
// ---------------------------------------------------------------------------------------------

/** Glowing medal used on celebration screens (star, flame, ...). */
@Composable
fun MedalBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    iconTint: Color = Color.White,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension / 2
            glow(center, r, color.copy(alpha = 0.35f))
            for (i in 0 until 12) {
                rotate(i * 30f, pivot = center) {
                    drawLine(color.copy(alpha = 0.45f), Offset(center.x, center.y - r * 0.72f), Offset(center.x, center.y - r * 0.9f), strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            drawCircle(color.copy(alpha = 0.35f), radius = r * 0.62f, center = center)
            drawCircle(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color)), radius = r * 0.5f, center = center)
        }
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.34f))
    }
}

/** Two leafy sprigs framing content (used behind medals). */
@Composable
fun LeafFrame(modifier: Modifier = Modifier) {
    val c = FocusTheme.colors.illustration
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun leaf(center: Offset, angle: Float, len: Float, color: Color) {
            rotate(angle, pivot = center) {
                drawOval(color, topLeft = Offset(center.x - len * 0.18f, center.y - len / 2), size = Size(len * 0.36f, len))
            }
        }
        for (side in listOf(-1f, 1f)) {
            val baseX = w / 2 + side * w * 0.3f
            for (i in 0 until 5) {
                val y = h * 0.85f - i * h * 0.13f
                val x = baseX + side * i * w * 0.025f
                leaf(Offset(x + side * w * 0.05f, y), side * (50f + i * 5f), h * 0.16f, if (i % 2 == 0) c.foliage else c.grassDark)
            }
        }
    }
}

@Composable
fun Confetti(modifier: Modifier = Modifier, seed: Int = 1) {
    val colors = FocusTheme.colors
    val palette = listOf(colors.xp, colors.primary, colors.accentPurple, colors.info, colors.streak, colors.illustration.flower)
    Canvas(modifier) {
        val random = Random(seed)
        repeat(26) { i ->
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height * 0.6f
            val color = palette[i % palette.size].copy(alpha = 0.8f)
            if (i % 3 == 0) {
                drawCircle(color, radius = 3f + random.nextFloat() * 3f, center = Offset(x, y))
            } else {
                rotate(random.nextFloat() * 180f, pivot = Offset(x, y)) {
                    drawRoundRect(color, topLeft = Offset(x, y), size = Size(12f, 5f), cornerRadius = CornerRadius(2f))
                }
            }
        }
    }
}

/** Onboarding "Mission" art: a checklist card with a plus badge. */
@Composable
fun ChecklistIllustration(modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val c = colors.illustration
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        LeafSprigs(c, w, h)
        rotate(-8f, pivot = Offset(w / 2, h / 2)) {
            val cardW = w * 0.5f
            val cardH = h * 0.6f
            val left = w / 2 - cardW / 2
            val top = h / 2 - cardH / 2
            drawRoundRect(colors.primaryContainer, topLeft = Offset(left + 10f, top + 12f), size = Size(cardW, cardH), cornerRadius = CornerRadius(28f))
            drawRoundRect(c.paper, topLeft = Offset(left, top), size = Size(cardW, cardH), cornerRadius = CornerRadius(28f))
            for (i in 0 until 3) {
                val y = top + cardH * (0.25f + i * 0.25f)
                drawRoundRect(colors.primary, topLeft = Offset(left + cardW * 0.12f, y - cardW * 0.07f), size = Size(cardW * 0.14f, cardW * 0.14f), cornerRadius = CornerRadius(8f))
                drawLine(Color.White, Offset(left + cardW * 0.15f, y), Offset(left + cardW * 0.19f, y + cardW * 0.03f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(left + cardW * 0.19f, y + cardW * 0.03f), Offset(left + cardW * 0.24f, y - cardW * 0.035f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawRoundRect(c.paperLine, topLeft = Offset(left + cardW * 0.34f, y - 5f), size = Size(cardW * (0.5f - i * 0.06f), 10f), cornerRadius = CornerRadius(5f))
            }
        }
        val badge = Offset(w * 0.7f, h * 0.7f)
        drawCircle(colors.xp.copy(alpha = 0.3f), radius = w * 0.1f, center = badge)
        drawCircle(colors.xp, radius = w * 0.08f, center = badge)
        drawLine(Color.White, Offset(badge.x - w * 0.035f, badge.y), Offset(badge.x + w * 0.035f, badge.y), strokeWidth = 6f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(badge.x, badge.y - w * 0.035f), Offset(badge.x, badge.y + w * 0.035f), strokeWidth = 6f, cap = StrokeCap.Round)
    }
}

/** Onboarding "Intelligence" art: a small chart card with an insight badge. */
@Composable
fun InsightIllustration(modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val c = colors.illustration
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        LeafSprigs(c, w, h)
        rotate(-6f, pivot = Offset(w / 2, h / 2)) {
            val cardW = w * 0.56f
            val cardH = h * 0.5f
            val left = w / 2 - cardW / 2
            val top = h / 2 - cardH / 2
            drawRoundRect(colors.primaryContainer, topLeft = Offset(left + 10f, top + 12f), size = Size(cardW, cardH), cornerRadius = CornerRadius(26f))
            drawRoundRect(c.paper, topLeft = Offset(left, top), size = Size(cardW, cardH), cornerRadius = CornerRadius(26f))
            val bars = listOf(0.35f, 0.55f, 0.45f, 0.8f)
            bars.forEachIndexed { i, v ->
                val barW = cardW * 0.08f
                val x = left + cardW * 0.12f + i * barW * 1.6f
                val barH = cardH * 0.55f * v
                drawRoundRect(colors.primary, topLeft = Offset(x, top + cardH * 0.8f - barH), size = Size(barW, barH), cornerRadius = CornerRadius(6f))
            }
            for (i in 0 until 3) {
                drawRoundRect(c.paperLine, topLeft = Offset(left + cardW * 0.62f, top + cardH * (0.35f + i * 0.17f)), size = Size(cardW * 0.26f, 9f), cornerRadius = CornerRadius(5f))
            }
        }
        val badge = Offset(w * 0.72f, h * 0.28f)
        drawCircle(colors.info.copy(alpha = 0.25f), radius = w * 0.1f, center = badge)
        drawCircle(colors.info, radius = w * 0.075f, center = badge)
        drawCircle(Color.White, radius = w * 0.03f, center = badge, style = Stroke(width = 5f))
    }
}

@Suppress("FunctionName")
private fun DrawScope.LeafSprigs(c: IllustrationColors, w: Float, h: Float) {
    fun leaf(center: Offset, angle: Float, len: Float, color: Color) {
        rotate(angle, pivot = center) {
            drawOval(color, topLeft = Offset(center.x - len * 0.2f, center.y - len / 2), size = Size(len * 0.4f, len))
        }
    }
    leaf(Offset(w * 0.18f, h * 0.55f), -35f, h * 0.22f, c.foliage)
    leaf(Offset(w * 0.12f, h * 0.7f), -60f, h * 0.18f, c.grassDark)
    leaf(Offset(w * 0.84f, h * 0.35f), 30f, h * 0.2f, c.foliage)
    leaf(Offset(w * 0.9f, h * 0.5f), 60f, h * 0.16f, c.grassDark)
}
