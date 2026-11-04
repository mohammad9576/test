package com.kodomo.app.ui.kids.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

private data class Spot(val position: Offset, val radius: Float, val dirt: Float)

/** Shake the phone to wash the mud off the puppy. Each strong shake fades a mud spot. */
@Composable
fun ShakeCleanGame(onFinish: (Int) -> Unit) {
    val spots = remember {
        mutableStateListOf<Spot>().apply {
            repeat(14) { add(Spot(Offset(Random.nextFloat(), Random.nextFloat()), Random.nextFloat() * 0.05f + 0.04f, 1f)) }
        }
    }
    var lastShake by remember { mutableLongStateOf(0L) }
    var shakes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    val clean = spots.count { it.dirt <= 0f }

    AccelerometerEffect { x, y, z ->
        val magnitude = sqrt(x * x + y * y + z * z) - 9.81f
        val now = System.currentTimeMillis()
        if (magnitude > 7f && now - lastShake > 160) {
            lastShake = now
            shakes++
            val dirty = spots.indices.filter { spots[it].dirt > 0f }
            if (dirty.isNotEmpty()) {
                val i = dirty.random()
                spots[i] = spots[i].copy(dirt = (spots[i].dirt - 0.34f).coerceAtLeast(0f))
            }
        }
    }
    LaunchedEffect(Unit) {
        while (spots.any { it.dirt > 0f } && seconds < 90) {
            delay(1_000)
            seconds++
        }
        onFinish(spots.count { it.dirt <= 0f } * 2 + (90 - seconds).coerceAtLeast(0) / 3)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🫧 لکه‌های پاک‌شده: ${clean.fa()} از ${spots.size.fa()}   ⏱ ${seconds.fa()}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { clean / spots.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = KodomoColors.Sky)
        Text("گوشی را محکم در دست بگیر و تکانش بده! (تکان‌ها: ${shakes.fa()})", style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("🐶", fontSize = 220.sp)
            Canvas(Modifier.size(300.dp)) {
                spots.forEach { spot ->
                    if (spot.dirt > 0f) {
                        val center = Offset(size.width * (0.15f + spot.position.x * 0.7f), size.height * (0.15f + spot.position.y * 0.7f))
                        drawCircle(Color(0xFF7A5230).copy(alpha = 0.85f * spot.dirt), size.minDimension * spot.radius, center)
                        drawCircle(Color(0xFF5C3D24).copy(alpha = 0.6f * spot.dirt), size.minDimension * spot.radius * 0.5f, center + Offset(6f, 4f))
                    }
                }
            }
        }
    }
}
