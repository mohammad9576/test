package com.kodomo.app.ui.kids.games

import android.graphics.Color as AndroidColor
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.components.CameraView
import com.kodomo.app.ui.kids.LocalKidSpace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

enum class HuntColor(val label: String, val swatch: Color, val hueRange: ClosedFloatingPointRange<Float>) {
    RED("قرمز", Color(0xFFE53935), 0f..15f),
    ORANGE("نارنجی", Color(0xFFFB8C00), 16f..40f),
    YELLOW("زرد", Color(0xFFFDD835), 41f..68f),
    GREEN("سبز", Color(0xFF43A047), 69f..165f),
    BLUE("آبی", Color(0xFF1E88E5), 166f..255f),
    PURPLE("بنفش", Color(0xFF8E24AA), 256f..330f),
    ;

    fun matches(hue: Float, saturation: Float, value: Float): Boolean {
        if (saturation < 0.35f || value < 0.25f) return false
        if (this == RED && hue >= 331f) return true
        return hue in hueRange
    }
}

/** Samples the centre of each camera frame and reports its average HSV colour. */
class CenterColorSampler {
    val hsv = MutableStateFlow(floatArrayOf(0f, 0f, 0f))

    fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val cx = image.width / 2
        val cy = image.height / 2
        val half = (minOf(image.width, image.height) * 0.08f).toInt().coerceAtLeast(4)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        var y = cy - half
        while (y < cy + half) {
            var x = cx - half
            while (x < cx + half) {
                val i = y * rowStride + x * pixelStride
                if (i + 2 < buffer.limit()) {
                    r += buffer.get(i).toInt() and 0xFF
                    g += buffer.get(i + 1).toInt() and 0xFF
                    b += buffer.get(i + 2).toInt() and 0xFF
                    n++
                }
                x += 3
            }
            y += 3
        }
        if (n == 0) return
        val out = FloatArray(3)
        AndroidColor.RGBToHSV((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), out)
        hsv.value = out
    }
}

@Composable
fun ColorHuntGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val sampler = remember { CenterColorSampler() }
    val sequence = remember { HuntColor.entries.shuffled() }
    var index by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(90) }
    val hsv by sampler.hsv.collectAsState()
    val target = sequence.getOrNull(index)
    val matching = target?.matches(hsv[0], hsv[1], hsv[2]) == true

    LaunchedEffect(target) { target?.let { space.voice.speak("یک چیز ${it.label} پیدا کن!") } }
    LaunchedEffect(matching, target) {
        if (matching && target != null) {
            delay(800)
            space.voice.speak("آفرین! ${target.label} را پیدا کردی!")
            index++
        }
    }
    LaunchedEffect(Unit) {
        while (seconds > 0 && index < sequence.size) {
            delay(1_000)
            seconds--
        }
        onFinish(index * 5 + if (index == sequence.size) seconds / 6 else 0)
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).background(target?.swatch ?: Color.Gray, CircleShape))
            Text(
                if (target != null) "یک چیز «${target.label}» پیدا کن!" else "همه‌ی رنگ‌ها پیدا شد!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text("⏱ ${seconds.fa()}", style = MaterialTheme.typography.titleMedium)
        }
        Text("پیدا شده: ${index.fa()} از ${sequence.size.fa()}", style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CameraView(modifier = Modifier.fillMaxSize(), analyzer = sampler::analyze)
            Box(
                Modifier
                    .size(90.dp)
                    .border(5.dp, if (matching) Color(0xFF5CE0A0) else Color.White, RoundedCornerShape(20.dp)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .size(56.dp)
                    .background(Color.hsv(hsv[0].coerceIn(0f, 360f), hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0f, 1f)), CircleShape)
                    .border(3.dp, Color.White, CircleShape),
            )
        }
    }
}
