package com.focusgrowing.app.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.DayBucket
import com.focusgrowing.app.domain.model.HourSlotShare
import com.focusgrowing.app.presentation.common.UiFormat

/** Simple, calm bar chart (no chart library needed). */
@Composable
fun FocusBarChart(buckets: List<DayBucket>, modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val max = (buckets.maxOfOrNull { it.focusSeconds } ?: 0L).coerceAtLeast(1L)
    val summary = buckets.joinToString { "${it.label}: ${UiFormat.duration(it.focusSeconds)}" }
    val showLabels = buckets.size <= 14
    val labelEvery = if (buckets.size <= 14) 1 else (buckets.size / 6).coerceAtLeast(1)
    Column(modifier.semantics { contentDescription = "Focus time chart. $summary" }) {
        val top = colors.chartPrimary
        val bottom = colors.chartPrimary.copy(alpha = 0.55f)
        val track = colors.progressTrack
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            if (buckets.isEmpty()) return@Canvas
            val slot = size.width / buckets.size
            val barWidth = (slot * 0.55f).coerceAtMost(28.dp.toPx())
            buckets.forEachIndexed { i, bucket ->
                val x = i * slot + (slot - barWidth) / 2
                drawRoundRect(
                    track,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2),
                    alpha = 0.35f,
                )
                val h = size.height * (bucket.focusSeconds.toFloat() / max)
                if (h > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(top, bottom), startY = size.height - h, endY = size.height),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(barWidth / 2),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { i, bucket ->
                Text(
                    if (showLabels || i % labelEvery == 0) bucket.label else "",
                    style = FocusTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun TopHoursList(slots: List<HourSlotShare>) {
    val palette = listOf(FocusTheme.colors.chartSecondary, FocusTheme.colors.chartTertiary, FocusTheme.colors.chartPrimary)
    Column {
        slots.forEachIndexed { index, slot ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    UiFormat.hourSlot(slot.startHour, slot.endHour),
                    style = FocusTheme.typography.labelMedium,
                    color = FocusTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f),
                )
                FocusProgressBar(slot.share, color = palette[index % palette.size], height = 8.dp, modifier = Modifier.weight(0.45f))
                Text(
                    "${(slot.share * 100).toInt()}%",
                    style = FocusTheme.typography.labelMedium,
                    color = FocusTheme.colors.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.13f),
                )
            }
        }
    }
}

