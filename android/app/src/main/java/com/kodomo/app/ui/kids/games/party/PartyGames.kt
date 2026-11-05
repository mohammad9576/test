package com.kodomo.app.ui.kids.games.party

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.Sfx
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

/** Up to four players sharing one phone on the table; each owns a coloured zone facing their side. */
internal data class Player(val name: String, val emoji: String, val color: Color)

internal val partyPlayers = listOf(
    Player("قرمز", "🦊", Color(0xFFE53935)),
    Player("آبی", "🐳", Color(0xFF1E88E5)),
    Player("سبز", "🐸", Color(0xFF43A047)),
    Player("زرد", "🐥", Color(0xFFFDD835)),
)

/** «How many players?» picker shown before every party game. */
@Composable
internal fun PlayerCountPicker(title: String, rules: String, min: Int, max: Int, onStart: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(rules, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text("گوشی را روی میز بگذارید؛ هر بازیکن کنار رنگ خودش بنشیند.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        for (n in min..max) {
            Button(onClick = { onStart(n) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("${n.fa()} نفره  " + partyPlayers.take(n).joinToString(" ") { it.emoji }, fontSize = 22.sp)
            }
        }
    }
}

/**
 * Splits the screen into one zone per player. Zones in the top half are rotated 180° so the player sitting across the
 * table reads them the right way up.
 */
@Composable
internal fun PlayerZones(count: Int, modifier: Modifier = Modifier, center: @Composable BoxScope.() -> Unit = {}, zone: @Composable BoxScope.(Int) -> Unit) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val top = if (count == 2) listOf(0) else listOf(0, 1)
            val bottom = when (count) { 2 -> listOf(1); 3 -> listOf(2); else -> listOf(2, 3) }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                top.forEach { i ->
                    Box(Modifier.weight(1f).fillMaxSize().rotate(180f).background(partyPlayers[i].color.copy(alpha = 0.22f))) { zone(i) }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                bottom.forEach { i ->
                    Box(Modifier.weight(1f).fillMaxSize().background(partyPlayers[i].color.copy(alpha = 0.22f))) { zone(i) }
                }
            }
        }
        Box(Modifier.align(Alignment.Center)) { center() }
    }
}

/** Fires [onDown] for every new finger that lands on this element (works for several players at once). */
internal fun Modifier.onEveryPress(onDown: () -> Unit) = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onDown()
    }
}

@Composable
internal fun WinnerBanner(text: String, onAgain: () -> Unit, onDone: () -> Unit) {
    LaunchedEffect(text) { Sfx.success() }
    Column(
        Modifier
            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Button(onClick = onAgain) { Text("یک دور دیگر 🔁") }
        androidx.compose.material3.OutlinedButton(onClick = onDone) { Text("پایان بازی") }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 1) Tap race: tap as fast as you can to drive your car to the flag.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun TapRaceGame(onFinish: (Int, Boolean) -> Unit) {
    var players by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    if (players == 0) {
        PlayerCountPicker("🏁 مسابقه‌ی ضربه", "هر کس سریع‌تر روی رنگ خودش ضربه بزند، ماشینش زودتر به پرچم می‌رسد!", 2, 4) { players = it }
        return
    }
    androidx.compose.runtime.key(round) {
        val goal = 45
        val progress = remember { mutableStateListOf(0, 0, 0, 0) }
        var winner by remember { mutableStateOf<Int?>(null) }
        var countdown by remember { mutableIntStateOf(3) }
        LaunchedEffect(Unit) { while (countdown > 0) { delay(800); countdown-- } }
        PlayerZones(
            players,
            center = {
                when {
                    winner != null -> WinnerBanner("🏆 ${partyPlayers[winner!!].emoji} ${partyPlayers[winner!!].name} برنده شد!", { round++ }) { onFinish(10, true) }
                    countdown > 0 -> Text(countdown.fa(), fontSize = 72.sp, fontWeight = FontWeight.Black)
                }
            },
        ) { i ->
            val p = partyPlayers[i]
            val pos by animateFloatAsState(progress[i] / goal.toFloat(), label = "car")
            Column(
                Modifier.fillMaxSize().onEveryPress {
                    if (countdown == 0 && winner == null) {
                        progress[i] = progress[i] + 1
                        if (progress[i] >= goal) winner = i
                    }
                }.padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${p.emoji} ${p.name}", style = MaterialTheme.typography.titleLarge, color = p.color, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF455A64), RoundedCornerShape(12.dp))) {
                    com.kodomo.app.ui.fx.Emoji("🏁", 36.dp, modifier = Modifier.align(Alignment.CenterEnd))
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                        com.kodomo.app.ui.fx.Emoji("🏎️", 41.dp, modifier = Modifier.padding(start = (maxWidth - 48.dp) * pos).align(Alignment.CenterStart))
                    }
                }
                Text("بزن! بزن! 👆", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 2) Reflex duel: wait for green, then be the first to tap. Tapping early costs a point.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun ReflexDuelGame(onFinish: (Int, Boolean) -> Unit) {
    var players by remember { mutableIntStateOf(0) }
    var match by remember { mutableIntStateOf(0) }
    if (players == 0) {
        PlayerCountPicker("⚡ کی سریع‌تره؟", "صبر کن تا دایره سبز شود، بعد زودتر از همه روی رنگ خودت بزن. اگر زودتر بزنی، یک امتیاز کم می‌شود!", 2, 4) { players = it }
        return
    }
    androidx.compose.runtime.key(match) {
        val target = 5
        val scores = remember { mutableStateListOf(0, 0, 0, 0) }
        var green by remember { mutableStateOf(false) }
        var roundWinner by remember { mutableStateOf<Int?>(null) }
        var round by remember { mutableIntStateOf(0) }
        var champion by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(round) {
            green = false
            roundWinner = null
            delay(Random.nextLong(1_500, 4_500))
            green = true
        }
        PlayerZones(
            players,
            center = {
                if (champion != null) {
                    WinnerBanner("🏆 ${partyPlayers[champion!!].emoji} ${partyPlayers[champion!!].name} قهرمان شد!", { match++ }) { onFinish(10, true) }
                } else {
                    Box(
                        Modifier.size(120.dp).background(if (green) Color(0xFF43A047) else Color(0xFFE53935), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (green) "الان!" else "صبر…", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                }
            },
        ) { i ->
            val p = partyPlayers[i]
            Column(
                Modifier.fillMaxSize().onEveryPress {
                    if (champion != null || roundWinner != null) return@onEveryPress
                    if (green) {
                        roundWinner = i
                        scores[i] = scores[i] + 1
                        if (scores[i] >= target) champion = i else round++
                    } else {
                        scores[i] = (scores[i] - 1).coerceAtLeast(0)
                    }
                }.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                com.kodomo.app.ui.fx.Emoji(p.emoji, 65.dp)
                Text("امتیاز: ${scores[i].fa()} از ${target.fa()}", style = MaterialTheme.typography.titleMedium, color = p.color, fontWeight = FontWeight.Bold)
                if (roundWinner == i) Text("⚡ تو زودتر زدی!", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 3) Air hockey: two players, one finger each, first to 5 goals.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun AirHockeyGame(onFinish: (Int, Boolean) -> Unit) {
    var match by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.key(match) {
        val target = 5
        var size by remember { mutableStateOf(Size.Zero) }
        var puck by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
        var velocity by remember { mutableStateOf(Offset(0f, 0f)) }
        var top by remember { mutableStateOf(Offset(0.5f, 0.12f)) }
        var bottom by remember { mutableStateOf(Offset(0.5f, 0.88f)) }
        var scoreTop by remember { mutableIntStateOf(0) }
        var scoreBottom by remember { mutableIntStateOf(0) }
        var winner by remember { mutableStateOf<Int?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.03f)
                    last = now
                    if (winner != null || size == Size.Zero) return@withFrameNanos
                    val aspect = size.height / size.width
                    var p = puck + velocity * dt
                    var v = velocity * (1f - 0.35f * dt)
                    val r = 0.045f
                    if (p.x < r) { p = p.copy(x = r); v = v.copy(x = abs(v.x)) }
                    if (p.x > 1 - r) { p = p.copy(x = 1 - r); v = v.copy(x = -abs(v.x)) }
                    val inGoal = p.x in 0.3f..0.7f
                    if (p.y < r / aspect) {
                        if (inGoal) { scoreBottom++; scope.launch { Sfx.success() }; p = Offset(0.5f, 0.5f); v = Offset(0f, -0.15f) } else { p = p.copy(y = r / aspect); v = v.copy(y = abs(v.y)) }
                    }
                    if (p.y > 1 - r / aspect) {
                        if (inGoal) { scoreTop++; scope.launch { Sfx.success() }; p = Offset(0.5f, 0.5f); v = Offset(0f, 0.15f) } else { p = p.copy(y = 1 - r / aspect); v = v.copy(y = -abs(v.y)) }
                    }
                    // Paddle hits (computed in pixels so the round shapes stay round).
                    listOf(top, bottom).forEach { paddle ->
                        val dx = (p.x - paddle.x) * size.width
                        val dy = (p.y - paddle.y) * size.height
                        val dist = hypot(dx, dy)
                        val min = size.width * 0.12f
                        if (dist < min && dist > 0.1f) {
                            val nx = dx / dist
                            val ny = dy / dist
                            val speed = (hypot(v.x * size.width, v.y * size.height) + size.width * 0.9f).coerceAtMost(size.width * 2.6f)
                            v = Offset(nx * speed / size.width, ny * speed / size.height)
                            p = Offset(paddle.x + nx * min / size.width, paddle.y + ny * min / size.height)
                            scope.launch { Sfx.pop() }
                        }
                    }
                    puck = p
                    velocity = v
                    if (scoreTop >= target) winner = 0
                    if (scoreBottom >= target) winner = 1
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFFFFFFF), Color(0xFFFFEBEE))))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { c ->
                                    if (!c.pressed) return@forEach
                                    val x = (c.position.x / this.size.width).coerceIn(0.06f, 0.94f)
                                    val y = c.position.y / this.size.height
                                    if (y < 0.5f) top = Offset(x, y.coerceIn(0.05f, 0.45f)) else bottom = Offset(x, y.coerceIn(0.55f, 0.95f))
                                    c.consume()
                                }
                            }
                        }
                    },
            ) {
                size = this.size
                val w = this.size.width
                val h = this.size.height
                drawLine(Color(0xFF90A4AE), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 6f)
                drawCircle(Color(0xFF90A4AE), w * 0.18f, Offset(w / 2, h / 2), style = Stroke(6f))
                drawRect(partyPlayers[1].color, Offset(w * 0.3f, 0f), Size(w * 0.4f, 14f))
                drawRect(partyPlayers[0].color, Offset(w * 0.3f, h - 14f), Size(w * 0.4f, 14f))
                drawCircle(Color(0xFF263238), w * 0.045f, Offset(puck.x * w, puck.y * h))
                drawCircle(partyPlayers[0].color, w * 0.075f, Offset(top.x * w, top.y * h))
                drawCircle(Color.White.copy(alpha = 0.6f), w * 0.03f, Offset(top.x * w, top.y * h))
                drawCircle(partyPlayers[1].color, w * 0.075f, Offset(bottom.x * w, bottom.y * h))
                drawCircle(Color.White.copy(alpha = 0.6f), w * 0.03f, Offset(bottom.x * w, bottom.y * h))
            }
            Text("${partyPlayers[0].emoji} ${scoreTop.fa()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = partyPlayers[0].color, modifier = Modifier.align(Alignment.CenterStart).padding(12.dp).rotate(180f))
            Text("${partyPlayers[1].emoji} ${scoreBottom.fa()}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = partyPlayers[1].color, modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp))
            winner?.let { w ->
                Box(Modifier.align(Alignment.Center).then(if (w == 0) Modifier.rotate(180f) else Modifier)) {
                    WinnerBanner("🏆 ${partyPlayers[w].emoji} ${partyPlayers[w].name} برنده شد!", { match++ }) { onFinish(10, true) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 4) Tug of war: two teams tap to pull the rope to their side.
// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun TugOfWarGame(onFinish: (Int, Boolean) -> Unit) {
    var players by remember { mutableIntStateOf(0) }
    var match by remember { mutableIntStateOf(0) }
    if (players == 0) {
        PlayerCountPicker("🪢 طناب‌کشی", "دو تیم: بالا در برابر پایین. هر چه سریع‌تر بزنید، طناب به سمت شما می‌آید!", 2, 4) { players = it }
        return
    }
    androidx.compose.runtime.key(match) {
        val limit = 30
        var rope by remember { mutableIntStateOf(0) } // negative = top team pulls, positive = bottom team
        var winner by remember { mutableStateOf<String?>(null) }
        val shift by animateFloatAsState(rope / limit.toFloat(), label = "rope")
        PlayerZones(
            players,
            center = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.kodomo.app.ui.fx.Emoji("🪢", 77.dp, modifier = Modifier.padding(top = (if (shift > 0) shift * 120 else 0f).dp, bottom = (if (shift < 0) -shift * 120 else 0f).dp))
                    winner?.let { WinnerBanner(it, { match++ }) { onFinish(10, true) } }
                }
            },
        ) { i ->
            val topTeam = if (players == 2) i == 0 else i < 2
            val p = partyPlayers[i]
            Column(
                Modifier.fillMaxSize().onEveryPress {
                    if (winner != null) return@onEveryPress
                    rope += if (topTeam) -1 else 1
                    if (rope <= -limit) winner = "🏆 تیم بالا برنده شد!"
                    if (rope >= limit) winner = "🏆 تیم پایین برنده شد!"
                }.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                com.kodomo.app.ui.fx.Emoji(p.emoji, 60.dp)
                Text(if (topTeam) "تیم بالا" else "تیم پایین", color = p.color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("بکش! 💪", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 5) Snap match: when the two pictures in the middle are the same, be the first to tap. Wrong taps lose a point.
// ---------------------------------------------------------------------------------------------------------------------

private val snapPictures = listOf("🍎", "🚗", "⚽", "🐶", "⭐", "🌙", "🎈", "🐟", "🌼", "🍩")

@Composable
fun SnapMatchGame(onFinish: (Int, Boolean) -> Unit) {
    var players by remember { mutableIntStateOf(0) }
    var match by remember { mutableIntStateOf(0) }
    if (players == 0) {
        PlayerCountPicker("👀 جفت‌یاب سریع", "وقتی دو تصویر وسط صفحه یکی شدند، زودتر از همه روی رنگ خودت بزن! اگر یکی نباشند و بزنی، امتیاز کم می‌شود.", 2, 4) { players = it }
        return
    }
    androidx.compose.runtime.key(match) {
        val target = 6
        val scores = remember { mutableStateListOf(0, 0, 0, 0) }
        var left by remember { mutableStateOf("🍎") }
        var right by remember { mutableStateOf("🚗") }
        var claimed by remember { mutableStateOf(false) }
        var champion by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(Unit) {
            while (champion == null) {
                delay(1_100)
                claimed = false
                left = snapPictures.random()
                right = if (Random.nextFloat() < 0.35f) left else snapPictures.random()
            }
        }
        PlayerZones(
            players,
            center = {
                if (champion != null) {
                    WinnerBanner("🏆 ${partyPlayers[champion!!].emoji} ${partyPlayers[champion!!].name} برنده شد!", { match++ }) { onFinish(10, true) }
                } else {
                    Row(
                        Modifier.background(Color.White, RoundedCornerShape(24.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        com.kodomo.app.ui.fx.Emoji(left, 62.dp)
                        com.kodomo.app.ui.fx.Emoji(right, 62.dp)
                    }
                }
            },
        ) { i ->
            val p = partyPlayers[i]
            Column(
                Modifier.fillMaxSize().onEveryPress {
                    if (champion != null || claimed) return@onEveryPress
                    if (left == right) {
                        claimed = true
                        scores[i] = scores[i] + 1
                        if (scores[i] >= target) champion = i
                    } else {
                        scores[i] = (scores[i] - 1).coerceAtLeast(0)
                    }
                }.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                com.kodomo.app.ui.fx.Emoji(p.emoji, 53.dp)
                Text("${scores[i].fa()} از ${target.fa()}", color = p.color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
