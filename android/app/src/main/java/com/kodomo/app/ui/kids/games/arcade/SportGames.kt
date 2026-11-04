package com.kodomo.app.ui.kids.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.VoicePhrases
import com.kodomo.app.ui.fx.ConfettiBurst
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.games.pose.PoseCamera
import com.kodomo.app.ui.kids.games.pose.PoseTracker
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/** Kid-friendly ping pong against a gentle robot: drag the bottom paddle; first to five points wins. */
@Composable
fun PingPongGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    var area by remember { mutableStateOf(IntSize.Zero) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var robotX by remember { mutableFloatStateOf(0.5f) }
    var ball by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var velocity by remember { mutableStateOf(Offset(0.25f, 0.45f)) }
    var you by remember { mutableIntStateOf(0) }
    var robot by remember { mutableIntStateOf(0) }
    var celebrate by remember { mutableIntStateOf(0) }
    val trail = remember { mutableStateListOf<Offset>() }
    val finish by rememberUpdatedState(onFinish)
    val paddleW = 0.26f
    val target = 5

    LaunchedEffect(area) {
        if (area == IntSize.Zero) return@LaunchedEffect
        var last = withFrameNanos { it }
        var speed = 1f
        while (you < target && robot < target) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceAtMost(0.033f)
            last = now
            // robot follows the ball, deliberately a bit slow so children can win
            val follow = (ball.x - robotX).coerceIn(-0.55f * dt, 0.55f * dt)
            robotX = (robotX + follow).coerceIn(paddleW / 2, 1 - paddleW / 2)
            var next = ball + velocity * (dt * speed)
            var v = velocity
            if (next.x < 0.03f || next.x > 0.97f) {
                v = v.copy(x = -v.x)
                next = next.copy(x = next.x.coerceIn(0.03f, 0.97f))
            }
            val padY = 0.9f
            val robotY = 0.1f
            if (v.y > 0 && next.y >= padY && ball.y < padY && abs(next.x - playerX) < paddleW / 2 + 0.03f) {
                val english = (next.x - playerX) / (paddleW / 2)
                v = Offset((v.x + english * 0.25f).coerceIn(-0.6f, 0.6f), -abs(v.y))
                speed = (speed + 0.06f).coerceAtMost(2.2f)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            if (v.y < 0 && next.y <= robotY && ball.y > robotY && abs(next.x - robotX) < paddleW / 2 + 0.03f) {
                v = Offset((v.x + Random.nextFloat() * 0.3f - 0.15f).coerceIn(-0.6f, 0.6f), abs(v.y))
            }
            if (next.y > 1.03f || next.y < -0.03f) {
                val playerScored = next.y < 0
                if (playerScored) {
                    you++
                    celebrate++
                    space.voice.speak(if (you >= target) VoicePhrases.YOU_WIN else VoicePhrases.PONG_POINT)
                } else {
                    robot++
                    space.voice.speak(if (robot >= target) VoicePhrases.NICE_TRY else VoicePhrases.PONG_ROBOT_POINT)
                }
                next = Offset(0.5f, 0.5f)
                // serve towards the player who just lost the point
                v = Offset((Random.nextFloat() - 0.5f) * 0.5f, if (playerScored) -0.45f else 0.45f)
                speed = 1f
                trail.clear()
                delay(900)
                last = withFrameNanos { it }
            }
            velocity = v
            ball = next
            trail.add(0, next)
            if (trail.size > 10) trail.removeAt(trail.lastIndex)
        }
        delay(1_200)
        finish(you * 4 + if (you >= target) 10 else 0)
    }

    Column(Modifier.fillMaxSize()) {
        Text("🙂 تو ${you.fa()}  —  ${robot.fa()} ربات 🤖", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2E7D5B), Color(0xFF1F5C42))))
                .onSizeChanged { area = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        playerX = (change.position.x / size.width).coerceIn(paddleW / 2, 1 - paddleW / 2)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(Color.White.copy(alpha = 0.7f), topLeft = Offset(8f, 8f), size = Size(w - 16f, h - 16f), cornerRadius = CornerRadius(40f), style = Stroke(6f))
                drawLine(Color.White.copy(alpha = 0.8f), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f)))
                // paddles
                drawRoundRect(Color(0xFFFF6B6B), topLeft = Offset((robotX - paddleW / 2) * w, h * 0.1f - 18f), size = Size(paddleW * w, 22f), cornerRadius = CornerRadius(12f))
                drawRoundRect(Color(0xFFFFC83D), topLeft = Offset((playerX - paddleW / 2) * w, h * 0.9f - 4f), size = Size(paddleW * w, 22f), cornerRadius = CornerRadius(12f))
                val robotFace = measurer.measure("🤖", TextStyle(fontSize = 28.sp))
                drawText(robotFace, topLeft = Offset(robotX * w - robotFace.size.width / 2f, h * 0.1f - 18f - robotFace.size.height))
                // ball trail + ball
                trail.forEachIndexed { i, p ->
                    drawCircle(Color.White.copy(alpha = 0.25f * (1f - i / 10f)), 18f * (1f - i / 14f), Offset(p.x * w, p.y * h))
                }
                drawCircle(Color.White, 18f, Offset(ball.x * w, ball.y * h))
                drawCircle(Color(0xFFFFE6A7), 8f, Offset(ball.x * w - 5f, ball.y * h - 5f))
            }
            Text("انگشتت را روی صفحه بکش 👆", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), style = MaterialTheme.typography.labelMedium)
            if (celebrate > 0) ConfettiBurst(celebrate)
        }
    }
}

/** Penalty shoot-out: swipe up from the ball; the goalkeeper dives (sometimes the wrong way). Five shots. */
@Composable
fun PenaltyGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val haptics = LocalHapticFeedback.current
    var area by remember { mutableStateOf(IntSize.Zero) }
    var shots by remember { mutableIntStateOf(0) }
    var goals by remember { mutableIntStateOf(0) }
    var ball by remember { mutableStateOf(Offset(0.5f, 0.86f)) }
    var ballScale by remember { mutableFloatStateOf(1f) }
    var keeperX by remember { mutableFloatStateOf(0.5f) }
    var flying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("انگشتت را از روی توپ به سمت دروازه بکش!") }
    var celebrate by remember { mutableIntStateOf(0) }
    var shot by remember { mutableStateOf<Offset?>(null) }
    val measurer = rememberTextMeasurer()
    val finish by rememberUpdatedState(onFinish)
    val totalShots = 5

    // keeper idles left-right
    LaunchedEffect(Unit) {
        var t = 0f
        while (true) {
            delay(16)
            if (!flying) {
                t += 0.03f
                keeperX = 0.5f + kotlin.math.sin(t) * 0.18f
            }
        }
    }
    LaunchedEffect(shot) {
        val aim = shot ?: return@LaunchedEffect
        flying = true
        val dive = if (Random.nextFloat() < 0.55f) aim.x else Random.nextFloat() * 0.6f + 0.2f
        val start = ball
        val steps = 32
        for (i in 1..steps) {
            val k = i / steps.toFloat()
            ball = Offset(start.x + (aim.x - start.x) * k, start.y + (aim.y - start.y) * k)
            ballScale = 1f - 0.45f * k
            keeperX += (dive - keeperX) * 0.12f
            delay(16)
        }
        val inGoal = aim.x in 0.18f..0.82f && aim.y in 0.12f..0.36f
        val saved = abs(keeperX - aim.x) < 0.13f
        shots++
        if (inGoal && !saved) {
            goals++
            celebrate++
            message = VoicePhrases.GOAL
            space.voice.speak(VoicePhrases.GOAL)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            message = if (!inGoal) "بیرون رفت! کمی دقیق‌تر 🎯" else VoicePhrases.SAVED_BY_KEEPER
            if (inGoal) space.voice.speak(VoicePhrases.SAVED_BY_KEEPER)
        }
        delay(1_300)
        ball = Offset(0.5f, 0.86f)
        ballScale = 1f
        flying = false
        shot = null
        if (shots >= totalShots) {
            delay(500)
            finish(goals * 5 + if (goals == totalShots) 10 else 0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("⚽ گل: ${goals.fa()} از ${shots.fa()}   (ضربه ${(shots + 1).coerceAtMost(totalShots).fa()} از ${totalShots.fa()})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF9ED8FF), Color(0xFF6CC56C), Color(0xFF3E9E4A))))
                .onSizeChanged { area = it }
                .pointerInput(Unit) {
                    var start = Offset.Zero
                    var end = Offset.Zero
                    detectDragGestures(
                        onDragStart = { start = it; end = it },
                        onDrag = { change, _ -> change.consume(); end = change.position },
                        onDragEnd = {
                            if (!flying && shots < totalShots) {
                                val d = end - start
                                if (d.y < -40f) {
                                    // project the swipe onto the goal line
                                    val power = (hypot(d.x, d.y) / size.height).coerceIn(0.2f, 1f)
                                    val targetX = (0.5f + d.x / size.width * 1.6f).coerceIn(-0.1f, 1.1f)
                                    val targetY = (0.24f + (0.6f - power) * 0.35f).coerceIn(0.05f, 0.5f)
                                    shot = Offset(targetX, targetY)
                                }
                            }
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // goal frame + net
                val gl = w * 0.18f
                val gr = w * 0.82f
                val gt = h * 0.12f
                val gb = h * 0.36f
                for (i in 0..12) {
                    val x = gl + (gr - gl) * i / 12f
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(x, gt), Offset(x, gb), 2f)
                }
                for (i in 0..6) {
                    val y = gt + (gb - gt) * i / 6f
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(gl, y), Offset(gr, y), 2f)
                }
                drawLine(Color.White, Offset(gl, gb), Offset(gl, gt), 14f)
                drawLine(Color.White, Offset(gl, gt), Offset(gr, gt), 14f)
                drawLine(Color.White, Offset(gr, gt), Offset(gr, gb), 14f)
                // penalty spot & box
                drawRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(w * 0.08f, gb), size = Size(w * 0.84f, h * 0.32f), style = Stroke(5f))
                drawCircle(Color.White, 8f, Offset(w * 0.5f, h * 0.86f))
                // keeper
                val keeper = measurer.measure("🧤", TextStyle(fontSize = 44.sp))
                val body = measurer.measure("🧍", TextStyle(fontSize = 70.sp))
                drawText(body, topLeft = Offset(keeperX * w - body.size.width / 2f, gb - body.size.height))
                drawText(keeper, topLeft = Offset(keeperX * w - keeper.size.width / 2f, gb - body.size.height * 0.75f))
                // ball
                val ballText = measurer.measure("⚽", TextStyle(fontSize = (52 * ballScale).sp))
                drawText(ballText, topLeft = Offset(ball.x * w - ballText.size.width / 2f, ball.y * h - ballText.size.height / 2f))
            }
            if (celebrate > 0) ConfettiBurst(celebrate)
        }
    }
}

/** Camera goalkeeper: balls fly at the screen; touch them with your hands before they arrive. Ten balls. */
@Composable
fun GoalkeeperGame(onFinish: (Int) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val measurer = rememberTextMeasurer()
    var stage by remember { mutableStateOf(IntSize.Zero) }
    var saves by remember { mutableIntStateOf(0) }
    var conceded by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var celebrate by remember { mutableIntStateOf(0) }
    val finish by rememberUpdatedState(onFinish)
    val totalBalls = 10

    LaunchedEffect(Unit) {
        delay(2_000)
        repeat(totalBalls) {
            target = Offset(Random.nextFloat() * 0.7f + 0.15f, Random.nextFloat() * 0.55f + 0.2f)
            progress = 0f
            var saved = false
            val duration = 2_600L - it * 120L
            val start = System.currentTimeMillis()
            while (progress < 1f && !saved) {
                delay(30)
                progress = ((System.currentTimeMillis() - start) / duration.toFloat()).coerceAtMost(1f)
                if (progress > 0.55f && stage != IntSize.Zero) {
                    val pose = tracker.pose
                    val size = Size(stage.width.toFloat(), stage.height.toFloat())
                    val ballPos = Offset(target.x * size.width, target.y * size.height)
                    val radius = size.minDimension * (0.05f + 0.1f * progress)
                    saved = pose?.hands?.any { h -> val p = pose.toScreen(h, size); hypot(p.x - ballPos.x, p.y - ballPos.y) < radius * 1.4f } == true
                }
            }
            if (saved) {
                saves++
                celebrate++
                space.voice.speak(VoicePhrases.GREAT_SAVE)
            } else {
                conceded++
            }
            progress = 0f
            delay(700)
        }
        finish(saves * 3 + if (saves >= 8) 10 else 0)
    }

    Column(Modifier.fillMaxSize()) {
        Text("🧤 مهار: ${saves.fa()} • گل خورده: ${conceded.fa()}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(12.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(12.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black).onSizeChanged { stage = it },
        ) {
            PoseCamera(tracker, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                if (progress > 0f) {
                    val c = Offset(target.x * size.width, target.y * size.height)
                    val from = Offset(size.width / 2, size.height * 0.05f)
                    val pos = from + (c - from) * progress
                    val layout = measurer.measure("⚽", TextStyle(fontSize = (18 + 70 * progress).sp))
                    drawCircle(Color.Black.copy(alpha = 0.25f * progress), size.minDimension * 0.06f * progress, c + Offset(0f, 30f))
                    drawText(layout, topLeft = pos - Offset(layout.size.width / 2f, layout.size.height / 2f))
                }
            }
            if (celebrate > 0) ConfettiBurst(celebrate)
        }
    }
}

