package com.kodomo.app.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.theme.KodomoColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Kodomo robot, fully vector-drawn and animated: floating idle motion, blinking, glowing antenna,
 * mood-specific faces (happy, excited, sleepy, thinking) and a waving arm when excited.
 */
@Composable
fun KodomoMascot(mood: MascotMood, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val floatPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (mood == MascotMood.SLEEPY) 5200 else 3400, easing = LinearEasing)),
        label = "float",
    )
    val blink by transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3800
                0.1f at 3920
                1f at 4050
            },
        ),
        label = "blink",
    )
    val glow by transition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val wave by transition.animateFloat(
        initialValue = -18f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave",
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val bob = sin(floatPhase * 2 * PI).toFloat() * s * if (mood == MascotMood.EXCITED) 0.045f else 0.025f
        // soft ground shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(s * 0.28f, s * 0.92f),
            size = Size(s * 0.44f, s * 0.06f),
        )
        translate(top = bob) {
            drawRobot(s, mood, blink, glow, wave)
        }
        if (mood == MascotMood.SLEEPY) {
            val zAlpha = (sin(floatPhase * 2 * PI).toFloat() + 1f) / 2f
            listOf(0.0f to 13, 0.07f to 17, 0.14f to 21).forEachIndexed { i, (dy, fontPx) ->
                drawText(
                    textMeasurer = textMeasurer,
                    text = "z",
                    topLeft = Offset(s * (0.74f + i * 0.06f), s * (0.14f - dy) + bob),
                    style = TextStyle(color = KodomoColors.Green.copy(alpha = 0.35f + zAlpha * 0.5f), fontSize = (fontPx * s / 200f).sp, fontWeight = FontWeight.Black),
                )
            }
        }
        if (mood == MascotMood.THINKING) {
            listOf(0.72f to 0.16f, 0.79f to 0.1f, 0.87f to 0.05f).forEachIndexed { i, (x, y) ->
                drawCircle(KodomoColors.Green.copy(alpha = 0.3f + 0.2f * i * glow), radius = s * (0.018f + i * 0.008f), center = Offset(s * x, s * y + bob))
            }
        }
    }
}

private fun DrawScope.drawRobot(s: Float, mood: MascotMood, blink: Float, glow: Float, wave: Float) {
    val white = Color.White
    val shell = Color(0xFFEAF2EE)
    val green = KodomoColors.Green
    val screen = KodomoColors.Ink
    val eye = KodomoColors.Glow

    // antenna
    drawLine(green, Offset(s * 0.5f, s * 0.13f), Offset(s * 0.5f, s * 0.2f), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
    drawCircle(eye.copy(alpha = 0.25f * glow), radius = s * 0.055f, center = Offset(s * 0.5f, s * 0.11f))
    drawCircle(eye.copy(alpha = 0.6f + 0.4f * glow), radius = s * 0.03f, center = Offset(s * 0.5f, s * 0.11f))

    // body
    drawRoundRect(shell, topLeft = Offset(s * 0.33f, s * 0.66f), size = Size(s * 0.34f, s * 0.24f), cornerRadius = CornerRadius(s * 0.1f))
    drawRoundRect(white, topLeft = Offset(s * 0.34f, s * 0.665f), size = Size(s * 0.32f, s * 0.22f), cornerRadius = CornerRadius(s * 0.1f))
    drawCircle(green.copy(alpha = 0.85f), radius = s * 0.035f, center = Offset(s * 0.5f, s * 0.77f))
    drawCircle(eye.copy(alpha = glow), radius = s * 0.015f, center = Offset(s * 0.5f, s * 0.77f))

    // arms
    drawRoundRect(white, topLeft = Offset(s * 0.25f, s * 0.68f), size = Size(s * 0.09f, s * 0.16f), cornerRadius = CornerRadius(s * 0.045f))
    val armAngle = if (mood == MascotMood.EXCITED) wave - 140f else 0f
    rotate(armAngle, pivot = Offset(s * 0.7f, s * 0.7f)) {
        drawRoundRect(white, topLeft = Offset(s * 0.66f, s * 0.68f), size = Size(s * 0.09f, s * 0.16f), cornerRadius = CornerRadius(s * 0.045f))
    }

    // headphone band
    drawArc(
        color = green, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(s * 0.17f, s * 0.17f), size = Size(s * 0.66f, s * 0.5f),
        style = Stroke(width = s * 0.035f, cap = StrokeCap.Round),
    )

    // head
    drawRoundRect(shell, topLeft = Offset(s * 0.2f, s * 0.22f), size = Size(s * 0.6f, s * 0.46f), cornerRadius = CornerRadius(s * 0.16f))
    drawRoundRect(white, topLeft = Offset(s * 0.21f, s * 0.215f), size = Size(s * 0.58f, s * 0.44f), cornerRadius = CornerRadius(s * 0.16f))

    // ear cups
    drawRoundRect(green, topLeft = Offset(s * 0.12f, s * 0.34f), size = Size(s * 0.11f, s * 0.22f), cornerRadius = CornerRadius(s * 0.055f))
    drawRoundRect(green, topLeft = Offset(s * 0.77f, s * 0.34f), size = Size(s * 0.11f, s * 0.22f), cornerRadius = CornerRadius(s * 0.055f))
    drawRoundRect(KodomoColors.GreenLight, topLeft = Offset(s * 0.145f, s * 0.38f), size = Size(s * 0.05f, s * 0.14f), cornerRadius = CornerRadius(s * 0.025f))
    drawRoundRect(KodomoColors.GreenLight, topLeft = Offset(s * 0.805f, s * 0.38f), size = Size(s * 0.05f, s * 0.14f), cornerRadius = CornerRadius(s * 0.025f))

    // face screen
    drawRoundRect(screen, topLeft = Offset(s * 0.27f, s * 0.29f), size = Size(s * 0.46f, s * 0.31f), cornerRadius = CornerRadius(s * 0.11f))
    drawRoundRect(Color.White.copy(alpha = 0.06f), topLeft = Offset(s * 0.3f, s * 0.305f), size = Size(s * 0.2f, s * 0.06f), cornerRadius = CornerRadius(s * 0.03f))

    // cheeks
    drawCircle(KodomoColors.Rose.copy(alpha = 0.45f), radius = s * 0.022f, center = Offset(s * 0.33f, s * 0.5f))
    drawCircle(KodomoColors.Rose.copy(alpha = 0.45f), radius = s * 0.022f, center = Offset(s * 0.67f, s * 0.5f))

    val leftEye = Offset(s * 0.4f, s * 0.42f)
    val rightEye = Offset(s * 0.6f, s * 0.42f)
    val eyeStroke = Stroke(width = s * 0.028f, cap = StrokeCap.Round)
    when (mood) {
        MascotMood.HAPPY -> {
            listOf(leftEye, rightEye).forEach { c ->
                scale(1f, blink, pivot = c) {
                    drawArc(eye, 200f, 140f, false, topLeft = c - Offset(s * 0.045f, s * 0.03f), size = Size(s * 0.09f, s * 0.08f), style = eyeStroke)
                }
            }
            drawArc(eye, 20f, 140f, false, topLeft = Offset(s * 0.43f, s * 0.44f), size = Size(s * 0.14f, s * 0.09f), style = eyeStroke)
        }
        MascotMood.EXCITED -> {
            listOf(leftEye, rightEye).forEach { c ->
                scale(1f, blink, pivot = c) {
                    drawCircle(eye, radius = s * 0.042f, center = c)
                    drawCircle(Color.White, radius = s * 0.013f, center = c + Offset(-s * 0.012f, -s * 0.014f))
                }
            }
            drawArc(eye, 0f, 180f, true, topLeft = Offset(s * 0.44f, s * 0.47f), size = Size(s * 0.12f, s * 0.09f))
        }
        MascotMood.SLEEPY -> {
            listOf(leftEye, rightEye).forEach { c ->
                drawArc(eye, 20f, 140f, false, topLeft = c - Offset(s * 0.04f, s * 0.035f), size = Size(s * 0.08f, s * 0.05f), style = eyeStroke)
            }
            drawCircle(eye, radius = s * 0.018f, center = Offset(s * 0.5f, s * 0.53f), style = Stroke(width = s * 0.014f))
        }
        MascotMood.THINKING -> {
            scale(1f, blink, pivot = leftEye) { drawCircle(eye, radius = s * 0.035f, center = leftEye) }
            drawLine(eye, rightEye - Offset(s * 0.04f, 0f), rightEye + Offset(s * 0.04f, -s * 0.012f), strokeWidth = s * 0.026f, cap = StrokeCap.Round)
            drawLine(eye, Offset(s * 0.46f, s * 0.53f), Offset(s * 0.56f, s * 0.51f), strokeWidth = s * 0.024f, cap = StrokeCap.Round)
        }
    }
}

/** Mascot with a speech bubble; used across the kids space for guidance and emotional feedback. */
@Composable
fun MascotSpeech(text: String, mood: MascotMood, modifier: Modifier = Modifier, mascotSize: Dp = 96.dp) {
    val jump = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var tapped by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val bubble = androidx.compose.runtime.remember(text) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(text) { bubble.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 250f)) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KodomoMascot(
            mood = if (tapped) MascotMood.EXCITED else mood,
            size = mascotSize,
            modifier = Modifier
                .graphicsLayer { translationY = -jump.value * 60f; rotationZ = jump.value * 12f }
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        tapped = true
                        launch { com.kodomo.app.ui.fx.Sfx.pop() }
                        jump.animateTo(1f, androidx.compose.animation.core.tween(180))
                        jump.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.3f, stiffness = 300f))
                        tapped = false
                    }
                },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = 0.7f + 0.3f * bubble.value
                    scaleY = 0.7f + 0.3f * bubble.value
                    alpha = bubble.value.coerceIn(0f, 1f)
                }
                .shadow(2.dp, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
