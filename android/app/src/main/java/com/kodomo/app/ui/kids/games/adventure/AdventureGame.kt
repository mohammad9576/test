package com.kodomo.app.ui.kids.games.adventure

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.PoseLandmark
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.GameMusic
import com.kodomo.app.ui.fx.GameSfx
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.games.pose.BodyPose
import com.kodomo.app.ui.kids.games.pose.PoseStage
import com.kodomo.app.ui.kids.games.pose.PoseTracker
import com.kodomo.app.ui.kids.games.pose.handsOnStage
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.domain.model.MascotMood
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================================================================
// Entry point: story card → countdown → mission of this stage. Stage = level (1..10) of the game.
// =====================================================================================================================

@Composable
fun AdventureGame(themeId: String, level: Int, onFinish: (Int, Boolean) -> Unit) {
    val theme = remember(themeId) { AdventureThemes.byId(themeId) }
    val stageIndex = (level - 1).coerceIn(0, 9)
    val stage = theme.stages[stageIndex]
    val space = LocalKidSpace.current
    var phase by remember(level) { mutableIntStateOf(0) } // 0 story, 1 countdown, 2 play
    var count by remember(level) { mutableIntStateOf(3) }

    DisposableEffect(theme) {
        GameMusic.play(theme.music)
        onDispose { GameMusic.release() }
    }

    when (phase) {
        0 -> StoryCard(theme, stageIndex, onStart = { phase = 1 })
        1 -> {
            LaunchedEffect(Unit) {
                space.voice.stop()
                while (count > 0) {
                    GameSfx.beep()
                    delay(800)
                    count--
                }
                GameSfx.beep(high = true)
                phase = 2
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom))), contentAlignment = Alignment.Center) {
                val pulse by rememberInfiniteTransition(label = "c").animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "p")
                Text(if (count > 0) count.fa() else "!برو", fontSize = (110 * pulse).sp, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
        else -> MissionHost(theme, stage, stageIndex, onFinish)
    }
}

/** Story before each stage: map of the 10 stages, powers earned so far, the narrator and the goal. */
@Composable
private fun StoryCard(theme: AdventureTheme, stageIndex: Int, onStart: () -> Unit) {
    val space = LocalKidSpace.current
    val stage = theme.stages[stageIndex]
    LaunchedEffect(stageIndex) { space.voice.speakAll(listOf(stage.title, stage.story)) }
    val float by rememberInfiniteTransition(label = "s").animateFloat(-6f, 6f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "f")
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom)))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${theme.emoji} ${theme.title}", style = MaterialTheme.typography.titleLarge, color = Color.White)
        StageMap(theme, stageIndex)
        Box(Modifier.padding(top = 4.dp).offset(y = float.dp)) { KodomoMascot(MascotMood.EXCITED, size = 130.dp) }
        Column(
            Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(26.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("مرحله‌ی ${(stageIndex + 1).fa()}: ${stage.title}", style = MaterialTheme.typography.titleLarge, color = theme.skyTop, textAlign = TextAlign.Center)
            Text("${stage.mission.emoji} مهارت: ${stage.mission.skill}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF6D4C41))
            Text(stage.story, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color(0xFF263238))
            Text("🎯 ${stage.goal}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
            Text("🎁 جایزه: ${stage.power}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8E24AA))
        }
        Text("جلوی دوربین بایست طوری که از سر تا زانو دیده شوی. فضای اطرافت خالی باشد.", style = MaterialTheme.typography.labelMedium, color = Color.White, textAlign = TextAlign.Center)
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) { Text("آماده‌ام! شروع ▶", fontSize = 20.sp) }
    }
}


@Composable
private fun StageMap(theme: AdventureTheme, current: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        theme.stages.forEachIndexed { i, s ->
            val done = i < current
            val here = i == current
            Box(
                Modifier
                    .size(if (here) 34.dp else 26.dp)
                    .background(
                        when {
                            done -> theme.accent
                            here -> Color.White
                            else -> Color.White.copy(alpha = 0.25f)
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) s.power.take(2) else if (here) s.mission.emoji else "🔒", fontSize = if (here) 18.sp else 12.sp)
            }
        }
    }
}

// =====================================================================================================================
// Shared mission runtime
// =====================================================================================================================

private enum class Kind { GOOD, BAD, BIG, COLOR }

private data class Thing(
    val id: Int, val x: Float, val y: Float, val vx: Float, val vy: Float,
    val emoji: String, val kind: Kind, val color: Int = -1, val life: Float = 1f, val hold: Float = 0f,
)

private data class Spark(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val life: Float)
private data class Floaty(val text: String, val x: Float, val y: Float, val life: Float, val color: Color)

/** All visual state of a running mission; drawn every frame on top of the camera. */
private class Scene {
    val things = mutableStateListOf<Thing>()
    val sparks = mutableStateListOf<Spark>()
    val floaties = mutableStateListOf<Floaty>()
    var hands by mutableStateOf<List<Offset>>(emptyList())
    var prevHands: List<Offset> = emptyList()
    var nose by mutableStateOf<Offset?>(null)
    var time by mutableFloatStateOf(0f)
    var nextId = 0

    fun burst(x: Float, y: Float, color: Color, n: Int = 12) {
        repeat(n) {
            val a = Random.nextFloat() * 2 * PI
            val v = Random.nextFloat() * 0.5f + 0.2f
            sparks += Spark(x, y, (cos(a) * v).toFloat(), (sin(a) * v).toFloat(), color, 1f)
        }
    }

    fun say(text: String, x: Float, y: Float, color: Color = Color.White) { floaties += Floaty(text, x, y, 1.2f, color) }

    fun step(dt: Float) {
        time += dt
        for (i in sparks.indices.reversed()) {
            val s = sparks[i]
            val n = s.copy(x = s.x + s.vx * dt, y = s.y + s.vy * dt, vy = s.vy + 0.6f * dt, life = s.life - dt * 1.6f)
            if (n.life <= 0f) sparks.removeAt(i) else sparks[i] = n
        }
        for (i in floaties.indices.reversed()) {
            val f = floaties[i]
            val n = f.copy(y = f.y - dt * 0.1f, life = f.life - dt)
            if (n.life <= 0f) floaties.removeAt(i) else floaties[i] = n
        }
    }

    /** Hand that moved fast enough this frame (a «punch»), normalised. */
    fun fastHands(minSpeed: Float = 0.03f): List<Offset> = hands.filterIndexed { i, h ->
        val p = prevHands.getOrNull(i) ?: return@filterIndexed false
        hypot(h.x - p.x, h.y - p.y) > minSpeed
    }
}

/** Measured emoji/text layouts are cached: measuring every frame is the most expensive part of drawing. */
private class Glyphs(private val measurer: TextMeasurer) {
    private val cache = HashMap<String, TextLayoutResult>()
    fun get(text: String, sizeSp: Int, color: Color = Color.Unspecified, bold: Boolean = false): TextLayoutResult =
        cache.getOrPut("$text|$sizeSp|${color.value}|$bold") {
            measurer.measure(text, TextStyle(fontSize = sizeSp.sp, color = color, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal))
        }
}

@Composable
private fun rememberGlyphs(): Glyphs {
    val measurer = rememberTextMeasurer()
    return remember(measurer) { Glyphs(measurer) }
}

private fun DrawScope.drawGlyph(g: TextLayoutResult, center: Offset, alpha: Float = 1f) {
    drawText(g, topLeft = center - Offset(g.size.width / 2f, g.size.height / 2f), alpha = alpha)
}

/** Themed scenery around the camera image so every adventure has its own world. */
private fun DrawScope.drawDecor(theme: AdventureTheme, t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(theme.skyTop.copy(alpha = 0.35f), Color.Transparent, theme.skyBottom.copy(alpha = 0.35f))))
    when (theme.decor) {
        Decor.FOREST -> {
            // hanging vines with leaves
            for (v in 0 until 5) {
                val x = w * (0.08f + v * 0.21f)
                val len = h * (0.12f + (v % 3) * 0.05f)
                val sway = sin(t * 1.2f + v) * 10f
                drawLine(Color(0xFF33691E), Offset(x, 0f), Offset(x + sway, len), strokeWidth = 6f)
                for (k in 1..3) drawCircle(Color(0xFF7CB342), 10f, Offset(x + sway * k / 3f + if (k % 2 == 0) 9f else -9f, len * k / 3f))
            }
            // grass
            val grass = Path().apply {
                moveTo(0f, h)
                var x = 0f
                while (x <= w) {
                    lineTo(x + 8f, h - 30f - 14f * sin(t * 2f + x / 40f))
                    lineTo(x + 16f, h)
                    x += 16f
                }
                close()
            }
            drawPath(grass, Color(0xFF558B2F).copy(alpha = 0.85f))
            // fireflies
            repeat(10) { i ->
                val fx = (i * 0.137f + sin(t * 0.4f + i) * 0.05f) % 1f
                val fy = 0.25f + (i * 0.071f) % 0.5f + cos(t * 0.7f + i) * 0.03f
                drawCircle(Color(0xFFFFF59D).copy(alpha = (0.4f + 0.4f * sin(t * 3f + i)).coerceIn(0f, 1f)), 5f, Offset(w * fx, h * fy))
            }
        }
        Decor.CITY -> {
            // skyline silhouettes with lit windows
            var x = 0f
            var i = 0
            while (x < w) {
                val bw = w * (0.08f + (i % 4) * 0.025f)
                val bh = h * (0.12f + (i * 37 % 5) * 0.035f)
                drawRect(Color(0xFF1A237E).copy(alpha = 0.8f), Offset(x, h - bh), Size(bw - 4f, bh))
                for (wy in 0 until (bh / 22f).toInt()) for (wx in 0 until 2) {
                    if ((i + wy + wx) % 3 != 0) drawRect(Color(0xFFFFF176).copy(alpha = 0.7f), Offset(x + 6f + wx * bw * 0.42f, h - bh + 8f + wy * 22f), Size(bw * 0.22f, 10f))
                }
                x += bw
                i++
            }
            // comic halftone corners + search lights
            for (gx in 0 until 6) for (gy in 0 until 6) drawCircle(theme.accent.copy(alpha = 0.25f), (6 - gx - gy).coerceAtLeast(0) * 1.6f, Offset(gx * 22f + 10f, gy * 22f + 10f))
            val beam = sin(t * 0.6f) * 0.3f
            val beamPath = Path().apply { moveTo(w * 0.15f, h); lineTo(w * (0.35f + beam), 0f); lineTo(w * (0.45f + beam), 0f); close() }
            drawPath(beamPath, Color.White.copy(alpha = 0.08f))
        }
        Decor.SPACE -> {
            drawRect(Color(0xFF0D1B3E).copy(alpha = 0.25f))
            repeat(40) { i ->
                val sx = (i * 0.1618f) % 1f
                val sy = (i * 0.3819f) % 1f
                drawCircle(Color.White.copy(alpha = (0.3f + 0.6f * sin(t * 2f + i)).coerceIn(0f, 1f)), 2.5f, Offset(w * sx, h * sy))
            }
            val p = Offset(w * 0.88f, h * 0.12f)
            drawCircle(Color(0xFFFF8A65), w * 0.08f, p)
            drawOval(Color(0xFFFFCC80), topLeft = p - Offset(w * 0.13f, w * 0.025f), size = Size(w * 0.26f, w * 0.05f), style = Stroke(5f))
        }
        Decor.OCEAN -> {
            drawRect(Color(0xFF0277BD).copy(alpha = 0.18f))
            repeat(4) { i ->
                val rx = w * (0.15f + i * 0.22f) + sin(t * 0.5f + i) * 20f
                val ray = Path().apply { moveTo(rx, 0f); lineTo(rx + 60f, 0f); lineTo(rx + 140f, h * 0.7f); lineTo(rx + 40f, h * 0.7f); close() }
                drawPath(ray, Color.White.copy(alpha = 0.07f))
            }
            repeat(14) { i ->
                val by = 1f - ((t * 0.08f + i * 0.071f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.35f), 4f + (i % 4) * 2f, Offset(w * ((i * 0.137f) % 1f) + sin(t + i) * 8f, h * by), style = Stroke(2f))
            }
            for (s in 0 until 6) {
                val x = w * (0.05f + s * 0.18f)
                val weed = Path().apply {
                    moveTo(x, h)
                    quadraticTo(x + 25f * sin(t * 1.3f + s), h * 0.88f, x + 10f * sin(t + s), h * 0.8f)
                }
                drawPath(weed, Color(0xFF2E7D32).copy(alpha = 0.8f), style = Stroke(10f, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.drawCommon(scene: Scene, glyphs: Glyphs, theme: AdventureTheme) {
    val w = size.width
    val h = size.height
    scene.sparks.forEach { s -> drawCircle(s.color.copy(alpha = s.life.coerceIn(0f, 1f)), size.minDimension * 0.012f, Offset(w * s.x, h * s.y)) }
    // magic hands: glowing rings that follow the child's hands
    scene.hands.forEach { hnd ->
        val c = Offset(w * hnd.x, h * hnd.y)
        drawCircle(theme.accent.copy(alpha = 0.25f), size.minDimension * 0.07f, c)
        drawCircle(Color.White.copy(alpha = 0.8f), size.minDimension * 0.045f, c, style = Stroke(5f))
        drawCircle(Color.White, size.minDimension * 0.012f, c)
    }
    scene.floaties.forEach { f -> drawGlyph(glyphs.get(f.text, 26, f.color, bold = true), Offset(w * f.x, h * f.y), f.life.coerceIn(0f, 1f)) }
}

/** Runs the frame clock and the pose feed for a mission; [frame] gets dt in seconds. */
@Composable
private fun MissionRuntime(
    scene: Scene,
    tracker: PoseTracker,
    stage: () -> IntSize,
    running: () -> Boolean,
    onPose: (BodyPose) -> Unit = {},
    frame: (Float) -> Unit,
) {
    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val s = stage()
            if (s == IntSize.Zero) return@collect
            scene.prevHands = scene.hands
            scene.hands = handsOnStage(tracker.pose, s).map { Offset(it.x / s.width, it.y / s.height) }
            val pose = tracker.pose
            val nosePoint = pose?.visible(PoseLandmark.NOSE, 0.4f)
            scene.nose = if (pose != null && nosePoint != null) {
                val o = pose.toScreen(nosePoint, Size(s.width.toFloat(), s.height.toFloat()))
                Offset(o.x / s.width, o.y / s.height)
            } else null
            if (pose != null) onPose(pose)
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        while (running()) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            scene.step(dt)
            frame(dt)
        }
    }
}

private fun near(a: Offset, b: Offset, r: Float, aspect: Float) = hypot(a.x - b.x, (a.y - b.y) * aspect) < r

@Composable
private fun MissionHost(theme: AdventureTheme, stage: Stage, stageIndex: Int, onFinish: (Int, Boolean) -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    val done: (Int, Boolean) -> Unit = { score, passed ->
        if (passed) GameSfx.levelUp() else GameSfx.oops()
        finish(score, passed)
    }
    when (stage.mission) {
        Mission.CATCH -> CatchMission(theme, stageIndex, combo = false, done)
        Mission.SORT -> SortMission(theme, stageIndex, done)
        Mission.TWO_HANDS -> TwoHandsMission(theme, stageIndex, done)
        Mission.DODGE -> DodgeMission(theme, stageIndex, done)
        Mission.RESCUE -> RescueMission(theme, stageIndex, done)
        Mission.MEMORY -> MemoryMission(theme, stageIndex, done)
        Mission.BALANCE -> BalanceMission(theme, done)
        Mission.REACTION -> ReactionMission(theme, stageIndex, done)
        Mission.COMBO -> CatchMission(theme, stageIndex, combo = true, done)
        Mission.BOSS -> BossMission(theme, done)
    }
}

// =====================================================================================================================
// Missions
// =====================================================================================================================

/** CATCH / COMBO: items drift down; touch the good ones. In COMBO bad ones fall too and cost a heart. */
@Composable
private fun CatchMission(theme: AdventureTheme, stageIndex: Int, combo: Boolean, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var got by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var left by remember { mutableFloatStateOf(50f) }
    var over by remember { mutableStateOf(false) }
    val target = if (combo) 12 else 8
    var spawn by remember { mutableFloatStateOf(0f) }

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        left -= dt
        spawn += dt
        if (spawn > (if (combo) 0.75f else 1.0f)) {
            spawn = 0f
            val bad = combo && Random.nextFloat() < 0.35f
            scene.things += Thing(scene.nextId++, Random.nextFloat() * 0.8f + 0.1f, -0.05f, (Random.nextFloat() - 0.5f) * 0.05f, 0.16f + stageIndex * 0.012f, if (bad) theme.bad.random() else theme.good.random(), if (bad) Kind.BAD else Kind.GOOD)
        }
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        for (i in scene.things.indices.reversed()) {
            val t = scene.things[i]
            val n = t.copy(x = t.x + t.vx * dt + sin(scene.time * 2f + t.id) * 0.002f, y = t.y + t.vy * dt)
            val touched = scene.hands.any { near(it, Offset(n.x, n.y), 0.09f, aspect) }
            when {
                touched && n.kind == Kind.GOOD -> { scene.things.removeAt(i); got++; GameSfx.coin(); scene.burst(n.x, n.y, theme.accent); scene.say("+۱", n.x, n.y) }
                touched && n.kind == Kind.BAD -> { scene.things.removeAt(i); hearts--; GameSfx.oops(); scene.burst(n.x, n.y, Color.Red, 8); scene.say("آخ!", n.x, n.y, Color(0xFFFF5252)) }
                n.y > 1.1f -> scene.things.removeAt(i)
                else -> scene.things[i] = n
            }
        }
        if (!over && (got >= target || left <= 0f || hearts <= 0)) {
            over = true
            done(got * 3 + hearts * 2, got >= target)
        }
    }
    PoseStage(tracker, "${if (combo) "🌀" else "✋"} ${got.fa()} از ${target.fa()}   ${if (combo) "❤️".repeat(hearts.coerceAtLeast(0)) + "   " else ""}⏱ ${left.toInt().coerceAtLeast(0).fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        scene.things.forEach { t ->
            val c = Offset(size.width * t.x, size.height * t.y)
            drawCircle((if (t.kind == Kind.BAD) Color.Red else theme.accent).copy(alpha = 0.22f), size.minDimension * 0.06f, c)
            drawGlyph(glyphs.get(t.emoji, 44), c)
        }
        drawCommon(scene, glyphs, theme)
    }
}

/** SORT: coloured items; only the colour asked for counts (the colour changes every few seconds). */
@Composable
private fun SortMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var targetColor by remember { mutableIntStateOf(0) }
    var got by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var left by remember { mutableFloatStateOf(50f) }
    var switchIn by remember { mutableFloatStateOf(10f) }
    var spawn by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    val target = 10
    LaunchedEffect(targetColor) { space.voice.speak("فقط رنگ ${theme.sortColors[targetColor].first}") }

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        left -= dt
        switchIn -= dt
        if (switchIn <= 0f) { switchIn = 10f; targetColor = (targetColor + 1 + Random.nextInt(theme.sortColors.size - 1)) % theme.sortColors.size; GameSfx.magic() }
        spawn += dt
        if (spawn > 0.8f) {
            spawn = 0f
            val col = if (Random.nextFloat() < 0.45f) targetColor else Random.nextInt(theme.sortColors.size)
            scene.things += Thing(scene.nextId++, Random.nextFloat() * 0.8f + 0.1f, -0.05f, 0f, 0.15f + stageIndex * 0.01f, theme.good[col % theme.good.size], Kind.COLOR, col)
        }
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        for (i in scene.things.indices.reversed()) {
            val t = scene.things[i]
            val n = t.copy(y = t.y + t.vy * dt)
            val touched = scene.hands.any { near(it, Offset(n.x, n.y), 0.09f, aspect) }
            when {
                touched && n.color == targetColor -> { scene.things.removeAt(i); got++; GameSfx.coin(); scene.burst(n.x, n.y, theme.sortColors[n.color].second); scene.say("آفرین!", n.x, n.y) }
                touched -> { scene.things.removeAt(i); wrong++; GameSfx.oops(); scene.say("رنگ دیگر!", n.x, n.y, Color(0xFFFF8A80)) }
                n.y > 1.1f -> scene.things.removeAt(i)
                else -> scene.things[i] = n
            }
        }
        if (!over && (got >= target || left <= 0f)) { over = true; done(got * 3 - wrong, got >= target && wrong <= got) }
    }
    PoseStage(tracker, "🎯 فقط ${theme.sortColors[targetColor].first}!   ${got.fa()} از ${target.fa()}   ⏱ ${left.toInt().coerceAtLeast(0).fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        // big colour banner
        drawRoundRect(theme.sortColors[targetColor].second, Offset(size.width * 0.3f, 10f), Size(size.width * 0.4f, 36f), CornerRadius(18f))
        scene.things.forEach { t ->
            val c = Offset(size.width * t.x, size.height * t.y)
            drawCircle(theme.sortColors[t.color].second, size.minDimension * 0.065f, c)
            drawCircle(Color.White.copy(alpha = 0.5f), size.minDimension * 0.065f, c, style = Stroke(4f))
            drawGlyph(glyphs.get(t.emoji, 34), c)
        }
        drawCommon(scene, glyphs, theme)
    }
}

/** TWO_HANDS: a big object appears; hold it with both hands at once for half a second. */
@Composable
private fun TwoHandsMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var item by remember { mutableStateOf<Thing?>(null) }
    var got by remember { mutableIntStateOf(0) }
    var left by remember { mutableFloatStateOf(60f) }
    var over by remember { mutableStateOf(false) }
    val target = 5

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        left -= dt
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        val cur = item
        if (cur == null) {
            item = Thing(scene.nextId++, Random.nextFloat() * 0.5f + 0.25f, Random.nextFloat() * 0.3f + 0.25f, (Random.nextFloat() - 0.5f) * 0.04f * stageIndex, 0f, theme.big, Kind.BIG, life = 1f)
        } else {
            // both hands on the object: two tracked points inside it that are clearly apart (not one hand's wrist+finger)
            val inside = scene.hands.filter { h -> near(h, Offset(cur.x, cur.y), 0.17f, aspect) }
            val both = inside.size >= 2 && (inside.maxOf { it.x } - inside.minOf { it.x }) > 0.09f
            val hold = if (both) cur.hold + dt / 0.5f else (cur.hold - dt).coerceAtLeast(0f)
            var nx = cur.x + cur.vx * dt
            var vx = cur.vx
            if (nx < 0.15f || nx > 0.85f) { nx = cur.x; vx = -vx }
            if (hold >= 1f) {
                got++
                GameSfx.powerUp()
                scene.burst(cur.x, cur.y, theme.accent, 24)
                scene.say("قهرمان! 💪", cur.x, cur.y)
                item = null
            } else item = cur.copy(x = nx, vx = vx, hold = hold)
        }
        if (!over && (got >= target || left <= 0f)) { over = true; done(got * 5, got >= target) }
    }
    PoseStage(tracker, "🙌 با هر دو دست بگیر!   ${got.fa()} از ${target.fa()}   ⏱ ${left.toInt().coerceAtLeast(0).fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        item?.let { t ->
            val c = Offset(size.width * t.x, size.height * t.y)
            val r = size.minDimension * 0.15f
            drawCircle(theme.accent.copy(alpha = 0.18f + 0.1f * sin(scene.time * 6f)), r * 1.2f, c)
            drawArc(theme.accent, -90f, 360f * t.hold, false, topLeft = c - Offset(r * 1.25f, r * 1.25f), size = Size(r * 2.5f, r * 2.5f), style = Stroke(12f, cap = StrokeCap.Round))
            drawGlyph(glyphs.get(t.emoji, 76), c)
            drawGlyph(glyphs.get("✋", 30), c + Offset(-r * 1.25f, 0f), 0.7f)
            drawGlyph(glyphs.get("🤚", 30), c + Offset(r * 1.25f, 0f), 0.7f)
        }
        drawCommon(scene, glyphs, theme)
    }
}

/** DODGE: obstacles fly at you – low ones (duck!) and side ones (step to the free side). */
@Composable
private fun DodgeMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var kind by remember { mutableIntStateOf(0) } // 0 low (duck), 1 left blocked, 2 right blocked
    var approach by remember { mutableFloatStateOf(0f) }
    var passed by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var standY by remember { mutableFloatStateOf(-1f) }
    var flash by remember { mutableStateOf<Boolean?>(null) }
    var flashFor by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    val total = 8
    val travel = (3.4f - stageIndex * 0.12f).coerceAtLeast(2.2f)

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        val nose = scene.nose
        if (nose != null && approach < 0.3f) standY = if (standY < 0f) nose.y else standY * 0.95f + nose.y * 0.05f
        if (flashFor > 0f) { flashFor -= dt; if (flashFor <= 0f) flash = null; return@MissionRuntime }
        approach += dt / travel
        if (approach >= 1f) {
            val ok = nose != null && when (kind) {
                0 -> standY > 0f && nose.y > standY + 0.1f
                1 -> nose.x > 0.55f
                else -> nose.x < 0.45f
            }
            if (ok) { passed++; GameSfx.whoosh(); scene.say("عالی!", 0.5f, 0.4f) } else { GameSfx.hit(); scene.say("اوه!", 0.5f, 0.4f, Color(0xFFFF5252)) }
            flash = ok
            flashFor = 0.7f
            approach = 0f
            count++
            kind = if (Random.nextFloat() < 0.4f) 0 else 1 + Random.nextInt(2)
            if (count >= total && !over) { over = true; done(passed * 4, passed >= 6) }
        }
    }
    val hint = when (kind) { 0 -> "⬇️ خم شو!"; 1 -> "➡️ برو سمت راست!"; else -> "⬅️ برو سمت چپ!" }
    PoseStage(tracker, "🤸 $hint   رد شده: ${passed.fa()} از ${total.fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        val w = size.width
        val h = size.height
        val scale = 0.25f + approach * 0.9f
        val col = when (flash) { true -> Color(0xFF66BB6A); false -> Color(0xFFE53935); null -> Color(0xFF8D6E63) }
        when (kind) {
            0 -> {
                val y = h * (0.15f + 0.1f * approach)
                drawRoundRect(col.copy(alpha = 0.55f + approach * 0.35f), Offset(w * (0.5f - scale / 2), y), Size(w * scale, h * 0.12f * scale + 10f), CornerRadius(30f))
                repeat(5) { i -> drawGlyph(glyphs.get(theme.lowObstacle, (24 + 40 * approach).toInt()), Offset(w * (0.5f - scale / 2 + scale * (i + 0.5f) / 5f), y + h * 0.06f * scale)) }
            }
            else -> {
                val leftSide = kind == 1
                val x0 = if (leftSide) w * (0.5f - scale / 2) else w * 0.5f
                drawRoundRect(col.copy(alpha = 0.5f + approach * 0.35f), Offset(x0, h * (0.5f - scale / 2)), Size(w * scale / 2, h * scale), CornerRadius(30f))
                drawGlyph(glyphs.get(theme.sideObstacle, (30 + 60 * approach).toInt()), Offset(x0 + w * scale / 4, h * 0.5f))
            }
        }
        drawCommon(scene, glyphs, theme)
    }
}

/** RESCUE: a friend is trapped behind 4 bubbles/knots; pop them all while avoiding the bad ones flying past. */
@Composable
private fun RescueMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val locks = remember { mutableStateListOf<Offset>() }
    var friendAt by remember { mutableStateOf(Offset(0.5f, 0.4f)) }
    var saved by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var left by remember { mutableFloatStateOf(70f) }
    var spawn by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    val target = 3

    fun newFriend() {
        friendAt = Offset(Random.nextFloat() * 0.5f + 0.25f, Random.nextFloat() * 0.25f + 0.3f)
        locks.clear()
        repeat(4) { i ->
            val a = i * PI / 2 + PI / 4
            locks += Offset(friendAt.x + (cos(a) * 0.17f).toFloat(), friendAt.y + (sin(a) * 0.13f).toFloat())
        }
    }
    LaunchedEffect(Unit) { newFriend() }

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        left -= dt
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        for (i in locks.indices.reversed()) {
            if (scene.hands.any { near(it, locks[i], 0.08f, aspect) }) { scene.burst(locks[i].x, locks[i].y, Color.White, 10); GameSfx.coin(); locks.removeAt(i) }
        }
        if (locks.isEmpty() && saved < target) { saved++; GameSfx.cheer(); scene.burst(friendAt.x, friendAt.y, theme.accent, 30); scene.say("نجاتش دادی! 💚", friendAt.x, friendAt.y - 0.1f); if (saved < target) newFriend() }
        spawn += dt
        if (spawn > (1.6f - stageIndex * 0.06f)) {
            spawn = 0f
            val fromLeft = Random.nextBoolean()
            scene.things += Thing(scene.nextId++, if (fromLeft) -0.05f else 1.05f, Random.nextFloat() * 0.5f + 0.2f, if (fromLeft) 0.25f else -0.25f, 0f, theme.bad.random(), Kind.BAD)
        }
        for (i in scene.things.indices.reversed()) {
            val t = scene.things[i]
            val n = t.copy(x = t.x + t.vx * dt, y = t.y + sin(scene.time * 3f + t.id) * 0.002f)
            if (scene.hands.any { near(it, Offset(n.x, n.y), 0.08f, aspect) }) { scene.things.removeAt(i); hearts--; GameSfx.oops(); scene.say("مراقب باش!", n.x, n.y, Color(0xFFFF5252)) }
            else if (n.x < -0.1f || n.x > 1.1f) scene.things.removeAt(i) else scene.things[i] = n
        }
        if (!over && (saved >= target || left <= 0f || hearts <= 0)) { over = true; done(saved * 6 + hearts * 2, saved >= target) }
    }
    PoseStage(tracker, "🆘 نجات‌یافته: ${saved.fa()} از ${target.fa()}   ${"❤️".repeat(hearts.coerceAtLeast(0))}   ⏱ ${left.toInt().coerceAtLeast(0).fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        val c = Offset(size.width * friendAt.x, size.height * friendAt.y)
        if (saved < target) {
            drawGlyph(glyphs.get(theme.friend, 64), c)
            locks.forEach { l ->
                val lc = Offset(size.width * l.x, size.height * l.y)
                drawLine(Color.White.copy(alpha = 0.5f), c, lc, strokeWidth = 4f)
                drawCircle(Color.White.copy(alpha = 0.25f), size.minDimension * 0.055f, lc)
                drawGlyph(glyphs.get(theme.cage, 34), lc)
            }
        }
        scene.things.forEach { t -> drawGlyph(glyphs.get(t.emoji, 42), Offset(size.width * t.x, size.height * t.y)) }
        drawCommon(scene, glyphs, theme)
    }
}

/** MEMORY: four magic pads light up in a sequence; touch them in the same order. */
@Composable
private fun MemoryMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val pads = listOf(Offset(0.2f, 0.25f), Offset(0.8f, 0.25f), Offset(0.2f, 0.62f), Offset(0.8f, 0.62f))
    val padColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF43A047))
    val tones = listOf(60, 64, 67, 72)
    val sequence = remember { mutableStateListOf<Int>() }
    var showing by remember { mutableStateOf(true) }
    var lit by remember { mutableIntStateOf(-1) }
    var input by remember { mutableIntStateOf(0) }
    var rounds by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var cooldown by remember { mutableFloatStateOf(0f) }
    var round by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    val target = 3

    LaunchedEffect(round) {
        if (over) return@LaunchedEffect
        showing = true
        input = 0
        sequence.clear()
        repeat(3 + rounds + stageIndex / 4) { sequence += Random.nextInt(4) }
        delay(900)
        for (p in sequence) {
            lit = p
            GameSfx.beep(high = tones[p] > 65)
            delay(650)
            lit = -1
            delay(250)
        }
        showing = false
    }

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        if (showing || over) return@MissionRuntime
        if (cooldown > 0f) { cooldown -= dt; return@MissionRuntime }
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        val touched = pads.indexOfFirst { p -> scene.hands.any { near(it, p, 0.12f, aspect) } }
        if (touched >= 0) {
            cooldown = 0.6f
            lit = touched
            if (sequence.getOrNull(input) == touched) {
                GameSfx.beep(high = tones[touched] > 65)
                input++
                if (input >= sequence.size) {
                    rounds++
                    GameSfx.levelUp()
                    scene.burst(0.5f, 0.45f, theme.accent, 30)
                    scene.say("حافظه‌ی قهرمان! 🧠", 0.5f, 0.45f)
                    if (rounds >= target) { over = true; done(rounds * 6 - mistakes, true) } else round++
                }
            } else {
                mistakes++
                GameSfx.oops()
                scene.say("دوباره ببین 👀", 0.5f, 0.45f, Color(0xFFFF8A80))
                if (mistakes >= 3) { over = true; done(rounds * 6, false) } else round++
            }
        } else if (cooldown <= 0f) lit = -1
    }
    PoseStage(tracker, if (showing) "🧠 خوب نگاه کن…" else "🧠 حالا تو! ${input.fa()} از ${sequence.size.fa()}   دور ${(rounds + 1).coerceAtMost(target).fa()} از ${target.fa()}   ❌ ${mistakes.fa()} از ۳", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        pads.forEachIndexed { i, p ->
            val c = Offset(size.width * p.x, size.height * p.y)
            val r = size.minDimension * 0.11f
            val on = lit == i
            drawCircle(padColors[i].copy(alpha = if (on) 0.95f else 0.35f), r * if (on) 1.15f else 1f, c)
            drawCircle(Color.White.copy(alpha = if (on) 0.9f else 0.4f), r * if (on) 1.15f else 1f, c, style = Stroke(6f))
            drawGlyph(glyphs.get(theme.good[i % theme.good.size], 36), c)
        }
        drawCommon(scene, glyphs, theme)
    }
}

/** BALANCE: hold three poses (arms wide, one hand up, both hands up) still for three seconds each. */
@Composable
private fun BalanceMission(theme: AdventureTheme, done: (Int, Boolean) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val poses = listOf("دست‌ها را مثل هواپیما باز کن ✈️", "یک دستت را بالا ببر ☝️", "هر دو دست را بالا ببر 🙌")
    var index by remember { mutableIntStateOf(0) }
    var hold by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var left by remember { mutableFloatStateOf(60f) }
    var over by remember { mutableStateOf(false) }
    LaunchedEffect(index) { if (index < poses.size) space.voice.speak(poses[index].dropLast(3)) }

    fun matches(pose: BodyPose): Boolean {
        val nose = pose.visible(PoseLandmark.NOSE, 0.4f) ?: return false
        val lw = pose.visible(PoseLandmark.LEFT_WRIST, 0.4f) ?: return false
        val rw = pose.visible(PoseLandmark.RIGHT_WRIST, 0.4f) ?: return false
        val ls = pose.visible(PoseLandmark.LEFT_SHOULDER, 0.4f) ?: return false
        val rs = pose.visible(PoseLandmark.RIGHT_SHOULDER, 0.4f) ?: return false
        val shoulderW = abs(ls.x - rs.x)
        return when (index) {
            0 -> abs(lw.x - rw.x) > shoulderW * 2.4f && abs(lw.y - ls.y) < 0.12f && abs(rw.y - rs.y) < 0.12f
            1 -> (lw.y < nose.y) xor (rw.y < nose.y)
            else -> lw.y < nose.y && rw.y < nose.y
        }
    }

    MissionRuntime(scene, tracker, { stageSize }, { !over }, onPose = { holding = index < poses.size && matches(it) }) { dt ->
        left -= dt
        if (over) return@MissionRuntime
        hold = if (holding) hold + dt / 3f else (hold - dt * 0.6f).coerceAtLeast(0f)
        if (hold >= 1f) {
            hold = 0f
            index++
            GameSfx.powerUp()
            scene.burst(0.5f, 0.4f, theme.accent, 28)
            scene.say("ثابت و قوی! ⭐", 0.5f, 0.35f)
        }
        if (index >= poses.size || left <= 0f) { over = true; done(index * 7, index >= poses.size) }
    }
    PoseStage(tracker, "🧘 ${poses.getOrElse(index) { "عالی بود!" }}   ${index.fa()} از ${poses.size.fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        val c = Offset(size.width * 0.5f, size.height * 0.12f)
        drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(size.width * 0.2f, c.y - 16f), Size(size.width * 0.6f, 32f), CornerRadius(16f))
        drawRoundRect(if (holding) Color(0xFF66BB6A) else Color(0xFFFFCA28), Offset(size.width * 0.2f, c.y - 16f), Size(size.width * 0.6f * hold.coerceIn(0f, 1f), 32f), CornerRadius(16f))
        // wobbling bridge at the bottom
        val wob = if (holding) 0f else sin(scene.time * 8f) * 12f
        drawLine(Color(0xFF8D6E63), Offset(size.width * 0.1f, size.height * 0.92f + wob), Offset(size.width * 0.9f, size.height * 0.92f - wob), strokeWidth = 16f, cap = StrokeCap.Round)
        drawCommon(scene, glyphs, theme)
    }
}

/** REACTION: sudden events – shield (both hands up), dodge to a side, or catch something – within a short window. */
@Composable
private fun ReactionMission(theme: AdventureTheme, stageIndex: Int, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var event by remember { mutableIntStateOf(-1) } // 0 shield, 1 dodge left, 2 dodge right, 3 catch
    var window by remember { mutableFloatStateOf(0f) }
    var wait by remember { mutableFloatStateOf(1.5f) }
    var catchAt by remember { mutableStateOf(Offset(0.5f, 0.4f)) }
    var good by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var shieldUp by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    val total = 10
    val reactTime = (2.2f - stageIndex * 0.08f).coerceAtLeast(1.3f)

    MissionRuntime(scene, tracker, { stageSize }, { !over }, onPose = { pose ->
        val nose = pose.visible(PoseLandmark.NOSE, 0.4f)
        val lw = pose.visible(PoseLandmark.LEFT_WRIST, 0.4f)
        val rw = pose.visible(PoseLandmark.RIGHT_WRIST, 0.4f)
        shieldUp = nose != null && lw != null && rw != null && lw.y < nose.y && rw.y < nose.y
    }) { dt ->
        if (over) return@MissionRuntime
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        if (event < 0) {
            wait -= dt
            if (wait <= 0f) {
                event = Random.nextInt(4)
                window = reactTime
                catchAt = Offset(Random.nextFloat() * 0.6f + 0.2f, Random.nextFloat() * 0.35f + 0.2f)
                if (event == 0) GameSfx.boss() else GameSfx.beep(high = true)
            }
            return@MissionRuntime
        }
        window -= dt
        val nose = scene.nose
        val success = when (event) {
            0 -> shieldUp
            1 -> nose != null && nose.x < 0.35f
            2 -> nose != null && nose.x > 0.65f
            else -> scene.hands.any { near(it, catchAt, 0.1f, aspect) }
        }
        if (success || window <= 0f) {
            count++
            if (success) { good++; GameSfx.sparkle(); scene.burst(0.5f, 0.45f, theme.accent, 16); scene.say("سریع! ⚡", 0.5f, 0.45f) } else { GameSfx.oops(); scene.say("دیر شد!", 0.5f, 0.45f, Color(0xFFFF8A80)) }
            event = -1
            wait = Random.nextFloat() * 1.2f + 0.8f
            if (count >= total) { over = true; done(good * 4, good >= 8) }
        }
    }
    val label = when (event) { 0 -> "🛡️ سپر! دست‌ها بالا!"; 1 -> "⬅️ جاخالی به چپ!"; 2 -> "➡️ جاخالی به راست!"; 3 -> "✋ بگیرش!"; else -> "آماده باش…" }
    PoseStage(tracker, "⚡ $label   درست: ${good.fa()} از ${count.fa()}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        val w = size.width
        val h = size.height
        when (event) {
            0 -> {
                drawGlyph(glyphs.get(theme.bad.first(), 80), Offset(w * 0.5f, h * (0.1f + 0.3f * (1f - window / reactTime))))
                if (shieldUp) drawArc(Color(0xFF40C4FF).copy(alpha = 0.6f), 180f, 180f, false, topLeft = Offset(w * 0.2f, h * 0.15f), size = Size(w * 0.6f, h * 0.5f), style = Stroke(14f))
            }
            1, 2 -> {
                val x = if (event == 1) w * 0.75f else w * 0.25f
                drawRoundRect(Color(0xFFE53935).copy(alpha = 0.35f), Offset(x - w * 0.22f, 0f), Size(w * 0.44f, h), CornerRadius(20f))
                drawGlyph(glyphs.get(theme.sideObstacle, 70), Offset(x, h * 0.5f))
            }
            3 -> {
                val c = Offset(w * catchAt.x, h * catchAt.y)
                drawCircle(theme.accent.copy(alpha = 0.3f), size.minDimension * 0.08f * (1f + 0.2f * sin(scene.time * 10f)), c)
                drawGlyph(glyphs.get(theme.good.first(), 52), c)
            }
        }
        if (event >= 0) drawRect(Color.White.copy(alpha = 0.8f), Offset(0f, h - 10f), Size(w * (window / reactTime).coerceIn(0f, 1f), 10f))
        drawCommon(scene, glyphs, theme)
    }
}

/** BOSS: hit the glowing weak spots with fast punches; dodge the projectiles aimed at you. */
@Composable
private fun BossMission(theme: AdventureTheme, done: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val scene = remember { Scene() }
    val glyphs = rememberGlyphs()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var bossX by remember { mutableFloatStateOf(0.5f) }
    var hp by remember { mutableIntStateOf(14) }
    var hearts by remember { mutableIntStateOf(4) }
    var spot by remember { mutableStateOf<Offset?>(null) }
    var spotLife by remember { mutableFloatStateOf(0f) }
    var shootIn by remember { mutableFloatStateOf(2.5f) }
    var hitFlash by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    val maxHp = 14
    LaunchedEffect(Unit) { GameSfx.boss() }

    MissionRuntime(scene, tracker, { stageSize }, { !over }) { dt ->
        if (over) return@MissionRuntime
        val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
        bossX = 0.5f + sin(scene.time * 0.7f) * 0.25f
        hitFlash = (hitFlash - dt * 3f).coerceAtLeast(0f)
        // weak spot on the boss
        if (spot == null) { spot = Offset(bossX + (Random.nextFloat() - 0.5f) * 0.18f, 0.22f + Random.nextFloat() * 0.12f); spotLife = 1.8f }
        spot?.let { s ->
            val moving = Offset(s.x + (bossX - 0.5f) * 0.02f, s.y)
            spotLife -= dt
            if (scene.fastHands(0.025f).any { near(it, moving, 0.1f, aspect) }) {
                hp--
                hitFlash = 1f
                GameSfx.hit()
                scene.burst(moving.x, moving.y, theme.accent, 18)
                scene.say("بوم! 💥", moving.x, moving.y)
                spot = null
            } else if (spotLife <= 0f) spot = null else spot = moving
        }
        // projectiles aimed at where the child stands
        shootIn -= dt
        if (shootIn <= 0f) {
            shootIn = Random.nextFloat() * 1.2f + 1.4f
            val aimX = (scene.nose?.x ?: 0.5f)
            scene.things += Thing(scene.nextId++, bossX, 0.35f, (aimX - bossX) * 0.5f, 0.45f, theme.bad.random(), Kind.BAD)
            GameSfx.whoosh()
        }
        for (i in scene.things.indices.reversed()) {
            val t = scene.things[i]
            val n = t.copy(x = t.x + t.vx * dt, y = t.y + t.vy * dt)
            val nose = scene.nose
            if (n.y > 0.8f && nose != null && abs(n.x - nose.x) < 0.1f) { scene.things.removeAt(i); hearts--; GameSfx.oops(); scene.say("جاخالی بده!", n.x, 0.7f, Color(0xFFFF5252)) }
            else if (n.y > 1.1f) scene.things.removeAt(i) else scene.things[i] = n
        }
        if (hp <= 0 || hearts <= 0) {
            over = true
            if (hp <= 0) GameSfx.cheer()
            done((maxHp - hp) * 3 + hearts * 4, hp <= 0)
        }
    }
    PoseStage(tracker, "👑 ${theme.bossName}   ${"❤️".repeat(hearts.coerceAtLeast(0))}", onSize = { stageSize = it }) {
        drawDecor(theme, scene.time)
        val w = size.width
        val h = size.height
        // boss health bar
        drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(w * 0.15f, 12f), Size(w * 0.7f, 22f), CornerRadius(11f))
        drawRoundRect(Color(0xFFE53935), Offset(w * 0.15f, 12f), Size(w * 0.7f * hp / maxHp.toFloat(), 22f), CornerRadius(11f))
        val bc = Offset(w * bossX, h * 0.28f)
        drawCircle(Color(0xFF4A148C).copy(alpha = 0.35f + 0.15f * sin(scene.time * 3f)), size.minDimension * 0.22f, bc)
        drawGlyph(glyphs.get(theme.boss, 120), bc + Offset(if (hitFlash > 0f) sin(scene.time * 60f) * 12f else 0f, 0f))
        spot?.let { s ->
            val c = Offset(w * s.x, h * s.y)
            drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.6f), size.minDimension * 0.06f * (1f + 0.3f * sin(scene.time * 12f)), c)
            drawCircle(Color.White, size.minDimension * 0.025f, c)
        }
        scene.things.forEach { t -> drawGlyph(glyphs.get(t.emoji, 46), Offset(w * t.x, h * t.y)) }
        drawCommon(scene, glyphs, theme)
    }
}
