package com.kodomo.app.ui.kids.games.adventure

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.PoseLandmark
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.GameMusic
import com.kodomo.app.ui.fx.GameSfx
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.games.pose.PoseStage
import com.kodomo.app.ui.kids.games.pose.PoseTracker
import com.kodomo.app.ui.kids.games.pose.handsOnStage
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

// =====================================================================================================================
// «جواب با بدن»: a homework quiz where the child answers by moving to the left, middle or right answer.
// =====================================================================================================================

data class BodyQuestion(val prompt: String, val answers: List<String>, val correct: Int)

/** Questions scale with the child's age and the level: counting/shapes → sums → times tables, letters, science facts. */
internal fun bodyQuestion(age: Int, level: Int, random: Random = Random): BodyQuestion {
    fun options(answer: Int, spread: Int): Pair<List<String>, Int> {
        val set = linkedSetOf(answer)
        while (set.size < 3) set += (answer + random.nextInt(-spread, spread + 1)).coerceAtLeast(0)
        val list = set.shuffled(random)
        return list.map { it.fa() } to list.indexOf(answer)
    }
    val kind = when {
        age <= 5 -> random.nextInt(2)          // counting, shapes/colours
        age <= 8 -> 2 + random.nextInt(3)      // sums, letters, science
        else -> 3 + random.nextInt(3)          // letters/science/times tables
    }
    return when (kind) {
        0 -> {
            val n = random.nextInt(1, 6 + level.coerceAtMost(4))
            val (o, c) = options(n, 2)
            BodyQuestion("چند ستاره است؟ " + "⭐".repeat(n), o, c)
        }
        1 -> {
            val items = listOf("🔴 دایره" to "گرد", "🟥 مربع" to "چهارگوش", "🔺 مثلث" to "سه‌گوش")
            val pick = random.nextInt(items.size)
            val list = items.map { it.first }
            BodyQuestion("کدام ${items[pick].second} است؟", list, pick)
        }
        2 -> {
            val max = 5 + level * 3
            val a = random.nextInt(1, max)
            val b = random.nextInt(1, max)
            val minus = level >= 4 && random.nextBoolean() && a > b
            val ans = if (minus) a - b else a + b
            val (o, c) = options(ans, 3)
            BodyQuestion("⁦${a.fa()} ${if (minus) "−" else "+"} ${b.fa()} = ؟⁩", o, c)
        }
        3 -> {
            val words = listOf(
                "آب" to "آ", "بادکنک" to "ب", "پروانه" to "پ", "توپ" to "ت", "سیب" to "س", "جوجه" to "ج", "خرگوش" to "خ",
                "درخت" to "د", "رنگین‌کمان" to "ر", "زنبور" to "ز", "شیر" to "ش", "فیل" to "ف", "کتاب" to "ک", "گل" to "گ",
                "ماه" to "م", "نان" to "ن", "هویج" to "ه", "یخ" to "ی",
            )
            val (word, letter) = words.random(random)
            val wrong = words.map { it.second }.filter { it != letter }.distinct().shuffled(random).take(2)
            val list = (wrong + letter).shuffled(random)
            BodyQuestion("«$word» با کدام حرف شروع می‌شود؟", list, list.indexOf(letter))
        }
        4 -> {
            val facts = listOf(
                BodyQuestion("کدام سیاره به خورشید نزدیک‌تر است؟", listOf("عطارد", "مریخ", "زحل"), 0),
                BodyQuestion("گیاهان برای ساختن غذا به چه نیاز دارند؟", listOf("نور خورشید", "تاریکی", "یخ"), 0),
                BodyQuestion("کدام حیوان پستاندار است؟", listOf("مرغ", "نهنگ", "مار"), 1),
                BodyQuestion("آب در چند درجه یخ می‌زند؟", listOf("صفر", "پنجاه", "صد"), 0),
                BodyQuestion("قلب ما چه کاری می‌کند؟", listOf("خون را پمپ می‌کند", "غذا را هضم می‌کند", "صدا را می‌شنود"), 0),
                BodyQuestion("کدام جامد است؟", listOf("بخار", "سنگ", "آب"), 1),
                BodyQuestion("زمین دور چه چیزی می‌چرخد؟", listOf("ماه", "خورشید", "مریخ"), 1),
                BodyQuestion("زنبور عسل چه می‌سازد؟", listOf("ابریشم", "عسل", "شیر"), 1),
                BodyQuestion("بلندترین کوه ایران کدام است؟", listOf("دماوند", "سهند", "الوند"), 0),
                BodyQuestion("کدام از مواد بازیافتی است؟", listOf("بطری پلاستیکی", "پوست موز", "برگ خشک"), 0),
            )
            facts.random(random).let { q -> val order = q.answers.indices.shuffled(random); BodyQuestion(q.prompt, order.map { q.answers[it] }, order.indexOf(q.correct)) }
        }
        else -> {
            val a = random.nextInt(2, 6 + level.coerceAtMost(5))
            val b = random.nextInt(2, 10)
            val (o, c) = options(a * b, 6)
            BodyQuestion("⁦${a.fa()} × ${b.fa()} = ؟⁩", o, c)
        }
    }
}

@Composable
fun BodyQuizGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val space = LocalKidSpace.current
    val tracker = remember { PoseTracker() }
    val age = space.child.ageYears()
    val total = 10
    val need = 7
    var index by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf(bodyQuestion(age, level)) }
    var correct by remember { mutableIntStateOf(0) }
    var zone by remember { mutableIntStateOf(1) }
    var lockIn by remember { mutableFloatStateOf(0f) }
    var answerTime by remember { mutableFloatStateOf(0f) }
    var feedback by remember { mutableStateOf<Boolean?>(null) }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val finish by rememberUpdatedState(onFinish)
    val thinkSeconds = (9f - level * 0.4f).coerceAtLeast(5f)

    DisposableEffect(Unit) { GameMusic.play(GameMusic.Theme.CALM); onDispose { GameMusic.release() } }
    LaunchedEffect(index) { space.voice.speak(question.prompt.replace(Regex("[⭐🔴🟥🔺]"), "")) }
    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val pose = tracker.pose ?: return@collect
            val ls = pose.visible(PoseLandmark.LEFT_SHOULDER) ?: return@collect
            val rs = pose.visible(PoseLandmark.RIGHT_SHOULDER) ?: return@collect
            val x = (ls.x + rs.x) / 2f
            zone = when { x < 0.38f -> 0; x > 0.62f -> 2; else -> 1 }
        }
    }
    LaunchedEffect(index) {
        var last = withInfiniteAnimationFrameMillis { it }
        answerTime = 0f
        lockIn = 0f
        var lastZone = zone
        delay(1_500) // read the question first
        while (true) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            answerTime += dt
            if (zone != lastZone) { lastZone = zone; lockIn = 0f; GameSfx.whoosh() }
            lockIn += dt / 1.6f
            // the answer locks after standing 1.6 s in a zone (but only once the child had a moment to think)
            if ((lockIn >= 1f && answerTime > 2.5f) || answerTime > thinkSeconds + 1.6f) {
                val ok = zone == question.correct
                if (ok) { correct++; GameSfx.sparkle() } else GameSfx.oops()
                feedback = ok
                delay(1_300)
                feedback = null
                if (index + 1 >= total) { finish(correct * 3, correct >= need); return@LaunchedEffect }
                question = bodyQuestion(age, level)
                index++
                return@LaunchedEffect
            }
        }
    }

    val measurer = rememberTextMeasurer()
    PoseStage(tracker, "🧠 سؤال ${(index + 1).fa()} از ${total.fa()}   ✅ ${correct.fa()}   به سمت جواب درست برو و بایست!", onSize = { stageSize = it }) {
        val w = size.width
        val h = size.height
        val q = measurer.measure(question.prompt, TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center), constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w * 0.92f).toInt()))
        drawRoundRect(Color(0xFF283593).copy(alpha = 0.85f), Offset(w * 0.03f, 10f), Size(w * 0.94f, q.size.height + 24f), CornerRadius(22f))
        drawText(q, topLeft = Offset(w / 2 - q.size.width / 2f, 22f))
        val colors = listOf(Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5))
        for (i in 0 until 3) {
            val x0 = w * i / 3f
            val selected = zone == i
            val fill = when {
                feedback != null && i == question.correct -> Color(0xFF66BB6A)
                feedback == false && selected -> Color(0xFFE53935)
                else -> colors[i]
            }
            drawRoundRect(fill.copy(alpha = if (selected) 0.55f else 0.25f), Offset(x0 + 6f, h * 0.62f), Size(w / 3f - 12f, h * 0.34f), CornerRadius(24f))
            if (selected && feedback == null) drawRoundRect(Color.White, Offset(x0 + 6f, h * 0.62f), Size((w / 3f - 12f) * lockIn.coerceIn(0f, 1f), 10f), CornerRadius(5f))
            val a = measurer.measure(question.answers[i], TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center), constraints = androidx.compose.ui.unit.Constraints(maxWidth = (w / 3f - 20f).toInt()))
            drawText(a, topLeft = Offset(x0 + w / 6f - a.size.width / 2f, h * 0.79f - a.size.height / 2f))
        }
        feedback?.let {
            val f = measurer.measure(if (it) "آفرین! 🎉" else "جواب درست سبز است 💚", TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White))
            drawText(f, topLeft = Offset(w / 2 - f.size.width / 2f, h * 0.42f))
        }
    }
}

// =====================================================================================================================
// «قهرمان ریتم»: notes fly to four targets on the beat of the music; hit them with your hands at the right moment.
// =====================================================================================================================

private data class Note(val lane: Int, val hitAt: Float, var judged: Boolean = false)
private data class Judge(val text: String, val color: Color, val life: Float, val lane: Int)

@Composable
fun RhythmGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val tracker = remember { PoseTracker() }
    val notes = remember { mutableStateListOf<Note>() }
    val judges = remember { mutableStateListOf<Judge>() }
    var hands by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var clock by remember { mutableFloatStateOf(-2f) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var perfect by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    val finish by rememberUpdatedState(onFinish)
    val theme = if (level < 4) GameMusic.Theme.FOREST else if (level < 8) GameMusic.Theme.PARTY else GameMusic.Theme.HERO
    val beat = 60f / theme.bpm
    val travel = (2.2f - level * 0.1f).coerceAtLeast(1.2f)
    // four targets: upper-left, upper-right, lower-left, lower-right (all reachable while standing)
    val targets = listOf(Offset(0.22f, 0.3f), Offset(0.78f, 0.3f), Offset(0.2f, 0.62f), Offset(0.8f, 0.62f))
    val laneColors = listOf(Color(0xFFFF4081), Color(0xFF40C4FF), Color(0xFFFFD740), Color(0xFF69F0AE))
    val songBeats = 48 + level * 4

    remember {
        // chart: notes on beats; later levels add off-beat notes and doubles
        var lastLane = -1
        for (b in 0 until songBeats) {
            val dense = level >= 5 && b % 2 == 1 && Random.nextFloat() < 0.3f
            if (b % 2 == 0 || dense || (level >= 3 && Random.nextFloat() < 0.35f)) {
                var lane = Random.nextInt(4)
                if (lane == lastLane) lane = (lane + 1) % 4
                lastLane = lane
                notes += Note(lane, b * beat)
                if (level >= 7 && Random.nextFloat() < 0.15f) notes += Note((lane + 1) % 4, b * beat)
            }
        }
        true
    }
    DisposableEffect(Unit) { GameMusic.play(theme); onDispose { GameMusic.release() } }
    LaunchedEffect(Unit) {
        snapshotFlow { tracker.frames }.collect {
            val s = stageSize
            if (s == IntSize.Zero) return@collect
            hands = handsOnStage(tracker.pose, s).map { Offset(it.x / s.width, it.y / s.height) }
        }
    }
    LaunchedEffect(Unit) {
        var last = withInfiniteAnimationFrameMillis { it }
        val end = songBeats * beat + 2f
        while (clock < end) {
            val now = withInfiniteAnimationFrameMillis { it }
            val dt = (now - last).coerceAtMost(50) / 1000f
            last = now
            clock += dt
            val aspect = if (stageSize.width > 0) stageSize.height / stageSize.width.toFloat() else 1f
            for (i in notes.indices) {
                val n = notes[i]
                if (n.judged) continue
                val diff = clock - n.hitAt
                val t = targets[n.lane]
                val touching = hands.any { hypot(it.x - t.x, (it.y - t.y) * aspect) < 0.11f }
                if (touching && abs(diff) < 0.35f) {
                    val isPerfect = abs(diff) < 0.15f
                    notes[i] = n.copy(judged = true)
                    combo++
                    best = maxOf(best, combo)
                    score += (if (isPerfect) 3 else 2) + combo / 10
                    if (isPerfect) perfect++
                    judges += Judge(if (isPerfect) "عالی!" else "خوب", if (isPerfect) Color(0xFFFFEB3B) else Color.White, 0.8f, n.lane)
                    GameSfx.beep(high = isPerfect)
                } else if (diff > 0.35f) {
                    notes[i] = n.copy(judged = true)
                    combo = 0
                    misses++
                    judges += Judge("از دست رفت", Color(0xFFFF8A80), 0.8f, n.lane)
                }
            }
            for (i in judges.indices.reversed()) {
                val j = judges[i]
                val nj = j.copy(life = j.life - dt)
                if (nj.life <= 0f) judges.removeAt(i) else judges[i] = nj
            }
        }
        val hitRate = (notes.size - misses) / notes.size.toFloat()
        finish(score, hitRate >= 0.6f)
    }

    val measurer = rememberTextMeasurer()
    PoseStage(tracker, "🎵 امتیاز ${score.fa()}   🔥 ترکیب ${combo.fa()} (بهترین ${best.fa()})   ⭐ ${perfect.fa()}", onSize = { stageSize = it }) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF311B92).copy(alpha = 0.35f), Color.Transparent, Color(0xFF880E4F).copy(alpha = 0.35f))))
        // pulsing targets
        val pulse = 1f + 0.08f * kotlin.math.sin((clock / beat) * 2 * Math.PI).toFloat()
        targets.forEachIndexed { lane, t ->
            val c = Offset(w * t.x, h * t.y)
            drawCircle(laneColors[lane].copy(alpha = 0.25f), size.minDimension * 0.09f * pulse, c)
            drawCircle(laneColors[lane], size.minDimension * 0.09f * pulse, c, style = Stroke(6f))
        }
        // notes fly from the centre to their target and arrive exactly on the beat
        val centre = Offset(w * 0.5f, h * 0.45f)
        notes.forEach { n ->
            if (n.judged) return@forEach
            val p = 1f - (n.hitAt - clock) / travel
            if (p < 0f || p > 1.15f) return@forEach
            val t = targets[n.lane]
            val pos = Offset(centre.x + (w * t.x - centre.x) * p, centre.y + (h * t.y - centre.y) * p)
            drawCircle(laneColors[n.lane], size.minDimension * (0.025f + 0.045f * p.coerceAtMost(1f)), pos)
            drawCircle(Color.White.copy(alpha = 0.7f), size.minDimension * (0.012f + 0.02f * p.coerceAtMost(1f)), pos)
        }
        hands.forEach { hnd -> drawCircle(Color.White.copy(alpha = 0.8f), size.minDimension * 0.04f, Offset(w * hnd.x, h * hnd.y), style = Stroke(5f)) }
        judges.forEach { j ->
            val t = targets[j.lane]
            val l = measurer.measure(j.text, TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = j.color.copy(alpha = j.life.coerceIn(0f, 1f))))
            drawText(l, topLeft = Offset(w * t.x - l.size.width / 2f, h * t.y - size.minDimension * 0.16f - (0.8f - j.life) * 40f))
        }
        if (clock < 0f) {
            val c = measurer.measure("آماده باش… 🎶", TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White))
            drawText(c, topLeft = Offset(w / 2 - c.size.width / 2f, h * 0.4f))
        }
    }
}
