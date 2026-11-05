package com.kodomo.app.ui.kids.games.pose

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.PoseLandmark
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.Sfx
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Short floating «+۱» / «عالی!» texts used by all sports games. */
private data class Pop(val text: String, val x: Float, val y: Float, val life: Float, val color: Color)

private fun MutableList<Pop>.age(dt: Float) {
    for (i in indices.reversed()) {
        val p = this[i]
        val n = p.copy(y = p.y - dt * 0.12f, life = p.life - dt * 1.2f)
        if (n.life <= 0f) removeAt(i) else this[i] = n
    }
}

@Composable
private fun popsDrawer(): DrawScope.(List<Pop>) -> Unit {
    val measurer = rememberTextMeasurer()
    return { pops ->
        pops.forEach { p ->
            val layout = measurer.measure(p.text, TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = p.color.copy(alpha = p.life.coerceIn(0f, 1f))))
            drawText(layout, topLeft = Offset(size.width * p.x - layout.size.width / 2f, size.height * p.y))
        }
    }
}

private fun DrawScope.glove(c: Offset, r: Float, color: Color) {
    drawCircle(Color.Black.copy(alpha = 0.25f), r * 1.08f, c + Offset(r * 0.12f, r * 0.15f))
    drawCircle(color, r, c)
    drawCircle(Color.White.copy(alpha = 0.45f), r * 0.32f, c - Offset(r * 0.35f, r * 0.35f))
    drawRoundRect(Color.White, topLeft = c + Offset(-r * 0.7f, r * 0.55f), size = Size(r * 1.4f, r * 0.5f), cornerRadius = CornerRadius(r * 0.2f))
}

// ---------------------------------------------------------------------------------------------------------------------
// 1) Boxing: punch the pads before they fade. A punch needs a fast hand, so waving slowly does not count.
// ---------------------------------------------------------------------------------------------------------------------

private data class Pad(val id: Int, val x: Float, val y: Float, val life: Float, val gold: Boolean)

@Composable
fun BoxingGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val pads = remember { mutableStateListOf<Pad>() }
    val pops = remember { mutableStateListOf<Pop>() }
    var stage by remember { mutableStateOf(IntSize.Zero) }
    var hits by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    var hands by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val previous = remember { mutableStateListOf<Offset>() }
    val finish by rememberUpdatedState(onFinish)
    val target = 18 + level * 4
    val drawPops = popsDrawer()

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            if (stage == IntSize.Zero) return@collect
            val now = handsOnStage(tracker.pose, stage).map { Offset(it.x / stage.width, it.y / stage.height) }
            now.forEachIndexed { i, h ->
                val p = previous.getOrNull(i) ?: return@forEachIndexed
                if (hypot(h.x - p.x, h.y - p.y) < 0.03f) return@forEachIndexed // too slow for a punch
                pads.firstOrNull { hypot(it.x - h.x, (it.y - h.y) * stage.height / stage.width) < 0.11f }?.let { pad ->
                    pads.remove(pad)
                    hits += if (pad.gold) 3 else 1
                    combo++
                    best = maxOf(best, combo)
                    pops += Pop(if (pad.gold) "+۳ 💥" else if (combo >= 3) "×${combo.fa()}" else "+۱", pad.x, pad.y, 1f, if (pad.gold) Color(0xFFFFD54F) else Color.White)
                }
            }
            previous.clear(); previous.addAll(now)
            hands = now
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        var spawn = 0L
        var id = 0
        val lifeSeconds = (2.6f - level * 0.15f).coerceAtLeast(1.1f)
        while (seconds > 0) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            spawn += (dt * 1000).toLong()
            if (spawn > (950 - level * 55).coerceAtLeast(380) && pads.size < 2 + level / 3) {
                spawn = 0
                pads += Pad(id++, Random.nextFloat() * 0.7f + 0.15f, Random.nextFloat() * 0.45f + 0.12f, 1f, Random.nextFloat() < 0.12f)
            }
            for (i in pads.indices.reversed()) {
                val p = pads[i]
                val n = p.copy(life = p.life - dt / lifeSeconds)
                if (n.life <= 0f) { pads.removeAt(i); combo = 0 } else pads[i] = n
            }
            pops.age(dt)
        }
        finish(hits * 2 + best, hits >= target)
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🥊 ضربه: ${hits.fa()} از ${target.fa()}   🔥 ${best.fa()}   ⏱ ${seconds.fa()}", onSize = { stage = it }) {
        pads.forEach { p ->
            val c = Offset(size.width * p.x, size.height * p.y)
            val r = size.minDimension * 0.09f
            drawCircle(if (p.gold) Color(0xFFFFC107) else Color(0xFFE53935), r, c)
            drawCircle(Color.White, r * 0.62f, c)
            drawCircle(if (p.gold) Color(0xFFFFC107) else Color(0xFFE53935), r * 0.3f, c)
            drawArc(Color.White, -90f, 360f * p.life, false, topLeft = c - Offset(r * 1.25f, r * 1.25f), size = Size(r * 2.5f, r * 2.5f), style = Stroke(r * 0.12f, cap = StrokeCap.Round))
        }
        hands.forEach { h -> glove(Offset(h.x * size.width, h.y * size.height), size.minDimension * 0.06f, Color(0xFF1E88E5)) }
        drawPops(this, pops)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 2) Ski: lean your body left and right to steer between the gates and around the trees.
// ---------------------------------------------------------------------------------------------------------------------

private data class Slope(val x: Float, val y: Float, val gate: Boolean, val passed: Boolean = false)

@Composable
fun SkiGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val things = remember { mutableStateListOf<Slope>() }
    val pops = remember { mutableStateListOf<Pop>() }
    var skierX by remember { mutableFloatStateOf(0.5f) }
    var gates by remember { mutableIntStateOf(0) }
    var crashes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    var lean by remember { mutableFloatStateOf(0f) }
    val finish by rememberUpdatedState(onFinish)
    val target = 10 + level * 2
    val drawPops = popsDrawer()

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            val ls = pose.visible(PoseLandmark.LEFT_SHOULDER) ?: return@collect
            val rs = pose.visible(PoseLandmark.RIGHT_SHOULDER) ?: return@collect
            lean = ((ls.x + rs.x) / 2f - 0.5f) * 2.6f
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        var spawn = 0f
        val speed = 0.32f + level * 0.04f
        while (seconds > 0 && crashes < 5) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            skierX = (skierX + (0.5f + lean.coerceIn(-0.45f, 0.45f) - skierX) * (dt * 6f)).coerceIn(0.08f, 0.92f)
            spawn += dt
            if (spawn > (1.1f - level * 0.06f).coerceAtLeast(0.5f)) {
                spawn = 0f
                val gate = Random.nextFloat() < 0.55f
                things += Slope(Random.nextFloat() * 0.7f + 0.15f, 1.1f, gate)
            }
            for (i in things.indices.reversed()) {
                val t = things[i]
                var n = t.copy(y = t.y - speed * dt)
                if (!n.passed && n.y < 0.32f && t.y >= 0.32f) {
                    if (n.gate && abs(n.x - skierX) < 0.11f) { gates++; pops += Pop("+۱ 🚩", n.x, 0.3f, 1f, Color(0xFF1565C0)); n = n.copy(passed = true) }
                    if (!n.gate && abs(n.x - skierX) < 0.07f) { crashes++; pops += Pop("آخ! 🌲", n.x, 0.3f, 1f, Color(0xFFD32F2F)); n = n.copy(passed = true) }
                }
                if (n.y < -0.1f) things.removeAt(i) else things[i] = n
            }
            pops.age(dt)
        }
        finish(gates * 3 - crashes, gates >= target)
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "⛷️ دروازه: ${gates.fa()} از ${target.fa()}   🌲 ${crashes.fa()} از ۵   ⏱ ${seconds.fa()}") {
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color(0xFFE3F2FD).copy(alpha = 0.7f))))
        things.forEach { t ->
            val c = Offset(size.width * t.x, size.height * t.y)
            val s = size.minDimension * 0.06f
            if (t.gate) {
                drawLine(Color(0xFF1565C0), c - Offset(s * 1.8f, s), c - Offset(s * 1.8f, -s), strokeWidth = 6f)
                drawLine(Color(0xFFD32F2F), c + Offset(s * 1.8f, -s), c + Offset(s * 1.8f, s), strokeWidth = 6f)
                drawCircle(Color(0xFF1565C0), s * 0.35f, c - Offset(s * 1.8f, s))
                drawCircle(Color(0xFFD32F2F), s * 0.35f, c + Offset(s * 1.8f, -s))
            } else {
                val tree = Path().apply { moveTo(c.x, c.y - s * 1.6f); lineTo(c.x - s, c.y + s * 0.6f); lineTo(c.x + s, c.y + s * 0.6f); close() }
                drawPath(tree, Color(0xFF2E7D32))
                drawRect(Color(0xFF6D4C41), Offset(c.x - s * 0.15f, c.y + s * 0.6f), Size(s * 0.3f, s * 0.5f))
            }
        }
        val skier = Offset(size.width * skierX, size.height * 0.3f)
        rotate(-lean * 25f, skier) {
            drawLine(Color(0xFFFF7043), skier + Offset(-30f, 28f), skier + Offset(30f, 28f), strokeWidth = 10f, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFFB74D), 22f, skier - Offset(0f, 26f))
            drawRoundRect(Color(0xFF7E57C2), topLeft = skier - Offset(16f, 6f), size = Size(32f, 34f), cornerRadius = CornerRadius(10f))
        }
        drawPops(this, pops)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 3) Jump rope: jump every time the rope swings under your feet.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun JumpRopeGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val jump = remember { PoseJumpDetector() }
    val pops = remember { mutableStateListOf<Pop>() }
    var phase by remember { mutableFloatStateOf(0f) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var lastJumpAt by remember { mutableStateOf(0L) }
    var seconds by remember { mutableIntStateOf(50) }
    val finish by rememberUpdatedState(onFinish)
    val target = 12 + level * 3
    val drawPops = popsDrawer()

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            if (jump.update(pose)) lastJumpAt = System.currentTimeMillis()
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        val period = (1.9f - level * 0.1f).coerceAtLeast(0.9f)
        var counted = false
        while (seconds > 0 && misses < 4) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            phase = (phase + dt / period) % 1f
            // the rope reaches the floor at phase ≈ 0.5: a jump within ±0.45 s of that moment counts
            if (phase > 0.5f && !counted) {
                counted = true
                if (System.currentTimeMillis() - lastJumpAt < 650) {
                    streak++; total++; best = maxOf(best, streak)
                    pops += Pop(if (streak % 5 == 0) "عالی! ×${streak.fa()}" else "+۱", 0.5f, 0.55f, 1f, Color(0xFFFFEB3B))
                    Sfx.pop()
                } else {
                    misses++; streak = 0
                    pops += Pop("بپر! ⬆️", 0.5f, 0.55f, 1f, Color(0xFFFF5252))
                }
            }
            if (phase < 0.5f) counted = false
            pops.age(dt)
        }
        finish(total * 2 + best, total >= target)
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🪢 پرش‌ها: ${total.fa()} از ${target.fa()}   🔥 ${best.fa()}   ❌ ${misses.fa()} از ۴   ⏱ ${seconds.fa()}") {
        val w = size.width
        val h = size.height
        val a = phase * 2 * Math.PI
        // rope as an ellipse arc swinging around the child (from the hands at the sides)
        val bottom = h * (0.5f + 0.45f * cos(a).toFloat() * -1f)
        val rope = Path().apply {
            moveTo(w * 0.08f, h * 0.55f)
            quadraticTo(w * 0.5f, bottom + (bottom - h * 0.55f), w * 0.92f, h * 0.55f)
        }
        val front = sin(a).toFloat() > 0f
        drawPath(rope, if (front) Color(0xFFFF4081) else Color(0xFFFF4081).copy(alpha = 0.45f), style = Stroke(if (front) 12f else 7f, cap = StrokeCap.Round))
        drawCircle(Color(0xFF5D4037), 18f, Offset(w * 0.08f, h * 0.55f))
        drawCircle(Color(0xFF5D4037), 18f, Offset(w * 0.92f, h * 0.55f))
        drawPops(this, pops)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 4) Sprint: run on the spot (lift your knees) to race the robot to the finish line.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun SprintRaceGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    var you by remember { mutableFloatStateOf(0f) }
    var robot by remember { mutableFloatStateOf(0f) }
    var steps by remember { mutableIntStateOf(0) }
    var lastKnee by remember { mutableIntStateOf(0) } // -1 left, 1 right
    var started by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    val finish by rememberUpdatedState(onFinish)
    val stepGain = 0.022f
    val robotSpeed = 0.035f + level * 0.006f

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            if (!started) return@collect
            val pose = tracker.pose ?: return@collect
            val torso = pose.torso ?: return@collect
            val lh = pose.visible(PoseLandmark.LEFT_HIP, 0.4f) ?: return@collect
            val rh = pose.visible(PoseLandmark.RIGHT_HIP, 0.4f) ?: return@collect
            val lk = pose.visible(PoseLandmark.LEFT_KNEE, 0.3f)
            val rk = pose.visible(PoseLandmark.RIGHT_KNEE, 0.3f)
            val hipY = (lh.y + rh.y) / 2f * pose.imageHeight
            fun lifted(k: PosePoint?) = k != null && (k.y * pose.imageHeight - hipY) < torso * 0.55f
            val side = when {
                lifted(lk) && !lifted(rk) -> -1
                lifted(rk) && !lifted(lk) -> 1
                else -> 0
            }
            if (side != 0 && side != lastKnee) {
                lastKnee = side
                steps++
                you = (you + stepGain).coerceAtMost(1f)
            }
        }
    }
    LaunchedEffect(Unit) {
        while (countdown > 0) { delay(900); countdown-- }
        started = true
        var last = withInfiniteAnimationFrameMillis { it }
        while (you < 1f && robot < 1f) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            robot = (robot + robotSpeed * dt).coerceAtMost(1f)
        }
        if (you >= 1f) Sfx.success()
        finish(steps + if (you >= 1f) 20 else 0, you >= 1f)
    }

    val measurer = rememberTextMeasurer()
    PoseStage(tracker, if (!started) "🏃 زانوها را بالا بیاور و درجا بدو! شروع در ${countdown.fa()}…" else "🏃 قدم‌ها: ${steps.fa()}   تو ${(you * 100).toInt().fa()}٪ – ربات ${(robot * 100).toInt().fa()}٪") {
        val w = size.width
        val h = size.height
        val trackTop = h * 0.68f
        drawRoundRect(Color(0xFFD84315).copy(alpha = 0.85f), Offset(0f, trackTop), Size(w, h * 0.3f), CornerRadius(24f))
        drawLine(Color.White, Offset(0f, trackTop + h * 0.15f), Offset(w, trackTop + h * 0.15f), strokeWidth = 4f)
        repeat(8) { i -> drawRect(if (i % 2 == 0) Color.White else Color.Black, Offset(w * 0.93f, trackTop + i * h * 0.0375f), Size(w * 0.03f, h * 0.0375f)) }
        val runner = measurer.measure("🏃", TextStyle(fontSize = 40.sp))
        val bot = measurer.measure("🤖", TextStyle(fontSize = 40.sp))
        drawText(runner, topLeft = Offset(w * 0.05f + you * w * 0.82f, trackTop + h * 0.16f))
        drawText(bot, topLeft = Offset(w * 0.05f + robot * w * 0.82f, trackTop + h * 0.01f))
        if (!started) {
            val c = measurer.measure(countdown.fa(), TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Black, color = Color.White))
            drawText(c, topLeft = Offset(w / 2 - c.size.width / 2f, h * 0.25f))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 5) Weightlifter: squat down and stand up; every rep lifts the barbell higher.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun SquatGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    var reps by remember { mutableIntStateOf(0) }
    var down by remember { mutableStateOf(false) }
    var depth by remember { mutableFloatStateOf(0f) }
    var standingHip by remember { mutableFloatStateOf(0f) }
    var seconds by remember { mutableIntStateOf(60) }
    val pops = remember { mutableStateListOf<Pop>() }
    val finish by rememberUpdatedState(onFinish)
    val target = 8 + level * 2
    val drawPops = popsDrawer()

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            val torso = pose.torso ?: return@collect
            val lh = pose.visible(PoseLandmark.LEFT_HIP, 0.4f) ?: return@collect
            val rh = pose.visible(PoseLandmark.RIGHT_HIP, 0.4f) ?: return@collect
            val hip = (lh.y + rh.y) / 2f * pose.imageHeight
            if (standingHip == 0f || (!down && hip < standingHip)) standingHip = hip
            depth = ((hip - standingHip) / (torso * 0.45f)).coerceIn(0f, 1.2f)
            if (!down && depth > 0.85f) down = true
            if (down && depth < 0.25f) {
                down = false
                reps++
                pops += Pop("+۱ 💪", 0.5f, 0.2f, 1f, Color(0xFFFFEB3B))
                Sfx.pop()
            }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        while (seconds > 0 && reps < target) {
            val now = withInfiniteAnimationFrameMillis { it }
            pops.age((now - last).coerceAtMost(50) / 1000f)
            last = now
        }
        finish(reps * 3 + seconds / 3, reps >= target)
    }
    LaunchedEffect(Unit) { while (seconds > 0 && reps < target) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🏋️ اسکات: ${reps.fa()} از ${target.fa()}   ⏱ ${seconds.fa()}   " + if (down) "حالا بلند شو! ⬆️" else "بنشین ⬇️") {
        val w = size.width
        val h = size.height
        // depth meter
        drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(w * 0.04f, h * 0.15f), Size(w * 0.05f, h * 0.6f), CornerRadius(20f))
        drawRoundRect(if (depth > 0.85f) Color(0xFF66BB6A) else Color(0xFFFFCA28), Offset(w * 0.04f, h * 0.75f - h * 0.6f * depth.coerceAtMost(1f)), Size(w * 0.05f, h * 0.6f * depth.coerceAtMost(1f)), CornerRadius(20f))
        // barbell rising with progress
        val progress = reps / target.toFloat()
        val barY = h * (0.85f - 0.6f * progress.coerceAtMost(1f))
        drawLine(Color(0xFF9E9E9E), Offset(w * 0.25f, barY), Offset(w * 0.75f, barY), strokeWidth = 12f, cap = StrokeCap.Round)
        listOf(0.25f, 0.75f).forEach { x ->
            drawRoundRect(Color(0xFF263238), Offset(w * x - 18f, barY - 50f), Size(36f, 100f), CornerRadius(8f))
            drawRoundRect(Color(0xFFE53935), Offset(w * x - 30f, barY - 36f), Size(60f, 72f), CornerRadius(10f))
        }
        drawPops(this, pops)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 6) Basketball: raise both hands above your head, then throw forward-down. Aim with where your hands are.
// ---------------------------------------------------------------------------------------------------------------------

private data class Shot(val fromX: Float, val toX: Float, val t: Float, val scored: Boolean)

@Composable
fun BasketballGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    var hoopX by remember { mutableFloatStateOf(0.5f) }
    var armed by remember { mutableStateOf(false) }
    var aim by remember { mutableFloatStateOf(0.5f) }
    var shot by remember { mutableStateOf<Shot?>(null) }
    var baskets by remember { mutableIntStateOf(0) }
    var throws by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(60) }
    val pops = remember { mutableStateListOf<Pop>() }
    val finish by rememberUpdatedState(onFinish)
    val target = 6 + level * 2
    val hoopWidth = (0.24f - level * 0.012f).coerceAtLeast(0.12f)
    val drawPops = popsDrawer()

    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            val nose = pose.visible(PoseLandmark.NOSE, 0.4f) ?: return@collect
            val lw = pose.visible(PoseLandmark.LEFT_WRIST, 0.4f) ?: return@collect
            val rw = pose.visible(PoseLandmark.RIGHT_WRIST, 0.4f) ?: return@collect
            val ls = pose.visible(PoseLandmark.LEFT_SHOULDER, 0.4f) ?: return@collect
            val handsX = (lw.x + rw.x) / 2f
            if (lw.y < nose.y && rw.y < nose.y) { armed = true; aim = handsX }
            else if (armed && lw.y > ls.y && rw.y > ls.y && shot == null) {
                armed = false
                throws++
                // where the hands were when the throw started decides the direction
                val to = (0.5f + (aim - 0.5f) * 1.6f).coerceIn(0.05f, 0.95f)
                shot = Shot(0.5f, to, 0f, abs(to - hoopX) < hoopWidth / 2f)
            }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        while (seconds > 0) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            if (level >= 3) hoopX = 0.5f + sin(now / (1600f - level * 80f)).toFloat() * (0.1f + level * 0.02f).coerceAtMost(0.32f)
            shot?.let { s ->
                val n = s.copy(t = s.t + dt / 0.9f)
                if (n.t >= 1f) {
                    shot = null
                    if (s.scored) { baskets++; pops += Pop("سبد! 🏀 +۲", s.toX, 0.25f, 1f, Color(0xFFFFEB3B)); Sfx.success() }
                    else pops += Pop("نزدیک بود!", s.toX, 0.3f, 1f, Color.White)
                } else shot = n
            }
            pops.age(dt)
        }
        finish(baskets * 4, baskets >= target)
    }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }

    PoseStage(tracker, "🏀 سبد: ${baskets.fa()} از ${target.fa()}   پرتاب: ${throws.fa()}   ⏱ ${seconds.fa()}   " + if (armed) "حالا پرتاب کن! 👇" else "دست‌ها بالای سر 🙌") {
        val w = size.width
        val h = size.height
        // backboard + hoop
        val hx = w * hoopX
        drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(hx - w * 0.14f, h * 0.06f), Size(w * 0.28f, h * 0.16f), CornerRadius(10f))
        drawRoundRect(Color(0xFFE53935), Offset(hx - w * 0.06f, h * 0.1f), Size(w * 0.12f, h * 0.08f), CornerRadius(4f), style = Stroke(5f))
        drawLine(Color(0xFFFF6F00), Offset(hx - w * hoopWidth / 2f, h * 0.24f), Offset(hx + w * hoopWidth / 2f, h * 0.24f), strokeWidth = 10f, cap = StrokeCap.Round)
        repeat(5) { i ->
            val x = hx - w * hoopWidth / 2f + i * w * hoopWidth / 4f
            drawLine(Color.White.copy(alpha = 0.8f), Offset(x, h * 0.24f), Offset(hx + (x - hx) * 0.6f, h * 0.32f), strokeWidth = 3f)
        }
        // aim indicator
        if (armed) drawCircle(Color(0xFF69F0AE).copy(alpha = 0.6f), 16f, Offset(w * (0.5f + (aim - 0.5f) * 1.6f).coerceIn(0.05f, 0.95f), h * 0.24f))
        // ball in flight (parabola from the bottom centre to the hoop height)
        shot?.let { s ->
            val x = w * (s.fromX + (s.toX - s.fromX) * s.t)
            val y = h * (0.85f - 1.3f * s.t + 0.72f * s.t * s.t)
            drawCircle(Color(0xFFFF8F00), w * 0.045f, Offset(x, y))
            drawLine(Color.Black.copy(alpha = 0.5f), Offset(x - w * 0.045f, y), Offset(x + w * 0.045f, y), strokeWidth = 3f)
        }
        drawPops(this, pops)
    }
}
