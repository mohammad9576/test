package com.kodomo.app.ui.kids.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.prefs.LearningStore
import com.kodomo.app.data.prefs.StudyReward
import com.kodomo.app.data.prefs.StudyToday
import com.kodomo.app.domain.learn.Curriculum
import com.kodomo.app.domain.learn.Exercise
import com.kodomo.app.domain.learn.Grade
import com.kodomo.app.domain.learn.StudyUnit
import com.kodomo.app.domain.learn.Subject
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.kiosk.ScreenTimeManager
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.fx.GameSfx
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class StudyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val store: LearningStore,
    private val screenTime: ScreenTimeManager,
    private val family: FamilyRepository,
) : ViewModel() {
    val childId = savedState.childId
    val unitId: String = savedState.get<String>(Routes.ARG_ID).orEmpty().let(android.net.Uri::decode)
    private val age = kotlinx.coroutines.flow.MutableStateFlow(7)
    private var childName = ""

    init {
        viewModelScope.launch { family.getChild(childId)?.let { age.value = it.ageYears(); childName = it.name } }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val grade = age.flatMapLatest { store.grade(childId, it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Grade.G1)
    val stars = store.stars(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val today = store.today(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyToday(0, 0, 0))
    val reward = store.reward.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyReward())

    /** A correct answer: counts towards the play-time reward; returns the minutes granted right now (or null). */
    fun correct(onReward: (Int) -> Unit) = viewModelScope.launch {
        store.recordCorrect(childId, childName)?.let { minutes ->
            screenTime.grantExtraMinutes(minutes)
            onReward(minutes)
        }
    }

    fun finishUnit(unit: String, correct: Int, total: Int) = viewModelScope.launch {
        val s = when {
            correct >= total * 0.9 -> 3
            correct >= total * 0.7 -> 2
            correct >= total * 0.5 -> 1
            else -> 0
        }
        store.saveStars(childId, unit, s)
        family.addStars(childId, s)
    }
}

/** Progress towards the next play-time reward, shown on every study screen. */
@Composable
internal fun RewardBar(today: StudyToday, reward: StudyReward) {
    val into = today.correct % reward.every
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))), RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Text(
            "🎮 هر ${reward.every.fa()} جواب درست = ${reward.minutes.fa()} دقیقه بازی بیشتر   •   امروز: ${today.correct.fa()} ✅",
            style = MaterialTheme.typography.labelLarge,
        )
        LinearProgressIndicator(progress = { into / reward.every.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = Color(0xFFFB8C00))
        Text("${(reward.every - into).fa()} جواب درست دیگر تا جایزه" + if (today.rewardsPending > 0) " • ${today.rewardsPending.fa()} جایزه منتظر تأیید والدین" else "", style = MaterialTheme.typography.labelSmall)
    }
}

private val subjectColors = mapOf(
    Subject.MATH to (0xFF5AA9E6 to 0xFF1E5AA8), Subject.PERSIAN to (0xFFF28AA6 to 0xFFB83265), Subject.SCIENCE to (0xFF5FB98A to 0xFF2E7D5B),
    Subject.SOCIAL to (0xFFFFB648 to 0xFFE07B00), Subject.SKILLS to (0xFF9B7FE6 to 0xFF5E3FB5),
)

@Composable
fun StudyHomeScreen(onBack: () -> Unit, onOpenUnit: (String) -> Unit, onEnglish: () -> Unit, viewModel: StudyViewModel = hiltViewModel()) {
    val grade by viewModel.grade.collectAsStateWithLifecycle()
    val stars by viewModel.stars.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val reward by viewModel.reward.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableStateOf<Subject?>(null) }
    TrackActivity(ActivityCategory.LEARNING, "درس و تمرین")
    val bySubject = remember(grade) { Curriculum.bySubject(grade) }
    val subjects = bySubject.keys.toList()
    val shown = (subject?.let { bySubject[it] } ?: bySubject.values.flatten())

    KidScreenScaffold(title = "درس و تمرین • ${grade.label}", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { RewardBar(today, reward) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7E57C2), Color(0xFF26C6DA))), RoundedCornerShape(22.dp))
                        .bouncyClick(onEnglish)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔤", fontSize = 40.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("انگلیسی از صفر", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("Hello! الفبا، اعداد، رنگ‌ها، حیوانات… با تلفظ", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = subject == null, onClick = { subject = null }, label = { Text("همه") }) }
                    items(subjects.size) { i ->
                        val s = subjects[i]
                        FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text("${s.emoji} ${s.label}") })
                    }
                }
            }
            items(shown, key = { it.id }) { unit -> UnitTile(unit, stars[unit.id] ?: 0, current = unit.grade == grade) { onOpenUnit(unit.id) } }
        }
    }
}

@Composable
private fun UnitTile(unit: StudyUnit, stars: Int, current: Boolean, onClick: () -> Unit) {
    val colors = subjectColors[unit.subject] ?: (0xFF90A4AE to 0xFF546E7A)
    Column(
        Modifier
            .aspectRatio(1.05f)
            .background(Brush.verticalGradient(listOf(Color(colors.first), Color(colors.second))), RoundedCornerShape(24.dp))
            .bouncyClick(onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(unit.emoji, fontSize = 34.sp)
            Spacer(Modifier.weight(1f))
            Text(if (current) unit.grade.short else "مرور", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Text(unit.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text("${unit.subject.label}  " + "⭐".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** A unit: lesson cards, then 10 practice questions with explanations, then stars and the reward. */
@Composable
fun StudyUnitScreen(onBack: () -> Unit, viewModel: StudyViewModel = hiltViewModel()) {
    val unit = remember { Curriculum.byId(viewModel.unitId) }
    val today by viewModel.today.collectAsStateWithLifecycle()
    val reward by viewModel.reward.collectAsStateWithLifecycle()
    if (unit == null) { onBack(); return }
    TrackActivity(ActivityCategory.LEARNING, unit.title, unit.id)
    var practicing by rememberSaveable { mutableStateOf(false) }
    KidScreenScaffold(title = "${unit.emoji} ${unit.title}", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RewardBar(today, reward)
            if (!practicing) LessonCards(unit) { practicing = true }
            else PracticeSession(
                make = unit.make,
                onCorrect = { viewModel.correct(it) },
                onDone = { c, t -> viewModel.finishUnit(unit.id, c, t) },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun LessonCards(unit: StudyUnit, onPractice: () -> Unit) {
    val space = LocalKidSpace.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val card = unit.lesson[page]
    LaunchedEffect(page) { space.voice.speak(card.text) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("📘 درس • کارت ${(page + 1).fa()} از ${unit.lesson.size.fa()}", style = MaterialTheme.typography.labelLarge)
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color.White)), RoundedCornerShape(28.dp))
                .border(2.dp, Color(0xFF90CAF9), RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(card.emoji, fontSize = 72.sp)
            Text(card.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page > 0) androidx.compose.material3.OutlinedButton(onClick = { page-- }) { Text("قبلی") }
            androidx.compose.material3.OutlinedButton(onClick = { space.voice.speak(card.text) }) { Text("🔊 دوباره بخوان") }
            if (page < unit.lesson.lastIndex) androidx.compose.material3.Button(onClick = { page++ }) { Text("بعدی") }
        }
        if (page == unit.lesson.lastIndex) KodomoPrimaryButton("✏️ شروع تمرین (۱۰ سؤال)", onClick = onPractice, modifier = Modifier.fillMaxWidth())
        else androidx.compose.material3.TextButton(onClick = onPractice) { Text("بلدم! برو سراغ تمرین ⏭") }
    }
}

/** Ten multiple-choice questions; wrong answers show the explanation and can be retried once. */
@Composable
internal fun PracticeSession(
    make: (Random) -> Exercise,
    onCorrect: ((Int) -> Unit) -> Unit,
    onDone: (correct: Int, total: Int) -> Unit,
    onBack: () -> Unit,
    total: Int = 10,
    speakPrompt: Boolean = true,
    promptOverride: ((Exercise) -> Unit)? = null,
) {
    val space = LocalKidSpace.current
    val random = remember { Random(System.nanoTime()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var exercise by remember { mutableStateOf(make(random)) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var rewardMsg by remember { mutableStateOf<String?>(null) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(index) { if (speakPrompt) space.voice.speak(exercise.prompt.replace(Regex("[\\u2066\\u2069]"), "")) ; promptOverride?.invoke(exercise) }

    if (finished) {
        LaunchedEffect(Unit) { onDone(correct, total); GameSfx.cheer(); space.celebrate() }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MascotSpeech(if (correct >= total * 0.7) "آفرین! تو این درس را خوب یاد گرفتی! 🎉" else "خوب بود! یک بار دیگر تمرین کن تا قوی‌تر شوی 💪", if (correct >= 7) MascotMood.EXCITED else MascotMood.HAPPY)
            Text("${correct.fa()} از ${total.fa()} درست", style = MaterialTheme.typography.headlineMedium)
            val s = when { correct >= total * 0.9 -> 3; correct >= total * 0.7 -> 2; correct >= total * 0.5 -> 1; else -> 0 }
            Text("⭐".repeat(s) + "☆".repeat(3 - s), fontSize = 40.sp)
            rewardMsg?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE65100), textAlign = TextAlign.Center) }
            KodomoPrimaryButton("🔁 دوباره تمرین کن", onClick = { index = 0; correct = 0; chosen = null; exercise = make(random); finished = false }, modifier = Modifier.fillMaxWidth())
            androidx.compose.material3.OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("برگشت") }
        }
        return
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("سؤال ${(index + 1).fa()} از ${total.fa()}   •   ✅ ${correct.fa()}", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { index / total.toFloat() }, modifier = Modifier.fillMaxWidth())
        Box(
            Modifier.fillMaxWidth().background(Color(0xFF283593), RoundedCornerShape(24.dp)).bouncyClick { promptOverride?.invoke(exercise) }.padding(20.dp).scale(pop.value),
            contentAlignment = Alignment.Center,
        ) { Text(exercise.prompt, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center) }
        exercise.options.forEachIndexed { i, opt ->
            val state = when {
                chosen == null -> 0
                i == exercise.answer -> 1
                i == chosen -> 2
                else -> 3
            }
            val bg = when (state) { 1 -> Color(0xFF66BB6A); 2 -> Color(0xFFEF5350); 3 -> Color(0xFFECEFF1); else -> Color.White }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(bg, RoundedCornerShape(18.dp))
                    .border(2.dp, if (state == 0) Color(0xFF90CAF9) else bg, RoundedCornerShape(18.dp))
                    .bouncyClick {
                        if (chosen != null) return@bouncyClick
                        chosen = i
                        if (i == exercise.answer) {
                            correct++
                            GameSfx.coin()
                            onCorrect { minutes -> rewardMsg = "🎮 جایزه! ${minutes.fa()} دقیقه به زمان بازی‌ات اضافه شد."; GameSfx.levelUp() }
                        } else {
                            GameSfx.oops()
                            space.voice.speak(exercise.explain)
                        }
                    }
                    .padding(16.dp),
            ) { Text(opt, style = MaterialTheme.typography.titleLarge, color = if (state == 1 || state == 2) Color.White else Color(0xFF263238)) }
        }
        if (chosen != null) {
            Text((if (chosen == exercise.answer) "✅ درست است! " else "💡 ") + exercise.explain, style = MaterialTheme.typography.bodyLarge, color = if (chosen == exercise.answer) Color(0xFF2E7D32) else Color(0xFFC62828))
            rewardMsg?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE65100)) }
            KodomoPrimaryButton(if (index + 1 < total) "سؤال بعدی ▶" else "پایان", onClick = {
                if (index + 1 >= total) finished = true else { index++; exercise = make(random); chosen = null }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}
