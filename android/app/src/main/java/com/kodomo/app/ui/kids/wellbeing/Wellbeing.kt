package com.kodomo.app.ui.kids.wellbeing

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.data.prefs.Wellbeing
import com.kodomo.app.ui.fx.Emoji
import kotlinx.coroutines.delay

/** Keeps media volume at or below the parent's limit while the kids space is open (hearing protection). */
@Composable
fun VolumeGuard(maxPercent: Int) {
    val context = LocalContext.current
    LaunchedEffect(maxPercent) {
        if (maxPercent >= 100) return@LaunchedEffect
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@LaunchedEffect
        while (true) {
            runCatching {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val cap = (max * maxPercent / 100f).toInt().coerceAtLeast(1)
                if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) > cap) audio.setStreamVolume(AudioManager.STREAM_MUSIC, cap, 0)
            }
            delay(1500)
        }
    }
}

/** Time allowed outside Kodomo for an emergency call, read by the app guard. */
object EmergencyCall {
    @Volatile var allowedUntil: Long = 0L
    val active: Boolean get() = System.currentTimeMillis() < allowedUntil

    /** Opens the dialer with [phone] filled in; the app guard lets the dialer stay open for five minutes. */
    fun dial(context: Context, phone: String, launch: (Intent) -> Boolean): Boolean {
        if (phone.isBlank()) return false
        allowedUntil = System.currentTimeMillis() + 5 * 60_000
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch(intent)
    }
}

/** Full-screen 20-second eye rest: look at something far away, blink, stretch. */
@Composable
fun EyeBreakOverlay(childName: String, speak: (String) -> Unit, onDone: () -> Unit) {
    BackHandler(enabled = true) { }
    var left by remember { mutableIntStateOf(20) }
    LaunchedEffect(Unit) {
        speak("$childName جان، وقت استراحت چشم‌هاست! به یک چیز دور نگاه کن و چند بار پلک بزن.")
        while (left > 0) { delay(1000); left-- }
        speak("آفرین! چشم‌هات تازه شدند.")
        onDone()
    }
    val pulse by rememberInfiniteTransition(label = "eye").animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "p")
    val tips = listOf("👀 به دورترین چیز اتاق نگاه کن", "😌 ده بار آرام پلک بزن", "🙆 دست‌هایت را بالا ببر و کش بیا", "💧 یک لیوان آب بنوش")
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF6DD5ED), Color(0xFF2193B0)))).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Emoji("🌳", 150.dp, Modifier.scale(pulse))
            Text("استراحت چشم‌ها", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("$left", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
            tips.forEachIndexed { i, t ->
                val on = (20 - left) / 5 == i
                Text(
                    t,
                    color = if (on) Color(0xFF0D3B4F) else Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) Color.White else Color.White.copy(alpha = 0.18f)).padding(12.dp),
                )
            }
        }
    }
}

/** Big friendly buttons to call mom or dad (shown when the parent saved their numbers). */
@Composable
fun CallParentsRow(w: Wellbeing, onCall: (String) -> Unit, modifier: Modifier = Modifier) {
    if (w.momPhone.isBlank() && w.dadPhone.isBlank()) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (w.momPhone.isNotBlank()) CallButton("👩", "زنگ به مامان", Modifier.weight(1f)) { onCall(w.momPhone) }
        if (w.dadPhone.isNotBlank()) CallButton("👨", "زنگ به بابا", Modifier.weight(1f)) { onCall(w.dadPhone) }
    }
}

@Composable
private fun CallButton(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFEBEE)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Emoji(emoji, 34.dp)
        Text("📞 $label", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

/** Shown instead of the games until today's practice target is met. */
@Composable
fun StudyFirstGate(done: Int, need: Int, onStudy: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Emoji("📚", 120.dp)
            Text("اول درس، بعد بازی!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(
                "قرار خانواده: امروز $need تمرین درست حل کن تا بازی‌ها باز شوند.\nتا الان: $done از $need ⭐",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(need.coerceAtMost(10)) { i -> Text(if (i < done) "⭐" else "☆", fontSize = 26.sp) }
            }
            Button(onClick = onStudy, modifier = Modifier.fillMaxWidth()) { Text("بزن بریم سراغ تمرین ✏️", fontSize = 18.sp) }
            androidx.compose.material3.TextButton(onClick = onBack) { Text("برگشت") }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class StudyFirstViewModel @javax.inject.Inject constructor(
    private val learning: com.kodomo.app.data.prefs.LearningStore,
) : androidx.lifecycle.ViewModel() {
    fun today(childId: Long) = learning.today(childId)
}

/** Games open only after the family's daily practice target (if the parent set one). */
@Composable
fun StudyFirst(childId: Long, onStudy: () -> Unit, onBack: () -> Unit, content: @Composable () -> Unit) {
    val need = com.kodomo.app.ui.kids.LocalWellbeing.current.studyFirstAnswers
    if (need <= 0) { content(); return }
    val vm: StudyFirstViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val today by remember(childId) { vm.today(childId) }.collectAsState(null)
    val t = today ?: return
    if (t.correct >= need) content() else StudyFirstGate(t.correct, need, onStudy, onBack)
}

/** Applies the parent's screen brightness to the kids space and keeps other apps offline if asked. */
@Composable
fun BrightnessAndInternet(brightnessPercent: Int, blockInternet: Boolean) {
    val context = LocalContext.current
    DisposableEffect(brightnessPercent) {
        val window = (context as? android.app.Activity)?.window
        val before = window?.attributes?.screenBrightness
        window?.let { w ->
            w.attributes = w.attributes.apply {
                screenBrightness = if (brightnessPercent <= 0) android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else brightnessPercent / 100f
            }
        }
        onDispose { window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = before ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } } }
    }
    DisposableEffect(blockInternet) {
        if (blockInternet) com.kodomo.app.kiosk.InternetBlockVpn.start(context)
        onDispose { if (blockInternet) com.kodomo.app.kiosk.InternetBlockVpn.stop(context) }
    }
}
