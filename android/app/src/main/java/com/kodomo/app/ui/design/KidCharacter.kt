package com.kodomo.app.ui.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** The kids' own characters (girls always with a headscarf). Replaceable by artist PNGs in assets/art later. */
enum class CharacterKind(val label: String) {
    GIRL("دختر"), BOY("پسر"), TEEN_GIRL("دختر نوجوان"), TEEN_BOY("پسر نوجوان"), LITTLE_GIRL("دختر کوچولو"), LITTLE_BOY("پسر کوچولو");

    companion object {
        fun forChild(age: Int, girl: Boolean) = when {
            age >= 10 -> if (girl) TEEN_GIRL else TEEN_BOY
            age < 5 -> if (girl) LITTLE_GIRL else LITTLE_BOY
            else -> if (girl) GIRL else BOY
        }
    }
}

private val skinLight = Color(0xFFFFE0C8)
private val skinShade = Color(0xFFF1B993)

/**
 * A clay-style bust character drawn with soft gradients (light from the top-left, rim shading, glossy eye
 * highlights, blinking). Used for avatars, the home header and greeting scenes.
 */
@Composable
fun KidCharacter(kind: CharacterKind, size: Dp, modifier: Modifier = Modifier, scarf: Color? = null) {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 4200; 1f at 3800; 0.1f at 3900; 1f at 4000 }),
        label = "b",
    )
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "br")
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val cx = s / 2f
        val headR = s * if (kind == CharacterKind.LITTLE_BOY || kind == CharacterKind.LITTLE_GIRL) 0.27f else 0.24f
        val headC = Offset(cx, s * 0.42f - breathe * s * 0.008f)
        val girl = kind == CharacterKind.GIRL || kind == CharacterKind.TEEN_GIRL || kind == CharacterKind.LITTLE_GIRL
        val teen = kind == CharacterKind.TEEN_BOY || kind == CharacterKind.TEEN_GIRL
        val shirt = when (kind) {
            CharacterKind.BOY, CharacterKind.LITTLE_BOY -> listOf(Color(0xFFFFD54F), Color(0xFFF5A623))
            CharacterKind.TEEN_BOY -> listOf(Color(0xFF5C6BC0), Color(0xFF303F9F))
            CharacterKind.TEEN_GIRL -> listOf(Color(0xFF9C7BE8), Color(0xFF6A4BC4))
            else -> listOf(Color(0xFFC7A3F0), Color(0xFF9B6FDB))
        }
        val scarfColors = (scarf ?: if (kind == CharacterKind.TEEN_GIRL) Color(0xFF7E57C2) else Color(0xFFF48FB1)).let {
            listOf(lerp(it, Color.White, 0.25f), it, lerp(it, Color.Black, 0.2f))
        }
        // body / shoulders
        val body = Path().apply {
            moveTo(cx - s * 0.36f, s); cubicTo(cx - s * 0.36f, s * 0.72f, cx - s * 0.2f, s * 0.64f, cx, s * 0.64f)
            cubicTo(cx + s * 0.2f, s * 0.64f, cx + s * 0.36f, s * 0.72f, cx + s * 0.36f, s); close()
        }
        drawPath(body, Brush.verticalGradient(shirt, startY = s * 0.64f, endY = s))
        drawPath(body, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(cx - s * 0.12f, s * 0.72f), s * 0.25f))
        if (girl) {
            // headscarf: round hood behind the head that drapes over the shoulders
            val hood = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(headC.x - headR * 1.32f, headC.y - headR * 1.36f, headC.x + headR * 1.32f, headC.y + headR * 1.25f))
            }
            val drape = Path().apply {
                moveTo(headC.x - headR * 1.25f, headC.y); cubicTo(headC.x - headR * 1.5f, s * 0.78f, cx - s * 0.22f, s * 0.86f, cx, s * 0.84f)
                cubicTo(cx + s * 0.22f, s * 0.86f, headC.x + headR * 1.5f, s * 0.78f, headC.x + headR * 1.25f, headC.y); close()
            }
            val scarfBrush = Brush.radialGradient(scarfColors, Offset(headC.x - headR * 0.5f, headC.y - headR * 0.8f), headR * 2.6f)
            drawPath(drape, scarfBrush)
            drawPath(hood, scarfBrush)
        } else {
            drawRect(Brush.verticalGradient(listOf(skinShade, skinLight)), Offset(cx - headR * 0.32f, headC.y + headR * 0.6f), Size(headR * 0.64f, s * 0.12f))
        }
        // face
        val faceR = if (girl) headR * 0.92f else headR
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEBDD), skinLight, skinShade), Offset(headC.x - faceR * 0.35f, headC.y - faceR * 0.4f), faceR * 1.5f), faceR, headC)
        if (!girl) {
            // ears
            listOf(-1f, 1f).forEach { d -> drawCircle(Brush.radialGradient(listOf(skinLight, skinShade), Offset(headC.x + d * faceR * 0.98f, headC.y + faceR * 0.05f), faceR * 0.22f), faceR * 0.18f, Offset(headC.x + d * faceR * 0.98f, headC.y + faceR * 0.08f)) }
            // hair
            val hair = Path().apply {
                moveTo(headC.x - faceR * 1.02f, headC.y - faceR * 0.05f)
                cubicTo(headC.x - faceR * 1.1f, headC.y - faceR * 1.25f, headC.x + faceR * 1.1f, headC.y - faceR * 1.25f, headC.x + faceR * 1.02f, headC.y - faceR * 0.05f)
                cubicTo(headC.x + faceR * 0.7f, headC.y - faceR * 0.55f, headC.x - faceR * 0.2f, headC.y - faceR * 0.75f, headC.x - faceR * 1.02f, headC.y - faceR * 0.05f); close()
            }
            drawPath(hair, Brush.verticalGradient(listOf(Color(0xFF5D4037), Color(0xFF3E2723)), startY = headC.y - faceR * 1.2f, endY = headC.y))
            if (kind == CharacterKind.BOY || kind == CharacterKind.LITTLE_BOY) {
                // green cap like the boards
                val cap = Path().apply {
                    moveTo(headC.x - faceR * 1.0f, headC.y - faceR * 0.35f)
                    cubicTo(headC.x - faceR * 1.0f, headC.y - faceR * 1.35f, headC.x + faceR * 1.0f, headC.y - faceR * 1.35f, headC.x + faceR * 1.0f, headC.y - faceR * 0.35f); close()
                }
                drawPath(cap, Brush.radialGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)), Offset(headC.x - faceR * 0.3f, headC.y - faceR * 1.0f), faceR * 1.6f))
                drawOval(Brush.verticalGradient(listOf(Color(0xFF43A047), Color(0xFF1B5E20))), Offset(headC.x - faceR * 0.2f, headC.y - faceR * 0.48f), Size(faceR * 1.35f, faceR * 0.28f))
            }
            if (kind == CharacterKind.TEEN_BOY) {
                // headphones
                drawArc(Color(0xFF263238), 180f, 180f, false, Offset(headC.x - faceR * 1.15f, headC.y - faceR * 1.2f), Size(faceR * 2.3f, faceR * 2.2f), style = Stroke(faceR * 0.16f, cap = StrokeCap.Round))
                listOf(-1f, 1f).forEach { d -> drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF546E7A), Color(0xFF263238))), Offset(headC.x + d * faceR * 1.05f - faceR * 0.18f, headC.y - faceR * 0.25f), Size(faceR * 0.36f, faceR * 0.55f), androidx.compose.ui.geometry.CornerRadius(faceR * 0.15f)) }
            }
        } else {
            // the scarf's front band over the forehead
            drawArc(Brush.verticalGradient(scarfColors.take(2)), 200f, 140f, false, Offset(headC.x - faceR * 1.02f, headC.y - faceR * 1.02f), Size(faceR * 2.04f, faceR * 2.04f), style = Stroke(faceR * 0.28f, cap = StrokeCap.Round))
        }
        face(headC, faceR, blink, teen)
    }
}

private fun DrawScope.face(c: Offset, r: Float, blink: Float, teen: Boolean) {
    val eyeY = c.y + r * 0.05f
    listOf(-1f, 1f).forEach { d ->
        val ec = Offset(c.x + d * r * 0.36f, eyeY)
        val ew = r * (if (teen) 0.2f else 0.24f); val eh = r * (if (teen) 0.24f else 0.3f) * blink
        drawOval(Color(0xFF3B2A25), Offset(ec.x - ew / 2f, ec.y - eh / 2f), Size(ew, eh.coerceAtLeast(r * 0.03f)))
        if (blink > 0.5f) {
            drawCircle(Color.White, r * 0.06f, Offset(ec.x + ew * 0.18f, ec.y - eh * 0.2f))
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.03f, Offset(ec.x - ew * 0.2f, ec.y + eh * 0.2f))
        }
        // eyebrows
        drawArc(Color(0xFF5D4037), 200f, 140f, false, Offset(ec.x - ew * 0.7f, ec.y - eh * 1.2f - r * 0.12f), Size(ew * 1.4f, r * 0.2f), style = Stroke(r * 0.05f, cap = StrokeCap.Round))
        // cheeks
        drawCircle(Color(0x55FF8A80), r * 0.13f, Offset(c.x + d * r * 0.58f, c.y + r * 0.38f))
    }
    // nose and smile
    drawCircle(Color(0x33B5651D), r * 0.06f, Offset(c.x, c.y + r * 0.28f))
    drawArc(Color(0xFF8D3B2F), 20f, 140f, false, Offset(c.x - r * 0.25f, c.y + r * 0.22f), Size(r * 0.5f, r * 0.38f), style = Stroke(r * 0.07f, cap = StrokeCap.Round))
}

private fun lerp(a: Color, b: Color, f: Float) = Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, a.alpha)
