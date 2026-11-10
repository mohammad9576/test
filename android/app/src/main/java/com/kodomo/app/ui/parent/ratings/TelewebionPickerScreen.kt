package com.kodomo.app.ui.parent.ratings

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.kodoumo.KodoumoClient
import com.kodomo.app.data.kodoumo.KodoumoImporter
import com.kodomo.app.data.kodoumo.KodoumoListItem
import com.kodomo.app.data.kodoumo.KodoumoReview
import com.kodomo.app.domain.model.Episode
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.RatingPill
import com.kodomo.app.ui.kids.video.LockedWebPlayer
import com.kodomo.app.ui.kids.video.WebLock
import com.kodomo.app.ui.kids.video.WebLockRules
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TELEWEBION_KIDS = "https://telewebion.ir/kids"

/** Turns a Telewebion page title such as «انیمیشن فلان - قسمت ۳ | تلوبیون» into a search term for kodoumo. */
internal fun cleanTelewebionTitle(title: String): String {
    var t = title.split('|', '｜').first()
    listOf("تلوبیون", "Telewebion", "آپارات کودک", "آپارات", "فیلیمو", "فیلم‌نت", "فیلم نت", "نماوا", "شیدا", "طاقچه", "فیدیبو", "کتاب صوتی", "دانلود کتاب", "خرید", "پخش زنده", "تماشای آنلاین", "تماشای", "آنلاین", "دانلود", "انیمیشن", "کارتون", "سریال", "فیلم", "کودک")
        .forEach { t = t.replace(it, " ") }
    t = t.replace(Regex("""(قسمت|فصل|اپیزود)\s*\S+"""), " ")
    return t.replace(Regex("""[\-–—:«»"]+"""), " ").replace(Regex("""\s+"""), " ").trim()
}

/**
 * A kodoumo title the parent wants to find on a service (from its rating page): the service opens, and when the
 * parent checks a page the search starts with this name instead of the page title.
 */
object PendingKodoumoPick {
    @Volatile var title: String? = null
}

/** The page's own name and poster: og:title / the main heading beat document.title, which SPA sites rarely update. */
internal const val PAGE_INFO_JS = """
(function(){
  function meta(q){ var m = document.querySelector(q); return m ? (m.content || '').trim() : ''; }
  var h = document.querySelector('h1'); var h1 = h ? (h.innerText || '').trim() : '';
  var t = meta('meta[property="og:title"]') || meta('meta[name="twitter:title"]') || h1 || document.title || '';
  var img = meta('meta[property="og:image"]') || meta('meta[name="twitter:image"]');
  return JSON.stringify({t: t.slice(0, 160), h: h1.slice(0, 160), i: img});
})()
"""

internal data class PageInfo(val title: String, val heading: String, val poster: String?)

internal fun parsePageInfo(raw: String?): PageInfo? = runCatching {
    val inner = kotlinx.serialization.json.Json.parseToJsonElement(raw ?: "null")
    val text = (inner as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return null
    val o = kotlinx.serialization.json.Json.parseToJsonElement(text) as kotlinx.serialization.json.JsonObject
    fun f(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
    PageInfo(f("t"), f("h"), f("i").takeIf { it.startsWith("http") })
}.getOrNull()

/** Collects every episode link visible on a Telewebion series / program page. */
internal const val COLLECT_EPISODES_JS = """
(function(){
  var out = [], seen = {};
  document.querySelectorAll('a[href]').forEach(function(a){
    var h = a.href;
    if (h.indexOf(location.host) >= 0 && h !== location.href && !seen[h]) {
      seen[h] = 1;
      var t = (a.getAttribute('title') || a.getAttribute('aria-label') || a.innerText || '').trim().replace(/\s+/g, ' ').slice(0, 80);
      out.push([h, t]);
    }
  });
  return JSON.stringify(out.slice(0, 300));
})()
"""

/** Parses the evaluateJavascript result (a JSON-encoded string holding a JSON array of [url, title]). */
internal fun parseEpisodeLinks(raw: String?, provider: com.kodomo.app.ui.kids.video.ContentProvider = com.kodomo.app.ui.kids.video.ContentProvider.TELEWEBION): List<Episode> = runCatching {
    val inner = kotlinx.serialization.json.Json.parseToJsonElement(raw ?: "null")
    val text = (inner as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return emptyList()
    (kotlinx.serialization.json.Json.parseToJsonElement(text) as kotlinx.serialization.json.JsonArray).mapNotNull { e ->
        val pair = e as? kotlinx.serialization.json.JsonArray ?: return@mapNotNull null
        val url = (pair.getOrNull(0) as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
        val title = (pair.getOrNull(1) as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        val episodeLike = if (provider == com.kodomo.app.ui.kids.video.ContentProvider.TELEWEBION) "/episode/" in url else provider.isItem(url)
        if (url.startsWith("https://") && episodeLike && provider.owns(url)) Episode(url, title) else null
    }
}.getOrDefault(emptyList())

@HiltViewModel
class TelewebionPickerViewModel @Inject constructor(
    private val client: KodoumoClient,
    private val importer: KodoumoImporter,
    private val reviewRequests: com.kodomo.app.data.remote.ReviewRequestSender,
    private val db: com.kodomo.app.data.local.KodomoDatabase,
    familyRepository: com.kodomo.app.domain.repository.FamilyRepository,
    private val approvals: com.kodomo.app.data.prefs.ApprovalsStore,
) : ViewModel() {
    val children = familyRepository.observeChildren()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    /** The children this approval is for (all of them until the parent unticks some). */
    val chosen = MutableStateFlow<Set<Long>?>(null)
    fun toggleChild(id: Long) {
        val cur = chosen.value ?: children.value.map { it.id }.toSet()
        chosen.value = if (id in cur) cur - id else cur + id
    }
    val allowedKids = approvals.all.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun setKids(contentId: String, kids: Set<Long>) = viewModelScope.launch { approvals.set(contentId, kids) }
    private suspend fun saveKids(contentId: String) {
        val kids = chosen.value ?: children.value.map { it.id }.toSet()
        // A series approved again for another child keeps the children it already had.
        approvals.set(contentId, (approvals.childrenFor(contentId) ?: emptySet()) + kids)
    }
    /** Cartoons already approved through this screen. */
    val approved = db.contentDao().observeParentApprovedVideos()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(id: String) = viewModelScope.launch { db.contentDao().delete(id); approvals.remove(id) }

    data class State(
        val searching: Boolean = false,
        val results: List<KodoumoListItem> = emptyList(),
        val searched: Boolean = false,
        val error: String? = null,
        val selected: KodoumoListItem? = null,
        val review: KodoumoReview? = null,
        val loadingReview: Boolean = false,
        val saved: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /**
     * Titles on Telewebion / Filimo… rarely match kodoumo's exactly, so several shorter forms of the name are searched
     * on kodoumo.ir and in the full kodoumo catalogue stored on the phone, and the closest titles come first.
     */
    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(searching = true, error = null, results = emptyList(), selected = null, review = null, saved = null) }
        val found = linkedMapOf<String, KodoumoListItem>()
        var reached = false
        for (v in TitleMatch.variants(query)) {
            runCatching { client.search(v) }.onSuccess { list -> reached = true; list.forEach { found.putIfAbsent(it.slug, it) } }
            if (found.size >= 12) break
        }
        runCatching { db.contentDao().kodoumoAll() }.getOrDefault(emptyList()).forEach { e ->
            if (TitleMatch.score(query, e.title) >= 0.34) TitleMatch.toListItem(e)?.let { found.putIfAbsent(it.slug, it) }
        }
        val ranked = found.values.sortedByDescending { TitleMatch.score(query, it.title) }
        _state.update {
            it.copy(
                searching = false, searched = true, results = ranked,
                error = if (!reached && ranked.isEmpty()) "اتصال به kodoumo.ir برقرار نشد. این سایت فقط از اینترنت داخل ایران و بدون فیلترشکن باز می‌شود." else null,
            )
        }
    }

    fun select(item: KodoumoListItem) = viewModelScope.launch {
        _state.update { it.copy(selected = item, review = null, loadingReview = true, error = null) }
        runCatching { client.review(item.slug) }
            .onSuccess { r -> _state.update { it.copy(review = r, loadingReview = false) } }
            .onFailure { _state.update { it.copy(loadingReview = false, error = "دریافت ارزیابی کدومو ناموفق بود؛ دوباره تلاش کنید.") } }
    }

    fun clearSelection() = _state.update { it.copy(selected = null, review = null) }

    var provider: com.kodomo.app.ui.kids.video.ContentProvider = com.kodomo.app.ui.kids.video.ContentProvider.TELEWEBION

    fun approve(pageUrl: String, episodes: List<Episode>) = viewModelScope.launch {
        val s = _state.value
        val item = s.selected ?: return@launch
        val review = s.review ?: return@launch
        runCatching { importer.saveApprovedVideo(item, review, pageUrl, episodes, provider.book, provider.label) }
            .onSuccess { e -> saveKids(e.id); _state.update { State(saved = "«${e.title}» با ارزیابی کدومو اضافه شد ✅" + episodesNote(episodes)) } }
            .onFailure { err -> _state.update { it.copy(error = err.message ?: "ذخیره نشد.") } }
    }

    /** No (finished) kodoumo evaluation: the parent takes responsibility and the Kodomo team is asked to evaluate it. */
    fun approveWithoutRating(title: String, pageUrl: String, minAge: Int, note: String, episodes: List<Episode>) = viewModelScope.launch {
        runCatching { importer.saveParentApprovedVideo(title, pageUrl, minAge, note, episodes, provider.book, provider.label) }
            .onSuccess { e ->
                saveKids(e.id)
                val sent = reviewRequests.request(e.title, provider.name, pageUrl, note, minAge)
                _state.update {
                    State(
                        saved = "«${e.title}» با تأیید شما اضافه شد ✅" + episodesNote(episodes) +
                            if (sent) "\nدرخواست ارزیابی برای تیم کدومو ارسال شد." else "\nدرخواست ارزیابی ذخیره شد و با اتصال اینترنت ارسال می‌شود.",
                    )
                }
            }
            .onFailure { err -> _state.update { it.copy(error = err.message ?: "ذخیره نشد.") } }
    }

    private fun episodesNote(episodes: List<Episode>) = if (episodes.size > 1) " (${episodes.size.fa()} قسمت)" else ""

    fun reset() = _state.update { State() }
}

/**
 * Parent-side picker: browse telewebion.ir/kids (navigation limited to Telewebion), open a cartoon, then match it to
 * its kodoumo.ir review. The cartoon can be added for the child only after the parent has seen kodoumo's age rating,
 * indicator bars and «what parents should know» notes. Without a finished kodoumo evaluation the parent may still
 * approve it on their own responsibility, which also sends an evaluation request to the Kodomo team.
 */
@Composable
fun TelewebionPickerScreen(
    onBack: () -> Unit,
    provider: com.kodomo.app.ui.kids.video.ContentProvider = com.kodomo.app.ui.kids.video.ContentProvider.TELEWEBION,
    viewModel: TelewebionPickerViewModel = hiltViewModel(),
) {
    viewModel.provider = provider
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pageUrl by remember { mutableStateOf(provider.startUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var pagePoster by remember { mutableStateOf<String?>(null) }
    // The parent may sign in with their own account, so the sign-in pages of the service are reachable here.
    val lock = remember(provider) { WebLock.SameSite(provider.startUrl, provider.hosts, anyHost = provider.needsAccount) }

    BackHandler {
        when {
            checking -> { checking = false; viewModel.reset() }
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onBack()
        }
    }

    KidScreenScaffold(title = if (checking) "بررسی با کدومو" else provider.label, onBack = {
        if (checking) { checking = false; viewModel.reset() } else onBack()
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // The WebView stays composed (only shrunk) so the page the parent chose is kept while checking.
            LockedWebPlayer(
                lock = lock,
                modifier = if (checking) Modifier.fillMaxWidth().size(1.dp) else Modifier.fillMaxWidth().weight(1f),
                onPageChanged = { url, title -> pageUrl = url; pageTitle = title },
                onWebView = { webView = it },
                visible = !checking,
            )
            if (!checking) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.saved?.let { Text(it, color = KodomoColors.Green, style = MaterialTheme.typography.labelLarge) }
                    Text(
                        (if (provider.needsAccount) "اگر لازم است با حساب کاربری خودتان در ${provider.label} وارد شوید. " else "") +
                            (if (provider.book) "صفحه‌ی کتاب موردنظر را باز کنید" else "صفحه‌ی کارتون موردنظر را باز کنید") +
                            "، سپس دکمه‌ی زیر را بزنید. کودک فقط همین صفحه را می‌بیند و نمی‌تواند به جای دیگری برود.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val approved by viewModel.approved.collectAsStateWithLifecycle()
                    val mine = approved.filter { provider.owns(it.mediaUrl.orEmpty()) }
                    val kidsAll by viewModel.children.collectAsStateWithLifecycle()
                    val allowedKids by viewModel.allowedKids.collectAsStateWithLifecycle()
                    if (mine.isNotEmpty()) ApprovedStrip(mine, viewModel::remove, kidsAll, allowedKids, viewModel::setKids)
                    val isListing = android.net.Uri.parse(provider.startUrl).path.orEmpty().trimEnd('/') == android.net.Uri.parse(pageUrl).path.orEmpty().trimEnd('/') || !provider.owns(pageUrl)
                    Button(
                        onClick = {
                            query = PendingKodoumoPick.title ?: cleanTelewebionTitle(pageTitle)
                            val current = pageUrl
                            webView?.evaluateJavascript(PAGE_INFO_JS) { raw ->
                                val info = parsePageInfo(raw)
                                if (PendingKodoumoPick.title == null && info != null) {
                                    val better = cleanTelewebionTitle(info.title).ifBlank { cleanTelewebionTitle(info.heading) }
                                    if (better.isNotBlank() && better != query) { query = better; viewModel.search(better) }
                                }
                            }
                            // Remember every episode of the series shown on this page, so the child can pick between them.
                            webView?.evaluateJavascript(COLLECT_EPISODES_JS) { raw ->
                                val found = if (provider.book) emptyList() else parseEpisodeLinks(raw, provider)
                                episodes = if (found.none { WebLockRules.samePage(it.url, current) }) listOf(Episode(current, pageTitle)) + found else found
                            } ?: run { episodes = listOf(Episode(current, pageTitle)) }
                            webView?.evaluateJavascript("(function(){var m=document.querySelector('meta[property=\"og:image\"],meta[name=\"twitter:image\"]');return m?m.content:'';})()") { raw ->
                                pagePoster = raw?.trim('"')?.takeIf { it.startsWith("http") }
                            }
                            checking = true
                            viewModel.search(query)
                        },
                        enabled = !isListing,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (isListing) (if (provider.book) "یک کتاب را باز کنید" else "یک کارتون را باز کنید") else "بررسی در کدومو و افزودن برای کودک") }
                }
            } else {
                CheckPanel(
                    state = state,
                    pageUrl = pageUrl,
                    pageTitle = pageTitle,
                    query = query,
                    onQuery = { query = it },
                    onSearch = { viewModel.search(query) },
                    onSelect = viewModel::select,
                    onClear = viewModel::clearSelection,
                    episodes = episodes,
                    onApprove = { viewModel.approve(pageUrl, episodes); checking = false; PendingKodoumoPick.title = null },
                    onApproveWithoutRating = { minAge, note ->
                        viewModel.approveWithoutRating(query.ifBlank { cleanTelewebionTitle(pageTitle) }, pageUrl, minAge, note, episodes)
                        checking = false
                        PendingKodoumoPick.title = null
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background),
                    pagePoster = pagePoster,
                    book = provider.book,
                    kidsChooser = {
                        val kids by viewModel.children.collectAsStateWithLifecycle()
                        val chosen by viewModel.chosen.collectAsStateWithLifecycle()
                        KidsChooser(kids, chosen ?: kids.map { it.id }.toSet(), viewModel::toggleChild)
                    },
                )
            }
        }
    }
}

@Composable
internal fun CheckPanel(
    state: TelewebionPickerViewModel.State,
    pageUrl: String,
    pageTitle: String,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onSelect: (KodoumoListItem) -> Unit,
    onClear: () -> Unit,
    episodes: List<Episode>,
    onApprove: () -> Unit,
    onApproveWithoutRating: (minAge: Int, note: String) -> Unit,
    modifier: Modifier = Modifier,
    pagePoster: String? = null,
    book: Boolean = false,
    kidsChooser: @Composable () -> Unit = {},
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { kidsChooser() }
        item {
            if (pagePoster != null) Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(pagePoster, contentDescription = null, modifier = Modifier.size(56.dp, 76.dp))
                Spacer(Modifier.width(10.dp))
                Text("پوستر همین صفحه؛ با پوستر نتیجه‌های کدومو مقایسه کنید تا مطمئن شوید همان اثر است.", style = MaterialTheme.typography.bodySmall)
            }
            Text("صفحه‌ی انتخاب‌شده: ${pageTitle.ifBlank { pageUrl }}", style = MaterialTheme.typography.labelMedium)
            Text(pageUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            if (episodes.size > 1) Text("📺 ${episodes.size.fa()} قسمت در این صفحه پیدا شد؛ کودک می‌تواند بین قسمت‌ها انتخاب کند.", style = MaterialTheme.typography.labelMedium, color = KodomoColors.Green)
        }
        val review = state.review
        val selected = state.selected
        if (selected == null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = query, onValueChange = onQuery, singleLine = true, label = { Text("نام اثر در کدومو") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onSearch) { Text("جستجو") }
                }
            }
            if (state.searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { e -> item { Text("⚠️ $e", color = KodomoColors.Danger) } }
            if (state.searched && !state.searching && state.results.isEmpty() && state.error == null) {
                item {
                    Text(
                        "این اثر در کدومو پیدا نشد. نام را کوتاه‌تر یا به شکل دیگری بنویسید، یا اگر خودتان آن را مناسب می‌دانید با تأیید خودتان اضافه کنید.",
                        color = KodomoColors.Danger,
                    )
                }
            }
            if (state.searched && !state.searching && (state.results.isEmpty() || state.error != null)) {
                item { ParentOnlyApprovalCard(onApproveWithoutRating, book) }
            }
            if (state.results.isNotEmpty()) {
                item { Text("کدام‌یک همین اثر است؟ پوستر و نام را مقایسه کنید:", style = MaterialTheme.typography.labelLarge) }
            }
            items(state.results, key = { it.slug }) { item ->
                KodomoCard(Modifier.fillMaxWidth(), onClick = { onSelect(item) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.image != null) AsyncImage(item.image, contentDescription = null, modifier = Modifier.size(56.dp, 76.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            if (item == state.results.firstOrNull() && TitleMatch.score(query, item.title) >= 0.6) {
                                Text("✨ احتمالاً همین است — پوستر را با صفحه مقایسه کنید", style = MaterialTheme.typography.labelMedium, color = KodomoColors.Green)
                            }
                            Text(
                                when {
                                    item.underEvaluation -> "⏳ در حال ارزیابی کدومو"
                                    item.age == null -> "منتظر رده‌ی سنی"
                                    else -> "رده‌ی سنی کدومو: ${item.age.fa()}+"
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            if (state.results.isNotEmpty() && state.error == null) {
                item {
                    var open by remember { mutableStateOf(false) }
                    if (!open) {
                        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (book) "هیچ‌کدام همین کتاب نیست؛ با تأیید خودم" else "هیچ‌کدام همین فیلم نیست؛ با تأیید خودم")
                        }
                    } else {
                        ParentOnlyApprovalCard(onApproveWithoutRating, book)
                    }
                }
            }
        } else {
            item {
                OutlinedButton(onClick = onClear) { Text("◀ انتخاب اثر دیگر") }
            }
            if (state.loadingReview) item { CircularProgressIndicator() }
            state.error?.let { e -> item { Text("⚠️ $e", color = KodomoColors.Danger) } }
            if (review != null) {
                val age = review.age ?: selected.age
                val blocked = selected.underEvaluation || review.underEvaluation || age == null
                item {
                    Text(review.title.ifBlank { selected.title }, style = MaterialTheme.typography.headlineSmall)
                    if (review.latinName.isNotBlank()) Text(review.latinName, style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (age != null) "رده‌ی سنی کدومو: ${age.fa()}+ سال" else "کدومو هنوز رده‌ی سنی ثبت نکرده است",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (age != null) KodomoColors.Green else KodomoColors.Danger,
                    )
                }
                item {
                    Text("شاخص‌های اثر", style = MaterialTheme.typography.titleMedium)
                    review.indicators.forEach { ind ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            RatingPill(ind.label, ind.rate, positive = !ind.negative)
                            if (ind.text.isNotBlank()) Text(ind.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (review.indicators.isEmpty()) Text("شاخصی ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                }
                if (review.parentShouldKnow.isNotBlank()) item { NoteCard("آنچه لازم است والدین بدانند", review.parentShouldKnow) }
                if (review.sayToChildren.isNotBlank()) item { NoteCard("با فرزندتان درباره‌ی این‌ها صحبت کنید", review.sayToChildren) }
                if (review.special.isNotBlank()) item { NoteCard("ویژگی اثر", review.special) }
                if (review.story.isNotBlank()) item { NoteCard("داستان", review.story) }
                item {
                    var confirmed by remember(review.id) { mutableStateOf(false) }
                    if (blocked) {
                        Text("این اثر هنوز ارزیابی کامل کدومو را ندارد.", color = KodomoColors.Danger)
                        ParentOnlyApprovalCard(onApproveWithoutRating, book)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                            Text(if (book) "نکات و شاخص‌های کدومو را خواندم و این کتاب با همین صفحه مطابقت دارد." else "نکات و شاخص‌های کدومو را خواندم و این اثر با همین صفحه مطابقت دارد.", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "پس از افزودن، کارتون فقط برای کودکانی نمایش داده می‌شود که سن و حد مجاز خشونت/ترس/ناهنجاری آن‌ها با این ارزیابی بخواند.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Button(onClick = onApprove, enabled = confirmed, modifier = Modifier.fillMaxWidth()) { Text("تأیید و افزودن برای کودک") }
                    }
                }
            }
        }
    }
}

/**
 * Approval without an expert rating: the parent picks the minimum age and confirms responsibility; the title is then
 * playable and an evaluation request is sent to the Kodomo admin team.
 */
@Composable
private fun ParentOnlyApprovalCard(onApprove: (minAge: Int, note: String) -> Unit, book: Boolean = false) {
    var minAge by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    KodomoCard(Modifier.fillMaxWidth()) {
        Text(if (book) "نمایش با تأیید شما (بدون ارزیابی کارشناسی)" else "پخش با تأیید شما (بدون ارزیابی کارشناسی)", style = MaterialTheme.typography.titleMedium)
        Text(
            if (book) "اگر خودتان این کتاب را خوانده‌اید و مناسب می‌دانید، می‌توانید آن را تأیید کنید. کتاب با برچسب «تأیید والدین» نمایش داده می‌شود و درخواست ارزیابی برای تیم کدومو ارسال می‌شود."
            else "اگر خودتان این کارتون را دیده‌اید و مناسب می‌دانید، می‌توانید آن را تأیید کنید. کارتون با برچسب «تأیید والدین» پخش می‌شود و درخواست ارزیابی برای تیم کدومو ارسال می‌شود.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("مناسب از سن: ${minAge.fa()} سال", style = MaterialTheme.typography.labelLarge)
        androidx.compose.material3.Slider(value = minAge.toFloat(), onValueChange = { minAge = it.toInt() }, valueRange = 0f..15f, steps = 14)
        OutlinedTextField(value = note, onValueChange = { note = it.take(500) }, label = { Text("یادداشت برای کارشناسان (اختیاری)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
            Text(if (book) "این کتاب را خوانده‌ام و مسئولیت تأیید آن با من است." else "این کارتون را دیده‌ام و مسئولیت تأیید آن با من است.", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { onApprove(minAge, note) }, enabled = confirmed, modifier = Modifier.fillMaxWidth()) { Text("تأیید من و ارسال درخواست ارزیابی") }
    }
}

/** The cartoons the family has added, with age and episode count; long list scrolls sideways. */
@Composable
private fun ApprovedStrip(
    list: List<com.kodomo.app.data.local.entity.ContentEntity>,
    onRemove: (String) -> Unit,
    kids: List<com.kodomo.app.domain.model.Child> = emptyList(),
    allowedKids: Map<String, Set<Long>> = emptyMap(),
    onKids: (String, Set<Long>) -> Unit = { _, _ -> },
) {
    var confirm by remember { mutableStateOf<com.kodomo.app.data.local.entity.ContentEntity?>(null) }
    Text("افزوده‌شده‌ها (${list.size.fa()}) – برای انتخاب بچه‌ها یا حذف، روی هر کدام بزنید", style = MaterialTheme.typography.labelLarge)
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list.size) { i ->
            val e = list[i]
            val episodes = e.episodes?.lines()?.size ?: 1
            Column(
                Modifier
                    .size(width = 170.dp, height = 82.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .clickable { confirm = e }
                    .padding(8.dp),
            ) {
                Text(e.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text("${e.minAge.fa()}+ سال • ${episodes.fa()} قسمت" + if (e.parentOnly) " • تأیید والد" else " • کدومو", style = MaterialTheme.typography.labelSmall)
                val forKids = allowedKids[e.id]
                Text(if (forKids == null) "👨‍👩‍👧 همه‌ی بچه‌ها" else "👧 " + kids.filter { it.id in forKids }.joinToString("، ") { it.name }.ifBlank { "هیچ‌کس" }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    confirm?.let { e ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("«${e.title}»") },
            text = {
                Column {
                    var sel by remember(e.id) { mutableStateOf(allowedKids[e.id] ?: kids.map { it.id }.toSet()) }
                    KidsChooser(kids, sel) { id -> sel = if (id in sel) sel - id else sel + id; onKids(e.id, sel) }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text("بستن") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { onRemove(e.id); confirm = null }) { Text("حذف برای همه", color = KodomoColors.Danger) } },
        )
    }
}

@Composable
private fun NoteCard(title: String, text: String) {
    KodomoCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/** Fuzzy comparison of Persian titles (spelling variants, half-spaces, extra words such as «فصل ۲»). */
internal object TitleMatch {
    fun normalize(s: String) = s.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace(Regex("[«»:\\-–—،,.!؟?()\\[\\]]"), " ")
        .replace(Regex("\\s+"), " ").trim().lowercase()

    private val noise = setOf("انیمیشن", "کارتون", "سریال", "فیلم", "فصل", "قسمت", "کودک", "دوبله", "فارسی", "کامل", "the", "a")

    fun tokens(s: String) = normalize(s).split(' ').filter { it.length > 1 && it !in noise && it.none(Char::isDigit) }.toSet()

    /** 0..1: shared words, with a bonus when one title contains the other. */
    fun score(a: String, b: String): Double {
        val ta = tokens(a); val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val jac = ta.intersect(tb).size.toDouble() / ta.union(tb).size
        val na = normalize(a); val nb = normalize(b)
        val contains = if (na.contains(nb) || nb.contains(na)) 0.35 else 0.0
        return (jac + contains).coerceAtMost(1.0)
    }

    /** The name itself, then shorter forms: first words, and the longest word. */
    fun variants(q: String): List<String> {
        val words = normalize(q).split(' ').filter { it.isNotBlank() && it !in noise }
        return buildList {
            add(q.trim())
            if (words.size > 3) add(words.take(3).joinToString(" "))
            if (words.size > 2) add(words.take(2).joinToString(" "))
            words.maxByOrNull { it.length }?.takeIf { it.length >= 3 }?.let { add(it) }
        }.distinct()
    }

    fun toListItem(e: com.kodomo.app.data.local.entity.ContentEntity): KodoumoListItem? {
        val slug = e.sourceUrl?.let { android.net.Uri.parse(it).pathSegments.lastOrNull { s -> s.isNotBlank() } } ?: return null
        return KodoumoListItem(
            id = e.id.removePrefix("kodoumo_").toLongOrNull() ?: 0, slug = slug, title = e.title, image = e.imageUrl,
            shortDescription = e.description, age = e.minAge.takeIf { it <= 18 }, rate = null, underEvaluation = e.minAge > 18,
            permalink = e.sourceUrl.orEmpty(), typeSlug = e.category,
        )
    }
}

/** «برای کدام فرزندان؟» — tick the children this title is for. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun KidsChooser(kids: List<com.kodomo.app.domain.model.Child>, chosen: Set<Long>, onToggle: (Long) -> Unit) {
    if (kids.isEmpty()) return
    KodomoCard(Modifier.fillMaxWidth()) {
        Text("👨‍👩‍👧 برای کدام فرزندان؟", style = MaterialTheme.typography.titleSmall)
        Text("فقط بچه‌هایی که تیک دارند این اثر را می‌بینند (سن هر کودک هم جداگانه بررسی می‌شود).", style = MaterialTheme.typography.bodySmall)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kids.forEach { k ->
                androidx.compose.material3.FilterChip(
                    selected = k.id in chosen, onClick = { onToggle(k.id) },
                    label = { Text("${if (k.id in chosen) "✅" else "⬜"} ${k.name} (${k.ageYears().fa()} ساله)") },
                )
            }
        }
    }
}
