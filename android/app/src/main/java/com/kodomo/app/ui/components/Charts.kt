package com.kodomo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.ui.theme.KodomoColors
import com.kodomo.app.ui.theme.Vazirmatn
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun categoryColor(category: ActivityCategory): Color = when (category) {
    ActivityCategory.VIDEO -> KodomoColors.Sky
    ActivityCategory.BOOK -> KodomoColors.Sun
    ActivityCategory.GAME -> KodomoColors.Rose
    ActivityCategory.MUSIC -> KodomoColors.Lavender
    ActivityCategory.LEARNING -> KodomoColors.Green
    ActivityCategory.SCHOOL -> Color(0xFF4C6A92)
    ActivityCategory.EXPLORE -> KodomoColors.Peach
    ActivityCategory.BROWSER -> Color(0xFF2BAEA0)
    ActivityCategory.PROJECT -> Color(0xFFB57A4A)
    ActivityCategory.HOME -> Color(0xFF9AA9A2)
}

data class StackedBar(val label: String, val segments: List<Pair<Color, Float>>)

/** Animated stacked bar chart (minutes per day split by category). */
@Composable
fun StackedBarChart(bars: List<StackedBar>, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700))
    }
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(height)) {
        val maxTotal = bars.maxOfOrNull { b -> b.segments.sumOf { it.second.toDouble() }.toFloat() }?.coerceAtLeast(1f) ?: 1f
        val labelSpace = 22.dp.toPx()
        val chartH = size.height - labelSpace
        repeat(4) { i ->
            val y = chartH * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        if (bars.isEmpty()) return@Canvas
        val slot = size.width / bars.size
        val barW = slot * 0.5f
        bars.forEachIndexed { index, bar ->
            // RTL: first bar on the right
            val x = size.width - (index + 1) * slot + (slot - barW) / 2f
            var top = chartH
            bar.segments.forEach { (color, value) ->
                val h = chartH * (value / maxTotal) * progress.value
                drawRoundRect(color, topLeft = Offset(x, top - h), size = Size(barW, h), cornerRadius = CornerRadius(6f))
                top -= h
            }
            val layout = measurer.measure(bar.label, TextStyle(fontFamily = Vazirmatn, fontSize = 10.sp, color = labelColor))
            drawText(layout, topLeft = Offset(x + barW / 2 - layout.size.width / 2, chartH + 4.dp.toPx()))
        }
    }
}

/** Donut chart with legend (today's minutes by category). */
@Composable
fun DonutChart(values: List<Triple<String, Color, Float>>, centerLabel: String, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = this.size.minDimension * 0.16f
                val inset = stroke / 2
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                val total = values.sumOf { it.third.toDouble() }.toFloat()
                if (total <= 0f) return@Canvas
                var start = -90f
                values.forEach { (_, color, v) ->
                    val sweep = 360f * (v / total) * progress.value
                    drawArc(color, start, sweep, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += sweep
                }
            }
            Text(centerLabel, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { (label, color, _) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Radar (spider) chart for the Kodomo pedagogical passport. Values are on a 0..5 scale. */
@Composable
fun RadarChart(axes: List<Pair<String, Float>>, modifier: Modifier = Modifier, size: Dp = 240.dp, fill: Color = KodomoColors.Green) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val progress = remember { Animatable(0f) }
    LaunchedEffect(axes) { progress.animateTo(1f, tween(900)) }
    Canvas(modifier.size(size)) {
        if (axes.isEmpty()) return@Canvas
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 2 * 0.68f
        val n = axes.size
        fun point(i: Int, r: Float): Offset {
            val angle = -PI / 2 + 2 * PI * i / n
            return Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
        }
        for (level in 1..5) {
            val path = Path()
            for (i in 0 until n) {
                val p = point(i, radius * level / 5f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, grid, style = Stroke(1.5f))
        }
        for (i in 0 until n) drawLine(grid, center, point(i, radius), strokeWidth = 1.5f)
        val shape = Path()
        axes.forEachIndexed { i, (_, v) ->
            val p = point(i, radius * (v.coerceIn(0f, 5f) / 5f) * progress.value)
            if (i == 0) shape.moveTo(p.x, p.y) else shape.lineTo(p.x, p.y)
        }
        shape.close()
        drawPath(shape, fill.copy(alpha = 0.28f))
        drawPath(shape, fill, style = Stroke(3f))
        axes.forEachIndexed { i, (label, _) ->
            val p = point(i, radius + 24.dp.toPx())
            val layout = measurer.measure(label, TextStyle(fontFamily = Vazirmatn, fontSize = 11.sp, color = labelColor))
            drawText(layout, topLeft = Offset(p.x - layout.size.width / 2, p.y - layout.size.height / 2))
        }
    }
}

@Composable
fun Legend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
