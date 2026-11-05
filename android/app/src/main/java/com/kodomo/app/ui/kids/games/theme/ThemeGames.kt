package com.kodomo.app.ui.kids.games.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.fx.Sfx
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

@Composable
private fun Hud(text: String, progress: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = color)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Car race: three lanes, swipe or tap left/right to dodge traffic and collect coins. Faster with every level.
// ---------------------------------------------------------------------------------------------------------------------

private data class RoadThing(val lane: Int, val y: Float, val emoji: String, val coin: Boolean)

@Composable
fun CarRaceGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val target = 15 + level * 5
    var lane by remember { mutableIntStateOf(1) }
    val things = remember { mutableStateListOf<RoadThing>() }
    var coins by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var stripe by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val carX by animateFloatAsState((lane + 0.5f) / 3f, label = "car")

    LaunchedEffect(Unit) {
        var last = 0L
        var spawn = 0f
        val speed = 0.38f + level * 0.06f
        val every = (0.95f - level * 0.05f).coerceAtLeast(0.42f)
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                stripe = (stripe + speed * dt) % 0.2f
                spawn += dt
                if (spawn > every) {
                    spawn = 0f
                    val coin = Random.nextFloat() < 0.45f
                    things.add(RoadThing(Random.nextInt(3), -0.1f, if (coin) "🪙" else listOf("🚙", "🚌", "🚧", "🛻").random(), coin))
                }
                val next = mutableListOf<RoadThing>()
                things.forEach { t ->
                    val y = t.y + speed * dt
                    if (t.lane == lane && y in 0.78f..0.9f) {
                        if (t.coin) coins++ else lives--
                        scope.launch { Sfx.pop() }
                    } else if (y < 1.1f) next.add(t.copy(y = y))
                }
                things.clear()
                things.addAll(next)
                if (lives <= 0 || coins >= target) {
                    done = true
                    onFinish(coins, coins >= target)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Hud("🪙 ${coins.fa()} از ${target.fa()}   ${"❤️".repeat(lives.coerceAtLeast(0))}", coins / target.toFloat(), KodomoColors.Sun)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF546E7A))
                .pointerInput(Unit) {
                    detectTapGestures { pos -> lane = if (pos.x < size.width / 2) (lane - 1).coerceAtLeast(0) else (lane + 1).coerceAtMost(2) }
                }
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectDragGestures(onDragStart = { dragged = 0f }) { _, d ->
                        dragged += d.x
                        if (dragged > 60f) { lane = (lane + 1).coerceAtMost(2); dragged = 0f }
                        if (dragged < -60f) { lane = (lane - 1).coerceAtLeast(0); dragged = 0f }
                    }
                },
        ) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF7CB342), Offset.Zero, Size(this.size.width * 0.04f, this.size.height))
                drawRect(Color(0xFF7CB342), Offset(this.size.width * 0.96f, 0f), Size(this.size.width * 0.04f, this.size.height))
                for (l in 1..2) {
                    var y = -0.2f + stripe
                    while (y < 1.1f) {
                        drawRect(Color.White.copy(alpha = 0.8f), Offset(this.size.width * l / 3f - 4f, y * this.size.height), Size(8f, this.size.height * 0.08f))
                        y += 0.2f
                    }
                }
            }
            things.forEach { t ->
                com.kodomo.app.ui.fx.Emoji(t.emoji, 48.dp, modifier = Modifier.offset(x = w * ((t.lane + 0.5f) / 3f) - 22.dp, y = h * t.y))
            }
            com.kodomo.app.ui.fx.Emoji("🏎️", 65.dp, modifier = Modifier.offset(x = w * carX - 30.dp, y = h * 0.8f))
        }
        Text("برای عوض کردن خط، به چپ و راست بکش یا روی آن سمت بزن.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(6.dp))
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Space guard: drag the ship; it fires automatically. Stop meteors before they reach the planet.
// ---------------------------------------------------------------------------------------------------------------------

private data class Meteor(val id: Int, val x: Float, val y: Float, val speed: Float, val hp: Int)
private data class Laser(val x: Float, val y: Float)

@Composable
fun SpaceGuardGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val target = 15 + level * 4
    var shipX by remember { mutableFloatStateOf(0.5f) }
    val meteors = remember { mutableStateListOf<Meteor>() }
    val lasers = remember { mutableStateListOf<Laser>() }
    var hits by remember { mutableIntStateOf(0) }
    var shield by remember { mutableIntStateOf(5) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        var last = 0L
        var spawn = 0f
        var fire = 0f
        var id = 0
        val every = (1.1f - level * 0.07f).coerceAtLeast(0.4f)
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                spawn += dt
                fire += dt
                if (spawn > every) {
                    spawn = 0f
                    meteors.add(Meteor(id++, Random.nextFloat() * 0.85f + 0.07f, -0.05f, 0.12f + level * 0.025f + Random.nextFloat() * 0.06f, if (level >= 6 && Random.nextFloat() < 0.3f) 2 else 1))
                }
                if (fire > 0.28f) {
                    fire = 0f
                    lasers.add(Laser(shipX, 0.84f))
                }
                val movedLasers = lasers.map { it.copy(y = it.y - 1.3f * dt) }.filter { it.y > -0.05f }.toMutableList()
                val movedMeteors = mutableListOf<Meteor>()
                meteors.forEach { m0 ->
                    var m = m0.copy(y = m0.y + m0.speed * dt)
                    val hitLaser = movedLasers.firstOrNull { abs(it.x - m.x) < 0.06f && abs(it.y - m.y) < 0.05f }
                    if (hitLaser != null) {
                        movedLasers.remove(hitLaser)
                        m = m.copy(hp = m.hp - 1)
                        if (m.hp <= 0) { hits++; scope.launch { Sfx.pop() }; return@forEach }
                    }
                    if (m.y > 0.92f) { shield--; return@forEach }
                    movedMeteors.add(m)
                }
                lasers.clear(); lasers.addAll(movedLasers)
                meteors.clear(); meteors.addAll(movedMeteors)
                if (shield <= 0 || hits >= target) {
                    done = true
                    onFinish(hits, hits >= target)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Hud("☄️ ${hits.fa()} از ${target.fa()}   🛡️ ${shield.coerceAtLeast(0).fa()}", hits / target.toFloat(), KodomoColors.Lavender)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF3949AB))))
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { shipX = (it.x / size.width).coerceIn(0.05f, 0.95f) }) { c, _ ->
                        shipX = (c.position.x / size.width).coerceIn(0.05f, 0.95f)
                    }
                }
                .pointerInput(Unit) { detectTapGestures { shipX = (it.x / size.width).coerceIn(0.05f, 0.95f) } },
        ) {
            val w = maxWidth
            val h = maxHeight
            val stars = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
            Canvas(Modifier.fillMaxSize()) {
                stars.forEach { drawCircle(Color.White.copy(alpha = 0.6f), 2.5f, Offset(it.x * this.size.width, it.y * this.size.height)) }
                lasers.forEach { drawLine(Color(0xFF69F0AE), Offset(it.x * this.size.width, it.y * this.size.height), Offset(it.x * this.size.width, (it.y + 0.04f) * this.size.height), strokeWidth = 8f) }
                drawArc(Color(0xFF4FC3F7).copy(alpha = 0.5f), 180f, 180f, false, Offset(-this.size.width * 0.1f, this.size.height * 0.93f), Size(this.size.width * 1.2f, this.size.height * 0.3f), style = Stroke(6f))
            }
            meteors.forEach { m -> Text(if (m.hp > 1) "🪨" else "☄️", fontSize = 36.sp, modifier = Modifier.offset(x = w * m.x - 20.dp, y = h * m.y)) }
            com.kodomo.app.ui.fx.Emoji("🚀", 58.dp, modifier = Modifier.offset(x = w * shipX - 26.dp, y = h * 0.84f))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Little bakery: build the cake the customer orders (base, cream, topping, candles). More parts and less time per level.
// ---------------------------------------------------------------------------------------------------------------------

private val cakeBases = listOf("🟫 شکلاتی" to Color(0xFF6D4C41), "🟨 وانیلی" to Color(0xFFFFE082), "🟥 توت‌فرنگی" to Color(0xFFF48FB1))
private val creams = listOf("سفید" to Color.White, "صورتی" to Color(0xFFF8BBD0), "آبی" to Color(0xFFB3E5FC), "سبز" to Color(0xFFC8E6C9))
private val toppings = listOf("🍓", "🍒", "🍫", "🫐", "🍌", "⭐")

private data class CakeOrder(val base: Int, val cream: Int, val topping: Int, val candles: Int)

@Composable
fun BakeryGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val ordersToServe = 3 + level / 2
    val useCandles = level >= 3
    fun newOrder() = CakeOrder(Random.nextInt(cakeBases.size), Random.nextInt(creams.size), Random.nextInt(toppings.size), if (useCandles) Random.nextInt(1, 6) else 0)
    var order by remember { mutableStateOf(newOrder()) }
    var base by remember { mutableIntStateOf(-1) }
    var cream by remember { mutableIntStateOf(-1) }
    var topping by remember { mutableIntStateOf(-1) }
    var candles by remember { mutableIntStateOf(0) }
    var served by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun serve() {
        val ok = base == order.base && cream == order.cream && topping == order.topping && candles == order.candles
        if (ok) {
            served++
            message = "😋 مشتری خوشحال شد!"
            scope.launch { Sfx.success() }
        } else {
            mistakes++
            message = "😕 این همان کیکی نبود که خواسته بود. دوباره!"
        }
        if (served >= ordersToServe || mistakes >= 3) {
            onFinish(served * 4 - mistakes, served >= ordersToServe)
            return
        }
        if (ok) {
            order = newOrder()
            base = -1; cream = -1; topping = -1; candles = 0
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Hud("🎂 کیک‌های تحویلی: ${served.fa()} از ${ordersToServe.fa()}   ❌ ${mistakes.fa()} از ۳", served / ordersToServe.toFloat(), KodomoColors.Rose)
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFFFF8E1), RoundedCornerShape(18.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.kodomo.app.ui.fx.Emoji("🧒", 48.dp)
            Text(
                "سفارش: کیک ${cakeBases[order.base].first.drop(3)} با خامه‌ی ${creams[order.cream].first} و ${toppings[order.topping]}" +
                    if (order.candles > 0) " و ${order.candles.fa()} شمع" else "",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        // The cake being built.
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.size(220.dp, 160.dp)) {
                val w = this.size.width
                val h = this.size.height
                drawRoundRect(Color(0xFFBDBDBD), Offset(0f, h * 0.88f), Size(w, h * 0.1f), androidx.compose.ui.geometry.CornerRadius(20f))
                if (base >= 0) drawRoundRect(cakeBases[base].second, Offset(w * 0.12f, h * 0.45f), Size(w * 0.76f, h * 0.44f), androidx.compose.ui.geometry.CornerRadius(18f))
                if (cream >= 0) drawRoundRect(creams[cream].second, Offset(w * 0.1f, h * 0.36f), Size(w * 0.8f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(30f))
                repeat(candles) { i ->
                    val x = w * (0.25f + i * 0.12f)
                    drawRect(Color(0xFF64B5F6), Offset(x, h * 0.12f), Size(w * 0.03f, h * 0.24f))
                    drawCircle(Color(0xFFFFB300), w * 0.025f, Offset(x + w * 0.015f, h * 0.09f))
                }
            }
            if (topping >= 0) Text(toppings[topping].repeat(3), fontSize = 26.sp, modifier = Modifier.padding(bottom = 98.dp))
        }
        message?.let { Text(it, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Text("کیک", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cakeBases.size) { i -> Chip(cakeBases[i].first, base == i) { base = i } }
        }
        Text("خامه", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(creams.size) { i -> Chip("🍦 ${creams[i].first}", cream == i) { cream = i } }
        }
        Text("تزئین", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(toppings.size) { i -> Chip(toppings[i], topping == i) { topping = i } }
        }
        if (useCandles) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("شمع: ${candles.fa()}", style = MaterialTheme.typography.labelLarge)
                Chip("➖", false) { candles = (candles - 1).coerceAtLeast(0) }
                Chip("➕", false) { candles = (candles + 1).coerceAtMost(5) }
            }
        }
        Button(onClick = { serve() }, enabled = base >= 0 && cream >= 0 && topping >= 0, modifier = Modifier.fillMaxWidth()) { Text("تحویل به مشتری 🎁") }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 18.sp,
        modifier = Modifier
            .background(if (selected) KodomoColors.Green else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .bouncyClick(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
    )
}

// ---------------------------------------------------------------------------------------------------------------------
// Stylist: dress the character for the occasion (weather / event). Wrong choices for the weather cost a heart.
// ---------------------------------------------------------------------------------------------------------------------

private data class Occasion(val title: String, val emoji: String, val good: Set<String>, val bad: Set<String>)

private val occasions = listOf(
    Occasion("روز برفی", "❄️", setOf("🧥", "🧣", "🧤", "🥾", "🧢"), setOf("🩳", "🩴", "🕶️")),
    Occasion("ساحل تابستانی", "🏖️", setOf("🩳", "🩴", "🕶️", "👒", "👕"), setOf("🧥", "🧣", "🧤", "🥾")),
    Occasion("روز بارانی", "🌧️", setOf("☂️", "🧥", "🥾", "🧢"), setOf("🩴", "🕶️", "👒")),
    Occasion("جشن تولد", "🎉", setOf("👗", "👔", "🎀", "👞", "👑"), setOf("🥾", "🧤")),
    Occasion("کلاس ورزش", "🏃", setOf("👕", "🩳", "👟", "🧢"), setOf("👗", "👔", "👞", "👑")),
    Occasion("پیک‌نیک بهاری", "🌸", setOf("👕", "👒", "👟", "🕶️", "🎀"), setOf("🧣", "🧤", "🧥")),
)
private val wardrobe = listOf("🧥", "🧣", "🧤", "🥾", "🧢", "🩳", "🩴", "🕶️", "👒", "👕", "☂️", "👗", "👔", "🎀", "👞", "👑", "👟")

@Composable
fun StylistGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val rounds = 3 + level / 2
    val need = (2 + level / 3).coerceAtMost(4)
    var round by remember { mutableIntStateOf(0) }
    var occasion by remember { mutableStateOf(occasions.random()) }
    val chosen = remember { mutableStateListOf<String>() }
    var score by remember { mutableIntStateOf(0) }
    var hearts by remember { mutableIntStateOf(3) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun check() {
        val good = chosen.count { it in occasion.good }
        val bad = chosen.count { it in occasion.bad }
        if (good >= need && bad == 0) {
            score += good * 2
            feedback = "🌟 عالی! برای «${occasion.title}» حرفه‌ای لباس پوشیدی."
            scope.launch { Sfx.success() }
            round++
            if (round >= rounds) { onFinish(score, true); return }
            occasion = occasions.filter { it != occasion }.random()
            chosen.clear()
        } else {
            hearts--
            feedback = if (bad > 0) "🙈 «${chosen.first { it in occasion.bad }}» برای ${occasion.title} مناسب نیست!" else "باید دست‌کم ${need.fa()} وسیله‌ی مناسب انتخاب کنی."
            if (hearts <= 0) onFinish(score, false)
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Hud("👗 مرحله‌ی ${(round + 1).coerceAtMost(rounds).fa()} از ${rounds.fa()}   ${"❤️".repeat(hearts.coerceAtLeast(0))}", round / rounds.toFloat(), KodomoColors.Lavender)
        Text("${occasion.emoji} امروز: ${occasion.title}", style = MaterialTheme.typography.headlineSmall)
        Text("دست‌کم ${need.fa()} وسیله‌ی مناسب انتخاب کن.", style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier.size(180.dp).background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))), CircleShape).border(3.dp, KodomoColors.Peach, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.kodomo.app.ui.fx.Emoji("🧒", 77.dp)
                Text(chosen.joinToString(""), fontSize = 26.sp)
            }
        }
        feedback?.let { Text(it, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center) }
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(wardrobe.size) { i ->
                val item = wardrobe[i]
                val on = item in chosen
                Box(
                    Modifier
                        .size(52.dp)
                        .background(if (on) KodomoColors.Green.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .bouncyClick { if (on) chosen.remove(item) else chosen.add(item) },
                    contentAlignment = Alignment.Center,
                ) { com.kodomo.app.ui.fx.Emoji(item, 34.dp) }
            }
        }
        Button(onClick = { check() }, enabled = chosen.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("آماده‌ام! ✅") }
    }
}
