package com.kodomo.app.ui.kids.games.pose

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.VoicePhrases
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

/** Camera stage shared by all body games: preview, skeleton, a game layer and the «step back» coaching hint. */
@Composable
private fun PoseStage(
    tracker: PoseTracker,
    hud: String,
    modifier: Modifier = Modifier,
    onSize: (IntSize) -> Unit = {},
    layer: DrawScope.() -> Unit,
) {
    val space = LocalKidSpace.current
    val visible = tracker.pose?.upperBodyVisible == true
    LaunchedEffect(visible) {
        if (!visible) {
            delay(1_500)
            space.voice.speak(VoicePhrases.BODY_STEP_BACK)
        }
    }
    Column(modifier.fillMaxSize()) {
        Text(hud, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
                .onSizeChanged(onSize),
        ) {
            PoseCamera(tracker, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) { layer() }
            if (!visible) {
                Text(
                    "🙋 " + VoicePhrases.BODY_STEP_BACK,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                )
            }
        }
    }
}

/** Hand positions of the current pose in stage pixels. */
private fun handsOnStage(pose: BodyPose?, stage: IntSize): List<Offset> {
    if (pose == null || stage == IntSize.Zero) return emptyList()
    val size = Size(stage.width.toFloat(), stage.height.toFloat())
    return pose.hands.map { pose.toScreen(it, size) }
}

private data class Floater(val id: Int, val x: Float, val y: Float, val speed: Float, val color: Color, val bad: Boolean = false)

private val balloonColors = listOf(KodomoColors.Rose, KodomoColors.Sky, KodomoColors.Sun, KodomoColors.Lavender, KodomoColors.GreenLight, KodomoColors.Peach)

private fun DrawScope.drawBalloon(center: Offset, r: Float, color: Color) {
    drawOval(color, topLeft = center - Offset(r, r * 1.2f), size = Size(r * 2, r * 2.4f))
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.25f, center - Offset(r * 0.4f, r * 0.6f))
    val string = Path().apply {
        moveTo(center.x, center.y + r * 1.2f)
        quadraticTo(center.x + r * 0.3f, center.y + r * 1.8f, center.x, center.y + r * 2.4f)
    }
    drawPath(string, Color.White, style = Stroke(3f))
}

/** Balloons rise; touch them with your «magic hands» or jump to pop every balloon on screen at once. */
@Composable
fun JumpPopGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val jump = remember { PoseJumpDetector() }
    val balloons = remember { mutableStateListOf<Floater>() }
    var stage by remember { mutableStateOf(IntSize.Zero) }
    var popped by remember { mutableIntStateOf(0) }
    var jumps by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    var nextId by remember { mutableIntStateOf(0) }
    val finish by rememberUpdatedState(onFinish)

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            if (jump.update(pose) && balloons.isNotEmpty()) {
                popped += balloons.size
                jumps++
                balloons.clear()
                space.voice.speak(VoicePhrases.BALLOON_JUMP)
            }
            val hands = handsOnStage(pose, stage)
            if (hands.isEmpty() || stage == IntSize.Zero) return@collect
            val r = minOf(stage.width, stage.height) * 0.09f
            val hit = balloons.filter { b ->
                val c = Offset(b.x * stage.width, b.y * stage.height)
                hands.any { h -> hypot(h.x - c.x, h.y - c.y) < r * 1.3f }
            }
            if (hit.isNotEmpty()) {
                balloons.removeAll(hit)
                popped += hit.size
            }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        var spawn = 0L
        while (seconds > 0) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50)
            last = now
            spawn += dt
            if (spawn > 900 && balloons.size < 7) {
                spawn = 0
                balloons += Floater(nextId++, Random.nextFloat() * 0.8f + 0.1f, 1.15f, Random.nextFloat() * 0.00007f + 0.00008f, balloonColors.random())
            }
            for (i in balloons.indices.reversed()) {
                val b = balloons[i]
                val ny = b.y - b.speed * dt
                if (ny < -0.15f) balloons.removeAt(i) else balloons[i] = b.copy(y = ny)
            }
        }
        finish(popped * 2 + jumps * 3)
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🎈 ترکانده‌شده: ${popped.fa()} • پرش: ${jumps.fa()} • ⏱ ${seconds.fa()}", onSize = { stage = it }) {
        balloons.forEach { b ->
            drawBalloon(Offset(size.width * b.x, size.height * b.y), size.minDimension * 0.09f, b.color)
        }
    }
}

/** Catch falling stars with your hands; avoid the rain clouds. */
@Composable
fun StarCatchGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val items = remember { mutableStateListOf<Floater>() }
    var stage by remember { mutableStateOf(IntSize.Zero) }
    var stars by remember { mutableIntStateOf(0) }
    var clouds by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    var nextId by remember { mutableIntStateOf(0) }
    val measurer = rememberTextMeasurer()
    val finish by rememberUpdatedState(onFinish)

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val hands = handsOnStage(tracker.pose, stage)
            if (hands.isEmpty()) return@collect
            val r = minOf(stage.width, stage.height) * 0.08f
            val hit = items.filter { s ->
                val c = Offset(s.x * stage.width, s.y * stage.height)
                hands.any { h -> hypot(h.x - c.x, h.y - c.y) < r * 1.4f }
            }
            hit.forEach { if (it.bad) clouds++ else stars++ }
            if (hit.any { it.bad }) space.voice.speak(VoicePhrases.STAR_CLOUD)
            items.removeAll(hit)
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        var spawn = 0L
        while (seconds > 0) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50)
            last = now
            spawn += dt
            if (spawn > 750) {
                spawn = 0
                val bad = Random.nextFloat() < 0.22f
                items += Floater(nextId++, Random.nextFloat() * 0.8f + 0.1f, -0.1f, Random.nextFloat() * 0.00008f + 0.0001f, KodomoColors.Star, bad)
            }
            for (i in items.indices.reversed()) {
                val s = items[i]
                val ny = s.y + s.speed * dt
                if (ny > 1.1f) items.removeAt(i) else items[i] = s.copy(y = ny)
            }
        }
        finish((stars * 2 - clouds).coerceAtLeast(0))
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "⭐ ${stars.fa()}   🌧️ ${clouds.fa()}   ⏱ ${seconds.fa()}", onSize = { stage = it }) {
        items.forEach { s ->
            val layout = measurer.measure(if (s.bad) "🌧️" else "⭐", TextStyle(fontSize = 44.sp))
            drawText(layout, topLeft = Offset(size.width * s.x, size.height * s.y) - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
    }
}

/** The robot shows a pose; hold the same pose for 1.5 seconds. */
@Composable
fun PoseMatchGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val sequence = remember { (TargetPose.entries + TargetPose.entries.shuffled()).take(8) }
    var index by remember { mutableIntStateOf(0) }
    var hold by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(12) }
    val target = sequence.getOrNull(index)
    val finish by rememberUpdatedState(onFinish)

    LaunchedEffect(index) {
        val t = sequence.getOrNull(index)
        if (t == null) {
            finish(score * 5)
            return@LaunchedEffect
        }
        space.voice.speak(t.prompt)
        hold = 0f
        remaining = 12
        var lastTick = System.currentTimeMillis()
        var spokeHold = false
        while (true) {
            delay(80)
            val now = System.currentTimeMillis()
            val pose = tracker.pose
            if (pose != null && t.matches(pose)) {
                hold += (now - lastTick) / 1500f
                if (!spokeHold) {
                    spokeHold = true
                    space.voice.speak(VoicePhrases.POSE_HOLD)
                }
            } else {
                hold = (hold - 0.04f).coerceAtLeast(0f)
            }
            lastTick = now
            if (hold >= 1f) {
                score++
                space.voice.speak(VoicePhrases.POSE_DONE)
                delay(900)
                index++
                break
            }
        }
    }
    LaunchedEffect(index) {
        repeat(12) {
            delay(1_000)
            remaining = 11 - it
        }
        if (index < sequence.size && hold < 1f) index++
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(110.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                if (target != null) Canvas(Modifier.size(96.dp)) { drawTargetFigure(target) }
            }
            Column(Modifier.weight(1f)) {
                Text(target?.prompt ?: VoicePhrases.POSE_DONE, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { hold.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = KodomoColors.Green)
                Text("ژست ${(index + 1).coerceAtMost(sequence.size).fa()} از ${sequence.size.fa()} • امتیاز ${score.fa()} • ⏱ ${remaining.fa()}", style = MaterialTheme.typography.labelLarge)
            }
        }
        PoseStage(tracker, "", Modifier.weight(1f), layer = {})
    }
}

/** Stick figure demonstrating the requested pose. */
private fun DrawScope.drawTargetFigure(pose: TargetPose) {
    val s = size.minDimension
    val c = KodomoColors.GreenDark
    val head = Offset(s * 0.5f, s * 0.18f)
    val neck = Offset(s * 0.5f, s * 0.3f)
    val hip = Offset(s * 0.5f, s * 0.62f)
    val lShoulder = Offset(s * 0.38f, s * 0.33f)
    val rShoulder = Offset(s * 0.62f, s * 0.33f)
    val w = s * 0.045f
    drawCircle(c, s * 0.09f, head)
    drawLine(c, neck, hip, w, StrokeCap.Round)
    drawLine(c, lShoulder, rShoulder, w, StrokeCap.Round)
    drawLine(c, hip, Offset(s * 0.38f, s * 0.92f), w, StrokeCap.Round)
    drawLine(c, hip, Offset(s * 0.62f, s * 0.92f), w, StrokeCap.Round)
    val down = { from: Offset, dir: Float -> Offset(from.x + dir * s * 0.06f, s * 0.6f) }
    val up = { from: Offset, dir: Float -> Offset(from.x + dir * s * 0.08f, s * 0.04f) }
    val side = { from: Offset, dir: Float -> Offset(from.x + dir * s * 0.34f, from.y) }
    val onHead = { _: Offset, dir: Float -> Offset(head.x + dir * s * 0.06f, head.y - s * 0.06f) }
    // Mirror view: the child's right hand is drawn on the right side of the figure.
    val (left, right) = when (pose) {
        TargetPose.BOTH_UP -> up(lShoulder, -1f) to up(rShoulder, 1f)
        TargetPose.AIRPLANE -> side(lShoulder, -1f) to side(rShoulder, 1f)
        TargetPose.RIGHT_UP -> down(lShoulder, -1f) to up(rShoulder, 1f)
        TargetPose.LEFT_UP -> up(lShoulder, -1f) to down(rShoulder, 1f)
        TargetPose.HANDS_ON_HEAD -> onHead(lShoulder, -1f) to onHead(rShoulder, 1f)
    }
    drawLine(c, lShoulder, left, w, StrokeCap.Round)
    drawLine(c, rShoulder, right, w, StrokeCap.Round)
}

/** Dance while the music plays; freeze like a statue when it stops. Three lives. */
@Composable
fun FreezeDanceGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val tracker = remember { PoseTracker() }
    var phase by remember { mutableStateOf("ready") }
    var lives by remember { mutableIntStateOf(3) }
    var rounds by remember { mutableIntStateOf(0) }
    var wobble by remember { mutableFloatStateOf(0f) }
    val finish by rememberUpdatedState(onFinish)
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(DANCE_TRACK))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(Unit) {
        var previous: BodyPose? = null
        snapshotFlow { tracker.frames }.collect {
            val current = tracker.pose
            val m = movement(previous, current)
            wobble = wobble * 0.7f + m * 0.3f
            previous = current
        }
    }
    LaunchedEffect(Unit) {
        delay(1_500)
        while (lives > 0 && rounds < 8) {
            phase = "dance"
            space.voice.speak(VoicePhrases.FREEZE_DANCE)
            player.play()
            delay(Random.nextLong(5_000, 9_000))
            player.pause()
            phase = "freeze"
            space.voice.speak(VoicePhrases.FREEZE_NOW)
            delay(900)
            var moved = false
            val end = System.currentTimeMillis() + 3_000
            while (System.currentTimeMillis() < end) {
                if (wobble > 0.05f) moved = true
                delay(60)
            }
            rounds++
            if (moved) {
                lives--
                phase = "moved"
                space.voice.speak(VoicePhrases.FREEZE_MOVED)
            } else {
                phase = "great"
                space.voice.speak(VoicePhrases.FREEZE_GREAT)
            }
            delay(1_600)
        }
        finish((rounds - (3 - lives)).coerceAtLeast(0) * 5)
    }

    Box(Modifier.fillMaxSize()) {
        PoseStage(tracker, "🧊 دور ${rounds.fa()} • جان: ${"❤️".repeat(lives)}", layer = {
            val frozen = phase == "freeze"
            drawRect(if (frozen) Color(0x5580D8FF) else Color.Transparent)
        })
        Column(Modifier.align(Alignment.TopCenter).padding(top = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            KodomoMascot(if (phase == "dance") MascotMood.EXCITED else if (phase == "moved") MascotMood.THINKING else MascotMood.HAPPY, size = 96.dp)
            Text(
                when (phase) {
                    "dance" -> "💃 " + VoicePhrases.FREEZE_DANCE
                    "freeze" -> "🧊 " + VoicePhrases.FREEZE_NOW
                    "moved" -> VoicePhrases.FREEZE_MOVED
                    "great" -> VoicePhrases.FREEZE_GREAT
                    else -> VoicePhrases.BODY_READY
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Count jumping jacks («پروانه»); every rep releases a butterfly. */
@Composable
fun JumpingJacksGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val counter = remember { JumpingJackCounter() }
    var count by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    val butterflies = remember { mutableStateListOf<Floater>() }
    val measurer = rememberTextMeasurer()
    val goal = 15
    val finish by rememberUpdatedState(onFinish)

    LaunchedEffect(Unit) { space.voice.speak(VoicePhrases.JACKS_START) }
    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            if (counter.update(pose)) {
                count = counter.count
                VoicePhrases.numbers.getOrNull(count - 1)?.let { space.voice.speak(it) }
                butterflies += Floater(count, Random.nextFloat() * 0.8f + 0.1f, 0.9f, 0.0002f, balloonColors.random())
            }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        while (seconds > 0 && count < goal) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50)
            last = now
            for (i in butterflies.indices.reversed()) {
                val b = butterflies[i]
                val ny = b.y - b.speed * dt
                if (ny < -0.1f) butterflies.removeAt(i) else butterflies[i] = b.copy(y = ny, x = (b.x + (Random.nextFloat() - 0.5f) * 0.004f).coerceIn(0.05f, 0.95f))
            }
        }
        finish(count * 2 + if (count >= goal) seconds / 3 else 0)
    }
    LaunchedEffect(Unit) { while (seconds > 0 && count < goal) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🦋 ${count.fa()} از ${goal.fa()} • ⏱ ${seconds.fa()}") {
        butterflies.forEach { b ->
            val layout = measurer.measure("🦋", TextStyle(fontSize = 40.sp))
            drawText(layout, topLeft = Offset(size.width * b.x, size.height * b.y) - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
    }
}

/** Royalty-free upbeat track used by Freeze Dance (streams; the game still works silently when offline). */
private const val DANCE_TRACK = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
