package com.kodomo.app.ui.kids.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.theme.KodomoColors
import kotlin.math.hypot
import kotlin.random.Random

/** Tilt the phone to roll the rescue ball to the chicks while avoiding the mud holes. */
@Composable
fun TiltRescueGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val context = LocalContext.current
    var area by remember { mutableStateOf(IntSize.Zero) }
    var ball by remember { mutableStateOf(Offset.Zero) }
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var tilt by remember { mutableStateOf(Offset.Zero) }
    var rescued by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableFloatStateOf(60f) }
    val chicks = remember { mutableStateListOf<Offset>() }
    val holes = remember { mutableStateListOf<Offset>() }
    val measurer = rememberTextMeasurer()
    val target = 5 + level * 2

    AccelerometerEffect { x, y, _ -> tilt = Offset(-x, y) }

    fun randomPoint(): Offset {
        val m = 60f
        return Offset(Random.nextFloat() * (area.width - 2 * m) + m, Random.nextFloat() * (area.height - 2 * m) + m)
    }

    LaunchedEffect(area) {
        if (area == IntSize.Zero) return@LaunchedEffect
        ball = Offset(area.width / 2f, area.height / 2f)
        holes.clear()
        repeat(2 + level) { holes += randomPoint() }
        chicks.clear()
        repeat(3) { chicks += randomPoint() }
        var last = withFrameNanos { it }
        while (timeLeft > 0f && rescued < target) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            timeLeft -= dt
            val friction = 0.985f
            velocity = Offset((velocity.x + tilt.x * 160f * dt) * friction, (velocity.y + tilt.y * 160f * dt) * friction)
            var next = ball + velocity * dt * 6f
            val r = 34f
            if (next.x < r || next.x > area.width - r) velocity = velocity.copy(x = -velocity.x * 0.5f)
            if (next.y < r || next.y > area.height - r) velocity = velocity.copy(y = -velocity.y * 0.5f)
            next = Offset(next.x.coerceIn(r, area.width - r), next.y.coerceIn(r, area.height - r))
            ball = next
            if (holes.any { hypot(it.x - ball.x, it.y - ball.y) < 42f }) {
                ball = Offset(area.width / 2f, area.height / 2f)
                velocity = Offset.Zero
            }
            val hit = chicks.indexOfFirst { hypot(it.x - ball.x, it.y - ball.y) < 60f }
            if (hit >= 0) {
                chicks.removeAt(hit)
                rescued++
                chicks += randomPoint()
            }
        }
        onFinish(rescued * 5 + if (rescued >= target) timeLeft.toInt() / 3 else 0, rescued >= target)
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            if (hasAccelerometer(context)) "🐣 نجات‌یافته: ${rescued.fa()} از ${target.fa()}   ⏱ ${timeLeft.toInt().fa()}" else "این دستگاه حسگر شتاب‌سنج ندارد.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { area = it },
            ) {
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFDDF7E8), Color(0xFFB9E4CB))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f))
                holes.forEach { h ->
                    drawCircle(Color(0xFF7A5230), 40f, h)
                    drawCircle(Color(0xFF4E321C), 28f, h)
                }
                chicks.forEach { c ->
                    val layout = measurer.measure("🐣", TextStyle(fontSize = 36.sp))
                    drawText(layout, topLeft = c - Offset(layout.size.width / 2f, layout.size.height / 2f))
                }
                drawCircle(Color.Black.copy(alpha = 0.12f), 36f, ball + Offset(4f, 6f))
                drawCircle(Brush.radialGradient(listOf(Color.White, KodomoColors.Sky), center = ball - Offset(10f, 10f), radius = 50f), 34f, ball)
            }
        }
    }
}
