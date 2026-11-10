package com.kodomo.app.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin

private data class Palette(
    val sky: List<Color>, val far: List<Color>, val mid: List<Color>, val near: List<Color>,
    val flowers: List<Color>, val cloud: Color,
)

private fun palette(style: KidStyle, night: Boolean): Palette = when {
    night || style == KidStyle.GALAXY -> Palette(
        sky = listOf(Color(0xFF141B3D), Color(0xFF2B2C64), Color(0xFF4B3A7A)),
        far = listOf(Color(0xFF3E3874), Color(0xFF2B285A)), mid = listOf(Color(0xFF34306A), Color(0xFF221F4A)),
        near = listOf(Color(0xFF2A2B55), Color(0xFF181936)), flowers = listOf(Color(0xFF9FA8FF), Color(0xFFE1B7FF), Color(0xFF7FE0D8)),
        cloud = Color(0xFF6E6AA8),
    )
    style == KidStyle.BLOSSOM -> Palette(
        sky = listOf(Color(0xFFF3D9FF), Color(0xFFFFE3F1), Color(0xFFFFF1F6)),
        far = listOf(Color(0xFFE6C7F2), Color(0xFFD6B0EA)), mid = listOf(Color(0xFFB9E7B0), Color(0xFF8ED08A)),
        near = listOf(Color(0xFF9EDC8F), Color(0xFF6FBF6A)), flowers = listOf(Color(0xFFFF8FC0), Color(0xFFFFD36E), Color(0xFFB98CFF), Color.White),
        cloud = Color.White,
    )
    style == KidStyle.SKY -> Palette(
        sky = listOf(Color(0xFF7CC7FF), Color(0xFFA9DCFF), Color(0xFFDDF3FF)),
        far = listOf(Color(0xFFB2E3C7), Color(0xFF8FD1AE)), mid = listOf(Color(0xFF88D39B), Color(0xFF5DB878)),
        near = listOf(Color(0xFF7CCB6A), Color(0xFF4FA94F)), flowers = listOf(Color(0xFFFFD36E), Color(0xFFFF8A80), Color.White),
        cloud = Color.White,
    )
    else -> Palette( // MEADOW
        sky = listOf(Color(0xFF8ED6F5), Color(0xFFBDE9F6), Color(0xFFE6F8EC)),
        far = listOf(Color(0xFFB9E4C3), Color(0xFF94D3A8)), mid = listOf(Color(0xFF8CD48C), Color(0xFF61B86A)),
        near = listOf(Color(0xFF86CF63), Color(0xFF4FA845)), flowers = listOf(Color(0xFFFFD36E), Color(0xFFFF9EBB), Color.White, Color(0xFFB98CFF)),
        cloud = Color.White,
    )
}

/**
 * The child's world behind every kids screen: sky, sun or moon, puffy 3D clouds, layered soft hills, grass and
 * flowers (a castle for «Blossom», stars and a planet for teens and at night). Gently animated, drawn in code so it
 * stays sharp on every screen size.
 */
@Composable
fun KidScene(style: KidStyle, night: Boolean, modifier: Modifier = Modifier, groundHeight: Float = 0.2f) {
    val p = remember(style, night) { palette(style, night) }
    val dark = night || style == KidStyle.GALAXY
    // Moving parts are separate layers moved by the GPU (graphicsLayer), so the detailed scene is drawn only once.
    val drift by rememberInfiniteTransition(label = "scene").animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "t")
    val spin by rememberInfiniteTransition(label = "sun").animateFloat(0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "r")
    androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
        // static: sky, stars, moon/planet, castle, mountains, hills, grass and flowers
        Canvas(Modifier.fillMaxSize()) { drawStatic(p, style, dark, groundHeight) }
        if (!dark) {
            // rotating sun rays
            Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.2f, sunY(this.size)) }) {
                val sc = Offset(size.width * 0.2f, size.width * 0.2f)
                for (k in 0 until 12) rotate(k * 30f, sc) {
                    drawRoundRect(Color(0xFFFFC94D), Offset(sc.x - size.width * 0.008f, sc.y - size.width * 0.13f), Size(size.width * 0.016f, size.width * 0.035f), androidx.compose.ui.geometry.CornerRadius(size.width * 0.008f))
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val sc = Offset(size.width * 0.2f, size.width * 0.2f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4B8), Color(0xFFFFC542), Color(0xFFFFA726)), Offset(sc.x - size.width * 0.025f, sc.y - size.width * 0.025f), size.width * 0.11f), size.width * 0.075f, sc)
            }
        }
        // clouds drift across (each its own layer)
        listOf(Triple(0.1f, 0.2f, 0.16f), Triple(0.7f, 0.1f, 0.12f), Triple(0.45f, 0.33f, 0.09f)).forEachIndexed { i, (x0, y, r) ->
            Canvas(Modifier.fillMaxSize().graphicsLayer { translationX = (((x0 + drift * (1f - i * 0.25f)) % 1.3f) - 0.15f - x0) * size.width }) {
                cloud(Offset(size.width * x0, size.height * y), size.width * r, p.cloud, dark)
            }
        }
    }
}

private fun sunY(size: Size): Float = if (size.height <= 0f) 0f else (size.width * 0.2f) / size.height

private fun DrawScope.drawStatic(p: Palette, style: KidStyle, dark: Boolean, groundHeight: Float) {
    val w = size.width; val h = size.height
    drawRect(Brush.verticalGradient(p.sky))
    if (dark) {
        val rnd = java.util.Random(11)
        repeat(70) { drawCircle(Color.White.copy(alpha = 0.35f + rnd.nextFloat() * 0.6f), 1.2f + rnd.nextFloat() * 2.4f, Offset(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.65f)) }
        val mc = Offset(w * 0.2f, w * 0.22f)
        drawCircle(Brush.radialGradient(listOf(Color(0x55FFF3C4), Color.Transparent), mc, w * 0.22f), w * 0.22f, mc)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6D8), Color(0xFFF2D58A)), Offset(mc.x - w * 0.02f, mc.y - w * 0.02f), w * 0.09f), w * 0.07f, mc)
        if (style == KidStyle.GALAXY) {
            val pc = Offset(w * 0.82f, h * 0.3f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFB08CFF), Color(0xFF5B3FB5)), Offset(pc.x - 10f, pc.y - 10f), w * 0.08f), w * 0.06f, pc)
            drawOval(Color(0x99E1D4FF), Offset(pc.x - w * 0.1f, pc.y - w * 0.012f), Size(w * 0.2f, w * 0.024f))
        }
    } else {
        val sc = Offset(w * 0.2f, w * 0.2f)
        drawCircle(Brush.radialGradient(listOf(Color(0x88FFE9A8), Color.Transparent), sc, w * 0.26f), w * 0.26f, sc)
    }
    if (style == KidStyle.BLOSSOM && !dark) castle(Offset(w * 0.72f, h * (0.78f - groundHeight)), w * 0.2f)
    if (dark) mountains(w, h, groundHeight, p)
    hill(w, h, h * (1f - groundHeight - 0.13f), 0.18f, 0.6f, p.far)
    hill(w, h, h * (1f - groundHeight - 0.06f), 0.12f, 1.4f, p.mid)
    hill(w, h, h * (1f - groundHeight), 0.06f, 2.3f, p.near)
    val rnd = java.util.Random(5)
    val base = h * (1f - groundHeight)
    repeat(60) {
        val x = rnd.nextFloat() * w
        val y = base + rnd.nextFloat() * h * groundHeight * 0.9f + h * 0.02f
        val len = h * (0.01f + rnd.nextFloat() * 0.015f)
        drawLine(p.near[0].copy(alpha = 0.9f), Offset(x, y), Offset(x + 2f, y - len), strokeWidth = 3f)
    }
    repeat(22) {
        val x = rnd.nextFloat() * w
        val y = base + h * 0.03f + rnd.nextFloat() * h * groundHeight * 0.8f
        flower(Offset(x, y), h * (0.007f + rnd.nextFloat() * 0.006f), p.flowers[it % p.flowers.size])
    }
}

private fun DrawScope.hill(w: Float, h: Float, top: Float, amp: Float, phase: Float, colors: List<Color>) {
    val path = Path().apply {
        moveTo(0f, top)
        val steps = 8
        for (i in 1..steps) {
            val x0 = w * (i - 1) / steps; val x1 = w * i / steps
            val y = top - h * amp * 0.5f * (sin(phase + i * 0.9f) + 1f) / 2f
            quadraticBezierTo((x0 + x1) / 2f, y - h * 0.02f, x1, top - h * amp * 0.25f * (sin(phase + i * 1.3f) + 1f) / 2f)
        }
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(path, Brush.verticalGradient(colors, startY = top - h * amp, endY = h))
    drawPath(path, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), startY = top - h * amp, endY = top + h * 0.05f))
}

private fun DrawScope.mountains(w: Float, h: Float, ground: Float, p: Palette) {
    val base = h * (1f - ground - 0.08f)
    val path = Path().apply {
        moveTo(0f, base)
        lineTo(w * 0.15f, base - h * 0.16f); lineTo(w * 0.3f, base - h * 0.05f); lineTo(w * 0.5f, base - h * 0.22f)
        lineTo(w * 0.68f, base - h * 0.07f); lineTo(w * 0.85f, base - h * 0.18f); lineTo(w, base - h * 0.06f); lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFF6C5FB0), p.far.last()), startY = base - h * 0.22f, endY = base))
}

private fun DrawScope.cloud(c: Offset, r: Float, color: Color, dark: Boolean) {
    val shade = if (dark) color.copy(alpha = 0.55f) else Color(0xFFDDEBF5)
    // soft shadow under the cloud, then the puffs with a bottom-shaded gradient (looks 3D)
    drawOval(Color.Black.copy(alpha = if (dark) 0.10f else 0.05f), Offset(c.x - r * 1.3f, c.y + r * 0.45f), Size(r * 2.6f, r * 0.35f))
    val puffs = listOf(Offset(-0.75f, 0.15f) to 0.55f, Offset(0f, -0.15f) to 0.75f, Offset(0.75f, 0.1f) to 0.55f, Offset(0.35f, 0.25f) to 0.5f, Offset(-0.35f, 0.25f) to 0.5f)
    puffs.forEach { (o, s) ->
        val pc = Offset(c.x + o.x * r, c.y + o.y * r)
        drawCircle(Brush.verticalGradient(listOf(color.copy(alpha = if (dark) 0.6f else 1f), shade), startY = pc.y - r * s, endY = pc.y + r * s), r * s, pc)
    }
}

private fun DrawScope.flower(c: Offset, r: Float, color: Color) {
    for (i in 0 until 5) {
        val a = Math.toRadians(i * 72.0)
        drawCircle(color, r, Offset(c.x + (kotlin.math.cos(a) * r * 1.1f).toFloat(), c.y + (kotlin.math.sin(a) * r * 1.1f).toFloat()))
    }
    drawCircle(Color(0xFFFFC542), r * 0.8f, c)
}

private fun DrawScope.castle(c: Offset, s: Float) {
    val wall = Brush.verticalGradient(listOf(Color(0xFFFBE3F2), Color(0xFFE9B9DA)), startY = c.y - s, endY = c.y)
    drawRect(wall, Offset(c.x - s * 0.5f, c.y - s * 0.55f), Size(s, s * 0.55f))
    listOf(-0.55f, -0.1f, 0.35f).forEachIndexed { i, dx ->
        val tw = s * 0.22f; val th = s * (0.85f + i % 2 * 0.25f)
        drawRect(wall, Offset(c.x + dx * s, c.y - th), Size(tw, th))
        val roof = Path().apply { moveTo(c.x + dx * s - tw * 0.15f, c.y - th); lineTo(c.x + dx * s + tw / 2f, c.y - th - s * 0.3f); lineTo(c.x + dx * s + tw * 1.15f, c.y - th); close() }
        drawPath(roof, Brush.verticalGradient(listOf(Color(0xFFB98CFF), Color(0xFF8E63E6)), startY = c.y - th - s * 0.3f, endY = c.y - th))
    }
}
