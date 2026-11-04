package com.kodomo.app.ui.kids.games

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.components.CameraView
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs
import kotlin.random.Random

/**
 * Camera motion detector: tracks the centroid of pixel changes along the display's vertical axis between frames.
 * A fast vertical swing of the centroid combined with high motion energy is counted as a jump.
 */
class JumpDetector {
    private var previous: IntArray? = null
    private var lastCentroid = Float.NaN
    private var lastJumpAt = 0L
    val jumps = MutableStateFlow(0)

    fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val gw = 24
        val gh = 24
        val luma = IntArray(gw * gh)
        for (gy in 0 until gh) {
            val y = gy * image.height / gh
            for (gx in 0 until gw) {
                val x = gx * image.width / gw
                val i = y * rowStride + x * pixelStride
                if (i + 2 < buffer.limit()) {
                    val r = buffer.get(i).toInt() and 0xFF
                    val g = buffer.get(i + 1).toInt() and 0xFF
                    val b = buffer.get(i + 2).toInt() and 0xFF
                    luma[gy * gw + gx] = (r * 299 + g * 587 + b * 114) / 1000
                }
            }
        }
        val prev = previous
        previous = luma
        if (prev == null) return
        // Sensor is landscape; with 90/270° rotation the display-vertical axis is the buffer's x axis.
        val verticalIsX = image.imageInfo.rotationDegrees % 180 != 0
        var energy = 0L
        var weighted = 0.0
        for (gy in 0 until gh) for (gx in 0 until gw) {
            val d = abs(luma[gy * gw + gx] - prev[gy * gw + gx])
            if (d > 28) {
                energy += d
                weighted += d * (if (verticalIsX) gx.toDouble() / gw else gy.toDouble() / gh)
            }
        }
        if (energy < 2_500) {
            lastCentroid = Float.NaN
            return
        }
        val centroid = (weighted / energy).toFloat()
        val now = System.currentTimeMillis()
        if (!lastCentroid.isNaN() && abs(centroid - lastCentroid) > 0.12f && energy > 6_000 && now - lastJumpAt > 650) {
            lastJumpAt = now
            jumps.value = jumps.value + 1
        }
        lastCentroid = centroid
    }
}

@Composable
fun JumpPopGame(onFinish: (Int) -> Unit) {
    data class Balloon(val x: Float, val y: Float, val color: Color, val speed: Float)

    val detector = remember { JumpDetector() }
    val balloons = remember { mutableStateListOf<Balloon>() }
    var popped by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(45) }
    val palette = listOf(KodomoColors.Rose, KodomoColors.Sky, KodomoColors.Sun, KodomoColors.Lavender, KodomoColors.GreenLight)

    LaunchedEffect(Unit) {
        detector.jumps.collect { count ->
            if (count > 0 && balloons.isNotEmpty()) {
                val index = balloons.indices.minByOrNull { balloons[it].y } ?: return@collect
                balloons.removeAt(index)
                popped++
            }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        var spawnTimer = 0L
        while (seconds > 0) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50)
            last = now
            spawnTimer += dt
            if (spawnTimer > 1_400 && balloons.size < 6) {
                spawnTimer = 0
                balloons += Balloon(Random.nextFloat() * 0.8f + 0.1f, 1.1f, palette.random(), Random.nextFloat() * 0.00008f + 0.00005f)
            }
            for (i in balloons.indices.reversed()) {
                val b = balloons[i]
                val ny = b.y - b.speed * dt
                if (ny < -0.15f) balloons.removeAt(i) else balloons[i] = b.copy(y = ny)
            }
        }
        onFinish(popped * 5)
    }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1_000)
            seconds--
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("🎈 ترکانده‌شده: ${popped.fa()}   ⏱ ${seconds.fa()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp))
        Text("گوشی را روی میز تکیه بده، عقب برو تا در تصویر دیده شوی و بالا بپر!", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
            CameraView(
                modifier = Modifier.fillMaxSize(),
                lensFacing = CameraSelector.LENS_FACING_FRONT,
                analyzer = detector::analyze,
            )
            Canvas(Modifier.fillMaxSize()) {
                balloons.forEach { b ->
                    val c = Offset(size.width * b.x, size.height * b.y)
                    val r = size.minDimension * 0.08f
                    drawOval(b.color, topLeft = c - Offset(r, r * 1.2f), size = androidx.compose.ui.geometry.Size(r * 2, r * 2.4f))
                    drawCircle(Color.White.copy(alpha = 0.4f), r * 0.25f, c - Offset(r * 0.4f, r * 0.6f))
                    val string = Path().apply {
                        moveTo(c.x, c.y + r * 1.2f)
                        quadraticTo(c.x + r * 0.3f, c.y + r * 1.8f, c.x, c.y + r * 2.4f)
                    }
                    drawPath(string, Color.White, style = Stroke(3f))
                }
            }
        }
    }
}
