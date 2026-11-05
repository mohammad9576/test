package com.kodomo.app.ui.kids.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.learn.English
import com.kodomo.app.domain.learn.Exercise
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EnglishVoiceEntryPoint { fun englishVoice(): EnglishVoice }

@Composable
private fun rememberEnglishVoice(): EnglishVoice {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember { EntryPointAccessors.fromApplication(context.applicationContext, EnglishVoiceEntryPoint::class.java).englishVoice() }
}

@Composable
fun EnglishHomeScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: StudyViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val stars by viewModel.stars.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val reward by viewModel.reward.collectAsStateWithLifecycle()
    val voice = rememberEnglishVoice()
    TrackActivity(ActivityCategory.LEARNING, "انگلیسی")
    val age = space.child.ageYears()
    KidScreenScaffold(title = "🔤 انگلیسی از صفر", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { MascotSpeech("Hello! سلام! بیا با هم انگلیسی یاد بگیریم؛ اول کلمه‌ها را گوش کن، بعد بازی کن.", MascotMood.EXCITED) }
            if (!voice.available) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("⚠️ صدای انگلیسی گوشی فعال نیست. از تنظیمات گوشی «تبدیل متن به گفتار» را روی Google و زبان English بگذارید.", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
            }
            item(span = { GridItemSpan(maxLineSpan) }) { RewardBar(today, reward) }
            items(English.units.filter { it.minAge <= age + 1 }, key = { it.id }) { unit ->
                val s = stars["en_" + unit.id] ?: 0
                Column(
                    Modifier
                        .aspectRatio(1.1f)
                        .background(Brush.verticalGradient(listOf(Color(0xFF7E57C2), Color(0xFF26C6DA))), RoundedCornerShape(24.dp))
                        .bouncyClick { onOpen(unit.id) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(unit.emoji, fontSize = 36.sp)
                    Text(unit.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${unit.words.size.fa()} کلمه  " + "⭐".repeat(s) + "☆".repeat(3 - s), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

/** One English unit: learn cards, then «listen & find», «read & find» and (from 7) «spell it». */
@Composable
fun EnglishUnitScreen(onBack: () -> Unit, viewModel: StudyViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val unit = remember { English.byId(viewModel.unitId) }
    val voice = rememberEnglishVoice()
    if (unit == null) { onBack(); return }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOfNotNull("یاد بگیر", "گوش کن و پیدا کن", "بخوان و پیدا کن", if (space.child.ageYears() >= 7) "هجی کن" else null)
    TrackActivity(ActivityCategory.LEARNING, "انگلیسی: ${unit.title}", "en_${unit.id}")
    KidScreenScaffold(title = "${unit.emoji} ${unit.title}", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) { tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) }) } }
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                when (tab) {
                    0 -> LearnWords(unit.words, voice)
                    else -> {
                        val mode = tab
                        androidx.compose.runtime.key(mode) {
                            PracticeSession(
                                make = { r -> englishExercise(unit.words, mode, r) },
                                onCorrect = { cb -> viewModel.correct(cb) },
                                onDone = { c, t -> viewModel.finishUnit("en_" + unit.id, c, t) },
                                onBack = onBack,
                                speakPrompt = mode == 3,
                                promptOverride = { ex -> if (mode == 1) voice.say(ex.explain.substringAfter("«").substringBefore("»")) else if (mode == 2) voice.say(ex.prompt) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Builds a question: 1 = hear the word, pick the picture; 2 = read the word, pick the picture; 3 = spell (pick the right spelling). */
internal fun englishExercise(words: List<com.kodomo.app.domain.learn.Word>, mode: Int, r: kotlin.random.Random): Exercise {
    val w = words.random(r)
    val others = words.filter { it != w }.shuffled(r).take(3)
    return when (mode) {
        1 -> {
            val opts = (others + w).shuffled(r)
            Exercise("🔊 گوش کن! تصویرش کدام است؟ (برای شنیدن دوباره اینجا را بزن)", opts.map { "${it.emoji}  ${it.fa}" }, opts.indexOf(w), "«${w.en}» یعنی ${w.fa} ${w.emoji}")
        }
        2 -> {
            val opts = (others + w).shuffled(r)
            Exercise(w.en, opts.map { "${it.emoji}  ${it.fa}" }, opts.indexOf(w), "«${w.en}» یعنی ${w.fa} ${w.emoji}")
        }
        else -> {
            val word = w.en.lowercase()
            val wrong = buildSet {
                if (word.length > 2) add(word.substring(0, word.length - 2) + word.last() + word[word.length - 2])
                add(word.replaceFirst(word.first().toString(), (word.first() + 1).toString()))
                add(word + word.last())
                add(word.dropLast(1))
            }.filter { it != word && it.isNotBlank() }
            val opts = (wrong.shuffled(r).take(3) + word).shuffled(r)
            Exercise("${w.emoji} ${w.fa} – درست نوشته‌اش کدام است؟", opts, opts.indexOf(word), "درست آن «$word» است.")
        }
    }
}

@Composable
private fun LearnWords(words: List<com.kodomo.app.domain.learn.Word>, voice: EnglishVoice) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    val w = words[i]
    LaunchedEffect(i) { voice.say(w.en) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("کلمه‌ی ${(i + 1).fa()} از ${words.size.fa()}", style = MaterialTheme.typography.labelLarge)
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFEDE7F6), Color.White)), RoundedCornerShape(32.dp))
                .bouncyClick { voice.say(w.en) }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(w.emoji, fontSize = 96.sp)
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(w.en, fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color(0xFF4527A0), textAlign = TextAlign.Center)
            }
            Text(w.fa, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text("🔊 بزن تا دوباره بشنوی", style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = { if (i > 0) i-- }, enabled = i > 0) { Text("قبلی") }
            androidx.compose.material3.Button(onClick = { if (i < words.lastIndex) i++ else i = 0 }) { Text(if (i < words.lastIndex) "بعدی ▶" else "از اول 🔁") }
        }
    }
}
