package com.kodomo.app.ui.kids.browser

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.EncyclopediaEntry
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.EncyclopediaRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SafeBrowserViewModel @Inject constructor(
    savedState: SavedStateHandle,
    familyRepository: FamilyRepository,
    private val encyclopedia: EncyclopediaRepository,
) : ViewModel() {
    private val childId = savedState.childId
    val query = MutableStateFlow("")
    private val age = MutableStateFlow(18)

    init {
        viewModelScope.launch { familyRepository.getChild(childId)?.let { age.value = it.ageYears(LocalDate.now()) } }
    }

    val results: StateFlow<List<EncyclopediaEntry>> = query.debounce(250).flatMapLatest { q ->
        if (q.isBlank()) encyclopedia.observeTopics(age.value) else encyclopedia.search(q, age.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class EncyclopediaArticleViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val encyclopedia: EncyclopediaRepository,
    private val activityRepository: ActivityRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val childId = savedState.childId
    val entry = MutableStateFlow<EncyclopediaEntry?>(null)

    fun load(id: Int) = viewModelScope.launch {
        entry.value = encyclopedia.get(id)
        if (activityRepository.countCompleted(childId, ActivityCategory.BROWSER, minSeconds = 15) >= 5) {
            rewardRepository.award(childId, AchievementCatalog.EXPLORER)
        }
    }
}

/** «مرورگر امن من»: searches only the curated offline encyclopedia; there is no WebView and no open-web access. */
@Composable
fun SafeBrowserScreen(onBack: () -> Unit, onOpen: (Int) -> Unit, viewModel: SafeBrowserViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    TrackActivity(ActivityCategory.BROWSER, "مرورگر امن")

    KidScreenScaffold(title = "مرورگر امن من", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it.take(60) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("دنبال چی می‌گردی؟ مثلاً «فضا»") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.padding(top = 6.dp).background(KodomoColors.Mint, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = KodomoColors.Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("جستجوی امن و کنترل‌شده • فقط مطالب تأییدشده‌ی کدومو", style = MaterialTheme.typography.labelSmall, color = KodomoColors.GreenDark)
                }
            }
            if (query.isBlank()) item { MascotSpeech("هر سؤالی داری بپرس! جواب‌ها را کارشناسان کدومو بررسی کرده‌اند.", MascotMood.THINKING) }
            if (results.isEmpty() && query.isNotBlank()) item { EmptyState("چیزی پیدا نکردم! یک کلمه‌ی دیگر امتحان کن 🤔") }
            items(results, key = { it.id }) { entry ->
                KodomoCard(onClick = {
                    space.guide(entry.title)
                    onOpen(entry.id)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.emoji, fontSize = 40.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(entry.topic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(entry.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EncyclopediaArticleScreen(articleId: Int, onBack: () -> Unit, viewModel: EncyclopediaArticleViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    LaunchedEffect(articleId) { viewModel.load(articleId) }
    TrackActivity(ActivityCategory.BROWSER, entry?.title ?: "مقاله", "encyclopedia_$articleId")

    KidScreenScaffold(title = entry?.title.orEmpty(), onBack = onBack) { padding ->
        val e = entry ?: return@KidScreenScaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(e.emoji, fontSize = 96.sp)
            Text(e.title, style = MaterialTheme.typography.headlineMedium)
            Text(e.summary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
            KodomoCard(Modifier.fillMaxWidth()) {
                Text(e.body, style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(onClick = { space.voice.speak("${e.title}. ${e.body}") }) { Text("🔊 برایم بخوان") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
