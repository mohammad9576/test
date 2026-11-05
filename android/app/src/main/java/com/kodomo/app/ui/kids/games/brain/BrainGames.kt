package com.kodomo.app.ui.kids.games.brain

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.Sfx
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/** Shared header: title line, a progress bar towards the level goal, and the time left. */
@Composable
private fun GameHeader(text: String, progress: Float, color: Color = KodomoColors.Green) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = color)
    }
}

@Composable
private fun Countdown(seconds: Int, onTick: (Int) -> Unit, running: () -> Boolean, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        var left = seconds
        while (left > 0 && running()) {
            delay(1_000)
            left--
            onTick(left)
        }
        onDone()
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 1) Memory cards: more pairs, shorter peek and less time as the level rises.
// ---------------------------------------------------------------------------------------------------------------------

private val cardFaces = listOf("🐶", "🐱", "🦊", "🐼", "🐸", "🦁", "🐵", "🐧", "🐢", "🦄", "🐙", "🐝")

@Composable
fun MemoryCardsGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val pairs = (2 + level).coerceAtMost(cardFaces.size)
    val cards = remember { (cardFaces.shuffled().take(pairs) * 2).shuffled() }
    val matched = remember { mutableStateListOf<Int>() }
    val open = remember { mutableStateListOf<Int>() }
    var peeking by remember { mutableStateOf(true) }
    var mistakes by remember { mutableIntStateOf(0) }
    val limit = (40 + pairs * 9 - level * 3).coerceAtLeast(35)
    var left by remember { mutableIntStateOf(limit) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay((3_200L - level * 220L).coerceAtLeast(900L))
        peeking = false
    }
    fun finish() {
        if (done) return
        done = true
        val solved = matched.size == cards.size
        onFinish(matched.size / 2 * 3 + (if (solved) left / 3 else 0) - mistakes / 2, solved)
    }
    Countdown(limit, { left = it }, { !done && matched.size < cards.size }) { finish() }

    val columns = when {
        cards.size <= 6 -> 3
        cards.size <= 16 -> 4
        else -> 5
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        GameHeader("🃏 جفت‌ها: ${(matched.size / 2).fa()} از ${pairs.fa()}   ⏱ ${left.fa()}", matched.size / cards.size.toFloat())
        if (peeking) Text("خوب نگاه کن و به خاطر بسپار! 👀", style = MaterialTheme.typography.bodyLarge)
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.indices.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { i ->
                        val faceUp = peeking || i in open || i in matched
                        val flip by animateFloatAsState(if (faceUp) 0f else 180f, tween(260), label = "flip")
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.8f)
                                .scale(if (i in matched) 0.92f else 1f)
                                .background(
                                    when {
                                        i in matched -> KodomoColors.Mint
                                        flip > 90f -> KodomoColors.Lavender
                                        else -> Color.White
                                    },
                                    RoundedCornerShape(14.dp),
                                )
                                .border(2.dp, if (i in matched) KodomoColors.Green else KodomoColors.Lavender, RoundedCornerShape(14.dp))
                                .clickable(enabled = !peeking && !done && i !in matched && i !in open && open.size < 2) {
                                    open.add(i)
                                    scope.launch { Sfx.pop() }
                                    if (open.size == 2) {
                                        val (a, b) = open[0] to open[1]
                                        scope.launch {
                                            if (cards[a] == cards[b]) {
                                                delay(250)
                                                matched.addAll(listOf(a, b))
                                                open.clear()
                                                if (matched.size == cards.size) {
                                                    Sfx.success()
                                                    finish()
                                                }
                                            } else {
                                                mistakes++
                                                delay((900L - level * 50L).coerceAtLeast(450L))
                                                open.clear()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (flip <= 90f) cards[i] else "❔", fontSize = if (columns >= 5) 26.sp else 34.sp)
                        }
                    }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
        Text("اشتباه‌ها: ${mistakes.fa()}", style = MaterialTheme.typography.bodyMedium)
    }
}

private operator fun <T> List<T>.times(n: Int): List<T> = List(n) { this }.flatten()

// ---------------------------------------------------------------------------------------------------------------------
// 2) Whack-a-mole: faster moles, more at once, more cats (don't tap!) and more holes as the level rises.
// ---------------------------------------------------------------------------------------------------------------------

private data class Mole(val hole: Int, val isCat: Boolean, val until: Long, val id: Long)

@Composable
fun WhackAMoleGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val holes = if (level >= 5) 12 else 9
    val columns = 3
    val target = 10 + level * 3
    val upTime = (1_400L - level * 95L).coerceAtLeast(480L)
    val concurrent = 1 + level / 3
    val catChance = 0.08f + level * 0.03f
    val moles = remember { mutableStateListOf<Mole>() }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(40) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        var id = 0L
        while (!done) {
            val now = System.currentTimeMillis()
            moles.removeAll { it.until < now }
            if (moles.size < concurrent) {
                val free = (0 until holes).filter { h -> moles.none { it.hole == h } }
                if (free.isNotEmpty()) {
                    moles.add(Mole(free.random(), Random.nextFloat() < catChance, now + upTime + Random.nextLong(0, 300), id++))
                }
            }
            delay((520L - level * 30L).coerceAtLeast(160L))
        }
    }
    Countdown(40, { left = it }, { !done }) {
        done = true
        onFinish(hits - misses, hits >= target)
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        GameHeader("🐹 ${hits.fa()} از ${target.fa()}   ⏱ ${left.fa()}", hits / target.toFloat(), KodomoColors.Peach)
        Text("فقط موش‌ها را بزن؛ به گربه 🐱 دست نزن!", style = MaterialTheme.typography.bodyMedium)
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (0 until holes).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { h ->
                        val mole = moles.firstOrNull { it.hole == h }
                        val rise by animateFloatAsState(if (mole != null) 1f else 0f, tween(140), label = "rise")
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Brush.verticalGradient(listOf(Color(0xFF8BC34A), Color(0xFF5D8C2E))), CircleShape)
                                .clickable(enabled = !done) {
                                    val m = moles.firstOrNull { it.hole == h } ?: return@clickable
                                    moles.remove(m)
                                    if (m.isCat) misses += 2 else hits++
                                    scope.launch { Sfx.pop() }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(56.dp).background(Color(0xFF4E342E), CircleShape))
                            if (mole != null) Text(if (mole.isCat) "🐱" else "🐹", fontSize = 46.sp, modifier = Modifier.scale(rise).offset(y = (-10 * rise).dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 3) Fruit catch: faster falling, more bombs, a smaller basket.
// ---------------------------------------------------------------------------------------------------------------------

private data class Falling(val x: Float, val y: Float, val emoji: String, val bomb: Boolean, val speed: Float)

private val fruits = listOf("🍎", "🍌", "🍇", "🍓", "🍊", "🍉", "🍐", "🥝")

@Composable
fun FruitCatchGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val target = 12 + level * 3
    val basketWidth = (0.30f - level * 0.013f).coerceAtLeast(0.17f)
    var basketX by remember { mutableFloatStateOf(0.5f) }
    val items = remember { mutableStateListOf<Falling>() }
    var caught by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var left by remember { mutableIntStateOf(50) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun end() {
        if (done) return
        done = true
        onFinish(caught, caught >= target)
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var sinceSpawn = 0f
        val spawnEvery = (1.0f - level * 0.06f).coerceAtLeast(0.38f)
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                sinceSpawn += dt
                if (sinceSpawn > spawnEvery) {
                    sinceSpawn = 0f
                    val bomb = Random.nextFloat() < 0.10f + level * 0.03f
                    items.add(Falling(Random.nextFloat() * 0.9f + 0.05f, -0.05f, if (bomb) "💣" else fruits.random(), bomb, 0.22f + level * 0.045f + Random.nextFloat() * 0.1f))
                }
                val next = mutableListOf<Falling>()
                items.forEach { f ->
                    val y = f.y + f.speed * dt
                    val inBasket = y > 0.86f && y < 0.95f && kotlin.math.abs(f.x - basketX) < basketWidth / 2
                    when {
                        inBasket && f.bomb -> { lives--; scope.launch { Sfx.pop() } }
                        inBasket -> { caught++; scope.launch { Sfx.pop() } }
                        y > 1.05f -> Unit
                        else -> next.add(f.copy(y = y))
                    }
                }
                items.clear()
                items.addAll(next)
                if (lives <= 0) end()
            }
        }
    }
    Countdown(50, { left = it }, { !done }) { end() }

    Column(Modifier.fillMaxSize()) {
        GameHeader("🧺 ${caught.fa()} از ${target.fa()}   ${"❤️".repeat(lives.coerceAtLeast(0))}   ⏱ ${left.fa()}", caught / target.toFloat(), KodomoColors.Rose)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.verticalGradient(listOf(Color(0xFFBFE3FF), Color(0xFFE8F5EE))))
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { basketX = it.x / size.width }) { change, _ ->
                        basketX = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) { detectTapGestures { basketX = it.x / size.width } },
        ) {
            val w = maxWidth
            val h = maxHeight
            items.forEach { f ->
                com.kodomo.app.ui.fx.Emoji(f.emoji, 41.dp, modifier = Modifier.offset(x = w * f.x - 17.dp, y = h * f.y))
            }
            Box(
                Modifier
                    .offset(x = w * (basketX - basketWidth / 2), y = h * 0.88f)
                    .size(width = w * basketWidth, height = 44.dp)
                    .background(Color(0xFFB07A44), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 6.dp, topEnd = 6.dp)),
                contentAlignment = Alignment.Center,
            ) { com.kodomo.app.ui.fx.Emoji("🧺", 31.dp) }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 4) Flappy robot: faster columns with narrower gaps.
// ---------------------------------------------------------------------------------------------------------------------

private data class Pipe(val x: Float, val gapCenter: Float, val passed: Boolean = false)

@Composable
fun FlappyRobotGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val target = 5 + level * 2
    val gap = (0.40f - level * 0.017f).coerceAtLeast(0.24f)
    val speed = 0.28f + level * 0.03f
    var y by remember { mutableFloatStateOf(0.45f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    val pipes = remember { mutableStateListOf<Pipe>() }
    var passed by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val robotX = 0.25f
    val robotR = 0.035f

    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        var last = 0L
        var distance = 0f
        pipes.add(Pipe(1.1f, Random.nextFloat() * (0.8f - gap) + 0.1f + gap / 2))
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.04f)
                last = now
                velocity += 1.5f * dt
                y += velocity * dt
                distance += speed * dt
                val moved = pipes.map { it.copy(x = it.x - speed * dt) }.filter { it.x > -0.2f }.toMutableList()
                if ((moved.lastOrNull()?.x ?: 0f) < 1.1f - (0.62f - level * 0.015f).coerceAtLeast(0.45f)) {
                    moved.add(Pipe(1.15f, Random.nextFloat() * (0.8f - gap) + 0.1f + gap / 2))
                }
                for (i in moved.indices) {
                    val p = moved[i]
                    if (!p.passed && p.x + 0.07f < robotX) {
                        moved[i] = p.copy(passed = true)
                        passed++
                        scope.launch { Sfx.pop() }
                    }
                    val overlapsX = kotlin.math.abs(p.x - robotX) < 0.07f + robotR
                    val inGap = y > p.gapCenter - gap / 2 + robotR && y < p.gapCenter + gap / 2 - robotR
                    if (overlapsX && !inGap) done = true
                }
                pipes.clear()
                pipes.addAll(moved)
                if (y > 1f || y < 0f) done = true
                if (passed >= target + 10) done = true
            }
        }
        onFinish(passed * 2, passed >= target)
    }

    Column(Modifier.fillMaxSize()) {
        GameHeader("🤖 ستون‌ها: ${passed.fa()} از ${target.fa()}", passed / target.toFloat(), KodomoColors.Sky)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.verticalGradient(listOf(Color(0xFF8EC9FF), Color(0xFFDFF1FF))))
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!started) started = true
                        if (!done) velocity = -0.62f
                    }
                },
        ) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                pipes.forEach { p ->
                    val left = (p.x - 0.07f) * size.width
                    val pw = 0.14f * size.width
                    val top = (p.gapCenter - gap / 2) * size.height
                    val bottom = (p.gapCenter + gap / 2) * size.height
                    drawRoundRect(Color(0xFF43A047), Offset(left, -20f), Size(pw, top + 20f), CornerRadius(18f))
                    drawRoundRect(Color(0xFF43A047), Offset(left, bottom), Size(pw, size.height - bottom + 20f), CornerRadius(18f))
                }
            }
            Text(
                "🤖",
                fontSize = 40.sp,
                modifier = Modifier
                    .offset(x = w * robotX - 20.dp, y = h * y - 24.dp)
                    .rotate((velocity * 40f).coerceIn(-25f, 45f)),
            )
            if (!started) {
                Text(
                    "روی صفحه بزن تا ربات پرواز کند!",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).padding(16.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 5) Slide puzzle: 3×3 with more shuffling, then 4×4 from level 7.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun SlidePuzzleGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val n = if (level >= 7) 4 else 3
    val tiles = remember {
        // Shuffle by random legal moves so the puzzle is always solvable.
        val board = MutableList(n * n) { (it + 1) % (n * n) }
        var empty = n * n - 1
        var previous = -1
        repeat(8 + level * (if (n == 3) 6 else 9)) {
            val r = empty / n
            val c = empty % n
            val options = listOfNotNull(
                if (r > 0) empty - n else null, if (r < n - 1) empty + n else null,
                if (c > 0) empty - 1 else null, if (c < n - 1) empty + 1 else null,
            ).filter { it != previous }
            val pick = options.random()
            board[empty] = board[pick]
            board[pick] = 0
            previous = empty
            empty = pick
        }
        mutableStateListOf<Int>().apply { addAll(board) }
    }
    var moves by remember { mutableIntStateOf(0) }
    val limit = if (n == 3) 150 else 240
    var left by remember { mutableIntStateOf(limit) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun solved() = tiles.indices.all { tiles[it] == (it + 1) % (n * n) }

    Countdown(limit, { left = it }, { !done }) {
        if (!done) {
            done = true
            onFinish(tiles.indices.count { tiles[it] != 0 && tiles[it] == it + 1 }, false)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GameHeader("🧩 حرکت‌ها: ${moves.fa()}   ⏱ ${left.fa()}", 1f - left / limit.toFloat(), KodomoColors.Lavender)
        Text("عددها را به ترتیب از ۱ بچین. روی خانه‌ی کنار جای خالی بزن.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        // Puzzle grid is always laid out left-to-right so the number order reads naturally.
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth().aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until n) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until n) {
                            val i = r * n + c
                            val v = tiles[i]
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(if (v == 0) Color.Transparent else if (v == i + 1) KodomoColors.Green else KodomoColors.Sun, RoundedCornerShape(14.dp))
                                    .clickable(enabled = v != 0 && !done) {
                                        val e = tiles.indexOf(0)
                                        val adjacent = (e / n == r && kotlin.math.abs(e % n - c) == 1) || (e % n == c && kotlin.math.abs(e / n - r) == 1)
                                        if (adjacent) {
                                            tiles[e] = v
                                            tiles[i] = 0
                                            moves++
                                            scope.launch { Sfx.pop() }
                                            if (solved()) {
                                                done = true
                                                scope.launch { Sfx.success() }
                                                onFinish(10 + level * 2 + left / 10, true)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v != 0) Text(v.fa(), color = Color.White, fontSize = if (n == 4) 28.sp else 36.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 6) Colour sequence (memory): longer sequences, faster playback, six pads from level 6.
// ---------------------------------------------------------------------------------------------------------------------

private val padColors = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFB8C00))
private val padEmojis = listOf("🍒", "🍀", "💧", "⭐", "🍇", "🍊")

@Composable
fun ColorSequenceGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val pads = if (level >= 6) 6 else 4
    val goal = 3 + level
    val sequence = remember { mutableStateListOf<Int>() }
    var lit by remember { mutableIntStateOf(-1) }
    var showing by remember { mutableStateOf(true) }
    var inputIndex by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val step = (720L - level * 45L).coerceAtLeast(300L)

    LaunchedEffect(round) {
        if (done) return@LaunchedEffect
        if (sequence.isEmpty()) repeat(2) { sequence.add(Random.nextInt(pads)) } else sequence.add(Random.nextInt(pads))
        showing = true
        inputIndex = 0
        delay(700)
        for (p in sequence) {
            lit = p
            Sfx.pop()
            delay(step)
            lit = -1
            delay(step / 3)
        }
        showing = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val reached = (sequence.size - 1).coerceAtLeast(0)
        GameHeader("🔴 طول دنباله: ${sequence.size.fa()}   هدف: ${goal.fa()}", reached / goal.toFloat(), KodomoColors.Rose)
        Text(if (showing) "خوب نگاه کن… 👀" else "حالا تو همان را بزن! (${(inputIndex).fa()} از ${sequence.size.fa()})", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.fillMaxWidth().aspectRatio(if (pads == 6) 0.67f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until pads).chunked(2).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { p ->
                        val on = lit == p
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .scale(if (on) 1.06f else 1f)
                                .background(padColors[p].copy(alpha = if (on) 1f else 0.45f), RoundedCornerShape(24.dp))
                                .clickable(enabled = !showing && !done) {
                                    scope.launch {
                                        lit = p
                                        Sfx.pop()
                                        delay(160)
                                        lit = -1
                                    }
                                    if (sequence[inputIndex] == p) {
                                        inputIndex++
                                        if (inputIndex == sequence.size) {
                                            if (sequence.size > goal) {
                                                done = true
                                                onFinish(sequence.size * 2, true)
                                            } else {
                                                showing = true
                                                round++
                                            }
                                        }
                                    } else {
                                        done = true
                                        val completed = sequence.size - 1
                                        onFinish(completed * 2, completed >= goal)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) { com.kodomo.app.ui.fx.Emoji(padEmojis[p], 53.dp) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 7) Math balloons: sums grow, subtraction then times-tables join in, and balloons rise faster.
// ---------------------------------------------------------------------------------------------------------------------

internal data class MathQuestion(val text: String, val answer: Int, val options: List<Int>)

internal fun mathQuestion(level: Int, random: Random = Random): MathQuestion {
    val max = 5 + level * 2
    val kind = when {
        level <= 3 -> 0
        level <= 6 -> random.nextInt(2)
        else -> random.nextInt(3)
    }
    val (text, answer) = when (kind) {
        0 -> { val a = random.nextInt(1, max); val b = random.nextInt(1, max); "${a.fa()} + ${b.fa()}" to a + b }
        1 -> { val a = random.nextInt(2, max + 5); val b = random.nextInt(1, a); "${a.fa()} − ${b.fa()}" to a - b }
        else -> { val a = random.nextInt(2, 6 + level / 2); val b = random.nextInt(2, 6); "${a.fa()} × ${b.fa()}" to a * b }
    }
    val options = buildSet {
        add(answer)
        while (size < 4) add((answer + random.nextInt(-4, 5)).coerceAtLeast(0))
    }.shuffled(random)
    return MathQuestion(text, answer, options)
}

@Composable
fun MathBalloonsGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val total = 10
    val need = 7
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(mathQuestion(level)) }
    var rise by remember { mutableFloatStateOf(0f) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val riseSeconds = (8.5f - level * 0.45f).coerceAtLeast(3.6f)

    fun next(right: Boolean) {
        if (done) return
        if (right) correct++
        feedback = if (right) "✅ آفرین!" else "جواب: ${question.answer.fa()}"
        index++
        if (index >= total) {
            done = true
            onFinish(correct * 2, correct >= need)
        } else {
            question = mathQuestion(level)
            rise = 0f
        }
    }

    LaunchedEffect(index) {
        var last = 0L
        val current = index
        while (!done && index == current && rise < 1f) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                rise += dt / riseSeconds
            }
        }
        if (!done && index == current) next(false)
    }

    Column(Modifier.fillMaxSize()) {
        GameHeader("➕ سؤال ${(index + 1).coerceAtMost(total).fa()} از ${total.fa()}   درست: ${correct.fa()}", correct / need.toFloat(), KodomoColors.Sun)
        Text(
            "⁦${question.text} = ?⁩",
            style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
        feedback?.let { Text(it, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(Color(0xFFDFF1FF), Color(0xFFFFF4D6))))) {
            val h = maxHeight
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                question.options.forEachIndexed { i, opt ->
                    val wobble = ((i % 2) * 2 - 1) * 0.06f
                    Box(
                        Modifier
                            .offset { IntOffset(0, ((h.toPx() - 110.dp.toPx()) * (1f - rise - wobble * rise)).roundToInt().coerceAtLeast(0)) }
                            .size(76.dp, 96.dp)
                            .background(padColors[i % padColors.size], RoundedCornerShape(50))
                            .clickable(enabled = !done) {
                                scope.launch { Sfx.pop() }
                                next(opt == question.answer)
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(opt.fa(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
