package com.kodomo.app.ui.fx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.kodomo.app.domain.model.AgeGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Short synthesized UI sounds (no audio assets): a soft «pop» for taps and a rising jingle for successes. */
object Sfx {
    private const val RATE = 22_050

    private fun tone(freqs: List<Pair<Float, Int>>, volume: Float): ShortArray {
        val total = freqs.sumOf { it.second } * RATE / 1000
        val out = ShortArray(total)
        var i = 0
        for ((f, ms) in freqs) {
            val n = ms * RATE / 1000
            for (k in 0 until n) {
                val t = k.toFloat() / RATE
                val env = (1f - k.toFloat() / n).let { it * it }
                out[i++] = (sin(2 * PI * f * t).toFloat() * env * volume * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private val popData by lazy { tone(listOf(880f to 45, 1320f to 35), 0.35f) }
    private val successData by lazy { tone(listOf(660f to 90, 880f to 90, 1175f to 160), 0.4f) }

    private suspend fun play(data: ShortArray) = withContext(Dispatchers.Default) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data, 0, data.size)
            track.play()
            kotlinx.coroutines.delay(data.size * 1000L / RATE + 50)
            track.release()
        }
    }

    suspend fun pop() = play(popData)
    suspend fun success() = play(successData)
}

private data class Particle(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

private val confettiColors = listOf(
    Color(0xFFF28AA6), Color(0xFFFFC83D), Color(0xFF5FB98A), Color(0xFF5AA9E6), Color(0xFF9B7FE6), Color(0xFFF59E6C),
)

/** A one-shot confetti burst from the top-centre; re-triggers whenever [key] changes. */
@Composable
fun ConfettiBurst(key: Any, modifier: Modifier = Modifier.fillMaxSize(), count: Int = 90) {
    val particles = remember(key) {
        List(count) { Particle(Random.nextFloat() * PI.toFloat(), Random.nextFloat() * 0.9f + 0.4f, confettiColors.random(), Random.nextFloat() * 10f + 8f, Random.nextFloat() * 720f - 360f) }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(2_400, easing = LinearEasing)) }
    if (progress.value >= 1f) return
    Canvas(modifier) {
        val t = progress.value
        val origin = Offset(size.width / 2, size.height * 0.15f)
        particles.forEach { p ->
            val dist = p.speed * size.minDimension * 0.7f * t
            val x = origin.x + cos(p.angle) * dist * 1.3f
            val y = origin.y - sin(p.angle) * dist * 0.6f + size.height * 0.9f * t * t
            rotate(p.spin * t, pivot = Offset(x, y)) {
                drawRoundRect(
                    p.color.copy(alpha = 1f - t * 0.6f),
                    topLeft = Offset(x - p.size / 2, y - p.size / 4),
                    size = Size(p.size, p.size / 2),
                    cornerRadius = CornerRadius(3f),
                )
            }
        }
    }
}

/**
 * Living backdrop of the kids space: a soft sky gradient with drifting clouds and floating bubbles (toddlers & kids)
 * or a calm aurora with stars (pre-teens / dark mode).
 */
@Composable
fun KidsBackground(group: AgeGroup, dark: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val transition = rememberInfiniteTransition(label = "bg")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(38_000, easing = LinearEasing)), label = "drift")
    val bob by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart), label = "bob")
    val bubbles = remember { List(14) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.5f + 0.5f) } }
    val night = dark && group == AgeGroup.PRE_TEEN
    Canvas(modifier) {
        val top = when {
            night -> Color(0xFF0E1B2C)
            group == AgeGroup.TODDLER -> Color(0xFFFFF4E0)
            else -> Color(0xFFE3F4FF)
        }
        val bottom = when {
            night -> Color(0xFF12322A)
            group == AgeGroup.TODDLER -> Color(0xFFE6F8EE)
            else -> Color(0xFFEFFAF3)
        }
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        if (night) {
            bubbles.forEach { (x, y, s) ->
                val twinkle = (sin((bob + x) * 2 * PI) + 1f).toFloat() / 2f
                drawCircle(Color.White.copy(alpha = 0.3f + 0.5f * twinkle), 2.5f * s, Offset(size.width * x, size.height * y * 0.6f))
            }
            return@Canvas
        }
        // clouds
        listOf(0.12f to 0.9f, 0.28f to 0.65f, 0.08f to 0.45f).forEachIndexed { i, (y, scale) ->
            val w = size.width
            val x = ((drift * (1f + i * 0.35f) + i * 0.37f) % 1f) * (w + 300f) - 150f
            val c = Offset(x, size.height * y)
            val r = 34f * scale * (size.minDimension / 400f)
            val cloud = Color.White.copy(alpha = 0.85f)
            drawCircle(cloud, r, c)
            drawCircle(cloud, r * 1.25f, c + Offset(r, -r * 0.4f))
            drawCircle(cloud, r, c + Offset(r * 2.1f, 0f))
            drawRoundRect(cloud, topLeft = c + Offset(0f, -r * 0.2f), size = Size(r * 2.1f, r * 1.2f), cornerRadius = CornerRadius(r))
        }
        // floating bubbles
        bubbles.forEach { (x, y, s) ->
            val yy = ((y - bob * s * 0.3f) % 1f + 1f) % 1f
            val c = Offset(size.width * x + sin((bob + x) * 2 * PI).toFloat() * 12f, size.height * (0.35f + yy * 0.65f))
            drawCircle(confettiColors[(x * 6).toInt().coerceIn(0, 5)].copy(alpha = 0.10f), 26f * s, c)
            drawCircle(Color.White.copy(alpha = 0.35f), 6f * s, c - Offset(8f * s, 8f * s))
        }
    }
}
