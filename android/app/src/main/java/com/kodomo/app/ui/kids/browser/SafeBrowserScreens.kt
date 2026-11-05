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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kodomo.app.ui.components.bouncyClick

@OptIn(FlowPreview::class)
@HiltViewModel
class SafeBrowserViewModel @Inject constructor(
    savedState: SavedStateHandle,
    familyRepository: FamilyRepository,
    private val encyclopedia: EncyclopediaRepository,
    private val sitesStore: com.kodomo.app.data.prefs.SafeSitesStore,
    contentRepository: com.kodomo.app.domain.repository.ContentRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val sites = sitesStore.sites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun requestSite(host: String) = viewModelScope.launch { sitesStore.request(host) }

    val bookmarks = sitesStore.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun toggleBookmark(url: String, title: String) = viewModelScope.launch { sitesStore.toggleBookmark(url, title) }
    fun logVisit(url: String, title: String) = viewModelScope.launch { if (url.startsWith("http")) sitesStore.logVisit(url, title) }

    init { viewModelScope.launch { sitesStore.seedDefaults() } }

    /** Kodomo's own pages (discoveries, lessons, books) that match the search – part of the «safe web». */
    val pageResults: StateFlow<List<com.kodomo.app.domain.model.ContentItem>> = kotlinx.coroutines.flow.combine(
        contentRepository.observeAllowedForChild(savedState.childId, com.kodomo.app.domain.model.ContentType.EXPLORE),
        contentRepository.observeAllowedForChild(savedState.childId, com.kodomo.app.domain.model.ContentType.LEARNING),
        contentRepository.observeAllowedForChild(savedState.childId, com.kodomo.app.domain.model.ContentType.BOOK),
        query.debounce(250),
    ) { a, b, c, q ->
        if (q.isBlank()) emptyList() else (a + b + c).filter { it.title.contains(q.trim()) || it.description.contains(q.trim()) || it.category.contains(q.trim()) }.take(20)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val childId = savedState.childId
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

/**
 * «مرورگر امن من»: a real browser feel – address bar, back/forward/reload/home, a start page with search and approved
 * sites – but it only ever opens Kodomo's own pages and websites the parent approved. Any other address shows a
 * friendly «not approved yet» page with a button that asks the parent.
 */
@Composable
fun SafeBrowserScreen(onBack: () -> Unit, onOpen: (Int) -> Unit, onOpenRoute: (String) -> Unit = {}, viewModel: SafeBrowserViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val pages by viewModel.pageResults.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    var currentTitle by remember { androidx.compose.runtime.mutableStateOf("") }
    TrackActivity(ActivityCategory.BROWSER, "مرورگر امن")

    var address by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var openSite by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var currentUrl by remember { androidx.compose.runtime.mutableStateOf("") }
    var blocked by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var requested by remember { androidx.compose.runtime.mutableStateOf(false) }
    var web by remember { androidx.compose.runtime.mutableStateOf<android.webkit.WebView?>(null) }
    var loading by remember { androidx.compose.runtime.mutableStateOf(false) }
    val hosts = sites.map { it.host.substringBefore('/') }

    fun go(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        val looksLikeAddress = text.contains('.') && !text.contains(' ')
        if (looksLikeAddress) {
            val host = com.kodomo.app.data.prefs.SafeSite.normalizeHost(text).substringBefore('/')
            if (hosts.any { host == it || host.endsWith(".$it") }) {
                blocked = null
                openSite = "https://" + com.kodomo.app.data.prefs.SafeSite.normalizeHost(text)
            } else {
                openSite = null
                blocked = host
                requested = false
            }
        } else {
            openSite = null
            blocked = null
            viewModel.query.value = text.take(60)
        }
    }

    androidx.activity.compose.BackHandler(enabled = openSite != null || blocked != null) {
        when {
            web?.canGoBack() == true -> web?.goBack()
            else -> { openSite = null; blocked = null; address = "" }
        }
    }

    KidScreenScaffold(title = "مرورگر امن من", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ---- toolbar ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { web?.takeIf { it.canGoBack() }?.goBack() ?: run { openSite = null; blocked = null } }) { Text("→", fontSize = 22.sp) }
                androidx.compose.material3.IconButton(onClick = { web?.takeIf { it.canGoForward() }?.goForward() }) { Text("←", fontSize = 22.sp) }
                OutlinedTextField(
                    value = if (openSite != null && address.isEmpty()) android.net.Uri.parse(currentUrl.ifBlank { openSite }).host.orEmpty() else address,
                    onValueChange = { address = it.take(120) },
                    leadingIcon = { Text(if (openSite != null) "🔒" else "🔎") },
                    placeholder = { Text("جستجو یا آدرس سایت") },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { go(address); address = "" }),
                    modifier = Modifier.weight(1f),
                )
                if (openSite != null) {
                    val marked = bookmarks.any { it.first == currentUrl }
                    androidx.compose.material3.IconButton(onClick = { if (currentUrl.isNotBlank()) viewModel.toggleBookmark(currentUrl, currentTitle) }) { Text(if (marked) "⭐" else "☆", fontSize = 22.sp) }
                }
                androidx.compose.material3.IconButton(onClick = { if (openSite != null) web?.reload() else go(address) }) { Text("⟳", fontSize = 22.sp) }
                androidx.compose.material3.IconButton(onClick = { openSite = null; blocked = null; address = ""; viewModel.query.value = "" }) { Text("🏠", fontSize = 20.sp) }
            }
            if (loading && openSite != null) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).background(KodomoColors.Mint, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = KodomoColors.Green, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("اینترنت امن کدومو • فقط سایت‌های تأییدشده‌ی والدین", style = MaterialTheme.typography.labelSmall, color = KodomoColors.GreenDark)
            }

            val site = openSite
            val blockedHost = blocked
            when {
                site != null -> {
                    androidx.compose.runtime.key(site, hosts) {
                        com.kodomo.app.ui.kids.video.LockedWebPlayer(
                            lock = com.kodomo.app.ui.kids.video.WebLock.SameSite(site, hosts),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            onPageChanged = { url, title -> currentUrl = url; currentTitle = title; loading = false; viewModel.logVisit(url, title) },
                            onWebView = { web = it; loading = true },
                        )
                    }
                }
                blockedHost != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    ) {
                        Text("🙈", fontSize = 80.sp)
                        Text("سایت «$blockedHost» هنوز تأیید نشده است.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text("می‌توانی از پدر یا مادرت بخواهی آن را بررسی کنند.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        if (requested) {
                            Text("✅ درخواستت فرستاده شد!", style = MaterialTheme.typography.titleMedium, color = KodomoColors.Green)
                        } else {
                            androidx.compose.material3.Button(onClick = { viewModel.requestSite(blockedHost); requested = true; space.celebrate() }) { Text("از مامان و بابا بپرس 🙋") }
                        }
                        OutlinedButton(onClick = { blocked = null }) { Text("برگشت به صفحه‌ی اول") }
                    }
                }
                else -> StartPage(
                    query = query,
                    sites = sites,
                    bookmarks = bookmarks,
                    onBookmark = { url -> blocked = null; openSite = url },
                    onTopic = { t -> viewModel.query.value = t },
                    results = results,
                    pages = pages,
                    onSite = { s -> space.guide(s.title); blocked = null; openSite = s.homeUrl },
                    onArticle = { e -> space.guide(e.title); onOpen(e.id) },
                    onPage = { item -> space.guide(item.title); onOpenRoute(item.id) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StartPage(
    query: String,
    sites: List<com.kodomo.app.data.prefs.SafeSite>,
    bookmarks: List<Pair<String, String>>,
    onBookmark: (String) -> Unit,
    onTopic: (String) -> Unit,
    results: List<EncyclopediaEntry>,
    pages: List<com.kodomo.app.domain.model.ContentItem>,
    onSite: (com.kodomo.app.data.prefs.SafeSite) -> Unit,
    onArticle: (EncyclopediaEntry) -> Unit,
    onPage: (com.kodomo.app.domain.model.ContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (query.isBlank()) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("کدومو", style = MaterialTheme.typography.displaySmall, color = KodomoColors.Green)
                    Text("جست‌وجوی امن برای بچه‌ها", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (sites.isNotEmpty()) {
                item { Text("سایت‌های من", style = MaterialTheme.typography.titleMedium) }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sites.size) { i ->
                            val s = sites[i]
                            val palette = listOf(0xFF5AA9E6 to 0xFF1E5AA8, 0xFFF28AA6 to 0xFFB83265, 0xFFFFB648 to 0xFFE07B00, 0xFF5FB98A to 0xFF2E7D5B, 0xFF9B7FE6 to 0xFF5E3FB5)[i % 5]
                            Column(
                                Modifier
                                    .size(width = 112.dp, height = 120.dp)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(palette.first), androidx.compose.ui.graphics.Color(palette.second))), RoundedCornerShape(22.dp))
                                    .bouncyClick { onSite(s) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(s.emoji, fontSize = 40.sp)
                                Text(s.title, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            } else {
                item { Text("هنوز سایتی تأیید نشده؛ پدر و مادرت می‌توانند از پنل والدین سایت‌های امن را اضافه کنند.", style = MaterialTheme.typography.bodySmall) }
            }
            if (bookmarks.isNotEmpty()) {
                item { Text("⭐ نشانک‌های من", style = MaterialTheme.typography.titleMedium) }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks.size) { i ->
                            val (url, title) = bookmarks[i]
                            androidx.compose.material3.AssistChip(onClick = { onBookmark(url) }, label = { Text(title, maxLines = 1) }, leadingIcon = { Text("⭐") })
                        }
                    }
                }
            }
            item { Text("🔎 درباره‌ی چه چیزی می‌خواهی بدانی؟", style = MaterialTheme.typography.titleMedium) }
            item {
                val topics = listOf("🚀 فضا", "🦁 حیوانات", "🌋 طبیعت", "❤️ بدن ما", "🧲 علم", "🦖 دایناسور", "🌈 رنگین‌کمان", "🐝 زنبور")
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topics.forEach { t -> androidx.compose.material3.SuggestionChip(onClick = { onTopic(t.substringAfter(' ')) }, label = { Text(t) }) }
                }
            }
            item { MascotSpeech("هر سؤالی داری بپرس! جواب‌ها را کارشناسان کدومو بررسی کرده‌اند.", MascotMood.THINKING) }
            item { Text("موضوع‌های دانشنامه", style = MaterialTheme.typography.titleMedium) }
        } else {
            if (results.isEmpty() && pages.isEmpty()) item { EmptyState("چیزی پیدا نکردم! یک کلمه‌ی دیگر امتحان کن 🤔") }
            if (pages.isNotEmpty()) item { Text("صفحه‌های کدومو", style = MaterialTheme.typography.titleMedium) }
            items(pages, key = { "p_" + it.id }) { item ->
                KodomoCard(onClick = { onPage(item) }) {
                    Text("kodomo › ${item.type.label} › ${item.category}", style = MaterialTheme.typography.labelSmall, color = KodomoColors.Green)
                    Text("${item.emoji} ${item.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            val matchingSites = sites.filter { it.title.contains(query.trim()) || it.host.contains(query.trim()) }
            items(matchingSites, key = { "s_" + it.host }) { s ->
                KodomoCard(onClick = { onSite(s) }) {
                    Text(s.host, style = MaterialTheme.typography.labelSmall, color = KodomoColors.Green)
                    Text("${s.emoji} ${s.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (results.isNotEmpty()) item { Text("دانشنامه", style = MaterialTheme.typography.titleMedium) }
        }
        items(results, key = { "e_" + it.id }) { entry ->
            KodomoCard(onClick = { onArticle(entry) }) {
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
            OutlinedButton(onClick = { space.voice.speakAll(listOf(e.title, e.body)) }) { Text("🔊 برایم بخوان") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
