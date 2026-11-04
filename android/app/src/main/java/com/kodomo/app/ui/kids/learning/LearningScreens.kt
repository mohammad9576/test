package com.kodomo.app.ui.kids.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningViewModel @Inject constructor(savedState: SavedStateHandle, contentRepository: ContentRepository) : ViewModel() {
    val category = MutableStateFlow<String?>(null)
    private val allowed = contentRepository.observeAllowedForChild(savedState.childId, ContentType.LEARNING)
    val categories: StateFlow<List<String>> = combine(allowed, category) { items, _ -> items.map { it.category }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lessons: StateFlow<List<ContentItem>> = combine(allowed, category) { items, c -> items.filter { c == null || it.category == c } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val familyRepository: FamilyRepository,
    private val rewardRepository: RewardRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val childId = savedState.childId
    private val contentId: String = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())
    val lesson = MutableStateFlow<ContentItem?>(null)
    val steps = MutableStateFlow<List<ContentPage>>(emptyList())
    val completed = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            lesson.value = contentRepository.getItem(contentId)
            steps.value = contentRepository.getPages(contentId)
        }
    }

    fun complete() {
        if (completed.value) return
        completed.value = true
        viewModelScope.launch {
            familyRepository.addStars(childId, 4)
            if (activityRepository.countCompleted(childId, ActivityCategory.LEARNING, minSeconds = 60) >= 3) {
                rewardRepository.award(childId, AchievementCatalog.SCIENTIST)
            }
        }
    }
}

@Composable
fun LearningScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: LearningViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    TrackActivity(ActivityCategory.LEARNING, "آموزش و یادگیری")
    val preTeen = space.spec.group == AgeGroup.PRE_TEEN

    KidScreenScaffold(title = if (preTeen) "تفکر و یادگیری" else "آموزش و یادگیری", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MascotSpeech(
                    if (preTeen) "امروز مثل یک دانشمند فکر کنیم: سؤال بپرس، آزمایش کن، نتیجه بگیر! 🧠" else "یادگیری یعنی بازی، تجربه و خلاقیت! 🔬",
                    MascotMood.THINKING,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = category == null, onClick = { viewModel.category.value = null }, label = { Text("همه") }) }
                    items(categories) { c -> FilterChip(selected = category == c, onClick = { viewModel.category.value = c }, label = { Text(c) }) }
                }
            }
            if (lessons.isEmpty()) item { EmptyState("درسی برای این دسته پیدا نشد.") }
            items(lessons, key = { it.id }) { lesson ->
                KodomoCard(onClick = {
                    space.guide(lesson.title)
                    onOpen(lesson.id)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiCover(lesson.emoji, lesson.colorStart, lesson.colorEnd, Modifier.size(80.dp), emojiSize = 42)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(lesson.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Text("⏱ ${lesson.durationMinutes.fa()} دقیقه", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(contentId: String, onBack: () -> Unit, viewModel: LessonViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    var index by remember { mutableIntStateOf(0) }
    TrackActivity(ActivityCategory.LEARNING, lesson?.title ?: "درس", contentId)

    KidScreenScaffold(title = lesson?.title ?: "", onBack = onBack) { padding ->
        val item = lesson
        if (item == null || steps.isEmpty()) {
            EmptyState("در حال آماده‌سازی درس...", Modifier.padding(padding))
            return@KidScreenScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(progress = { (index + 1) / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Text("گام ${(index + 1).fa()} از ${steps.size.fa()}", style = MaterialTheme.typography.labelLarge)
            val step = steps[index]
            EmojiCover(step.emoji, item.colorStart, item.colorEnd, Modifier.fillMaxWidth().height(200.dp), emojiSize = 96, corner = 30.dp)
            Text(step.text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            OutlinedButton(onClick = { space.voice.speak(step.text) }) { Text("🔊 برایم بخوان") }
            if (completed) {
                androidx.compose.runtime.LaunchedEffect(Unit) { space.celebrate() }
                MascotSpeech("آفرین! این درس را کامل کردی و ۴ ستاره گرفتی ⭐", MascotMood.EXCITED)
                KodomoPrimaryButton("بازگشت", onClick = onBack)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
                    KodomoPrimaryButton(
                        text = if (index == steps.lastIndex) "انجامش دادم! ✅" else "گام بعدی",
                        onClick = { if (index < steps.lastIndex) index++ else viewModel.complete() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
