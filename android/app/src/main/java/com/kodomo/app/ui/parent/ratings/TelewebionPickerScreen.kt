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
    listOf("تلوبیون", "Telewebion", "پخش زنده", "تماشای آنلاین", "تماشای", "آنلاین", "دانلود", "انیمیشن", "کارتون", "سریال", "فیلم", "کودک")
        .forEach { t = t.replace(it, " ") }
    t = t.replace(Regex("""(قسمت|فصل|اپیزود)\s*\S+"""), " ")
    return t.replace(Regex("""[\-–—:«»"]+"""), " ").replace(Regex("""\s+"""), " ").trim()
}

/** Collects every episode link visible on a Telewebion series / program page. */
private const val COLLECT_EPISODES_JS = """
(function(){
  var out = [], seen = {};
  document.querySelectorAll('a[href]').forEach(function(a){
    var h = a.href;
    if (/\/episode\//.test(h) && !seen[h]) {
      seen[h] = 1;
      var t = (a.getAttribute('title') || a.getAttribute('aria-label') || a.innerText || '').trim().replace(/\s+/g, ' ').slice(0, 80);
      out.push([h, t]);
    }
  });
  return JSON.stringify(out.slice(0, 300));
})()
"""

/** Parses the evaluateJavascript result (a JSON-encoded string holding a JSON array of [url, title]). */
internal fun parseEpisodeLinks(raw: String?): List<Episode> = runCatching {
    val inner = kotlinx.serialization.json.Json.parseToJsonElement(raw ?: "null")
    val text = (inner as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return emptyList()
    (kotlinx.serialization.json.Json.parseToJsonElement(text) as kotlinx.serialization.json.JsonArray).mapNotNull { e ->
        val pair = e as? kotlinx.serialization.json.JsonArray ?: return@mapNotNull null
        val url = (pair.getOrNull(0) as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
        val title = (pair.getOrNull(1) as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        if (url.startsWith("https://") && WebLockRules.hostMatches(android.net.Uri.parse(url).host.orEmpty(), WebLockRules.telewebionHosts)) Episode(url, title) else null
    }
}.getOrDefault(emptyList())

@HiltViewModel
class TelewebionPickerViewModel @Inject constructor(
    private val client: KodoumoClient,
    private val importer: KodoumoImporter,
    private val reviewRequests: com.kodomo.app.data.remote.ReviewRequestSender,
    private val db: com.kodomo.app.data.local.KodomoDatabase,
) : ViewModel() {
    /** Cartoons already approved through this screen. */
    val approved = db.contentDao().observeParentApprovedVideos()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(id: String) = viewModelScope.launch { db.contentDao().delete(id) }

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

    fun search(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        _state.update { it.copy(searching = true, error = null, results = emptyList(), selected = null, review = null, saved = null) }
        runCatching { client.search(query) }
            .onSuccess { list ->
                // Only works whose evaluation is finished and carry an age rating may be offered.
                _state.update { it.copy(searching = false, searched = true, results = list) }
            }
            .onFailure {
                _state.update { it.copy(searching = false, searched = true, error = "اتصال به kodoumo.ir برقرار نشد. این سایت فقط از اینترنت داخل ایران و بدون فیلترشکن باز می‌شود.") }
            }
    }

    fun select(item: KodoumoListItem) = viewModelScope.launch {
        _state.update { it.copy(selected = item, review = null, loadingReview = true, error = null) }
        runCatching { client.review(item.slug) }
            .onSuccess { r -> _state.update { it.copy(review = r, loadingReview = false) } }
            .onFailure { _state.update { it.copy(loadingReview = false, error = "دریافت ارزیابی کدومو ناموفق بود؛ دوباره تلاش کنید.") } }
    }

    fun clearSelection() = _state.update { it.copy(selected = null, review = null) }

    fun approve(pageUrl: String, episodes: List<Episode>) = viewModelScope.launch {
        val s = _state.value
        val item = s.selected ?: return@launch
        val review = s.review ?: return@launch
        runCatching { importer.saveApprovedVideo(item, review, pageUrl, episodes) }
            .onSuccess { e -> _state.update { State(saved = "«${e.title}» با ارزیابی کدومو اضافه شد ✅" + episodesNote(episodes)) } }
            .onFailure { err -> _state.update { it.copy(error = err.message ?: "ذخیره نشد.") } }
    }

    /** No (finished) kodoumo evaluation: the parent takes responsibility and the Kodomo team is asked to evaluate it. */
    fun approveWithoutRating(title: String, pageUrl: String, minAge: Int, note: String, episodes: List<Episode>) = viewModelScope.launch {
        runCatching { importer.saveParentApprovedVideo(title, pageUrl, minAge, note, episodes) }
            .onSuccess { e ->
                val sent = reviewRequests.request(e.title, "TELEWEBION", pageUrl, note, minAge)
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
fun TelewebionPickerScreen(onBack: () -> Unit, viewModel: TelewebionPickerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pageUrl by remember { mutableStateOf(TELEWEBION_KIDS) }
    var pageTitle by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    val lock = remember { WebLock.SameSite(TELEWEBION_KIDS, WebLockRules.telewebionHosts) }

    BackHandler {
        when {
            checking -> { checking = false; viewModel.reset() }
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onBack()
        }
    }

    KidScreenScaffold(title = if (checking) "بررسی با کدومو" else "کارتون‌های تلوبیون", onBack = {
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
                        "صفحه‌ی کارتون موردنظر را باز کنید، سپس دکمه‌ی زیر را بزنید. کودک فقط همین صفحه را می‌بیند و نمی‌تواند به جای دیگری برود.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val approved by viewModel.approved.collectAsStateWithLifecycle()
                    if (approved.isNotEmpty()) ApprovedStrip(approved, viewModel::remove)
                    val isListing = WebLockRules.samePage(TELEWEBION_KIDS, pageUrl)
                    Button(
                        onClick = {
                            query = cleanTelewebionTitle(pageTitle)
                            val current = pageUrl
                            // Remember every episode of the series shown on this page, so the child can pick between them.
                            webView?.evaluateJavascript(COLLECT_EPISODES_JS) { raw ->
                                val found = parseEpisodeLinks(raw)
                                episodes = if (found.none { WebLockRules.samePage(it.url, current) }) listOf(Episode(current, pageTitle)) + found else found
                            } ?: run { episodes = listOf(Episode(current, pageTitle)) }
                            checking = true
                            viewModel.search(query)
                        },
                        enabled = !isListing,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (isListing) "یک کارتون را باز کنید" else "بررسی این کارتون در کدومو و افزودن") }
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
                    onApprove = { viewModel.approve(pageUrl, episodes); checking = false },
                    onApproveWithoutRating = { minAge, note ->
                        viewModel.approveWithoutRating(query.ifBlank { cleanTelewebionTitle(pageTitle) }, pageUrl, minAge, note, episodes)
                        checking = false
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

@Composable
private fun CheckPanel(
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
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
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
                        "این اثر در کدومو پیدا نشد. تا وقتی کدومو آن را ارزیابی نکرده، امکان افزودن برای کودک وجود ندارد. نام را کوتاه‌تر یا به شکل دیگری بنویسید.",
                        color = KodomoColors.Danger,
                    )
                }
            }
            if (state.searched && !state.searching && (state.results.isEmpty() || state.error != null)) {
                item { ParentOnlyApprovalCard(onApproveWithoutRating) }
            }
            items(state.results, key = { it.slug }) { item ->
                KodomoCard(Modifier.fillMaxWidth(), onClick = { onSelect(item) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.image != null) AsyncImage(item.image, contentDescription = null, modifier = Modifier.size(56.dp, 76.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
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
                        ParentOnlyApprovalCard(onApproveWithoutRating)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                            Text("نکات و شاخص‌های کدومو را خواندم و این کارتون با همین صفحه‌ی تلوبیون مطابقت دارد.", style = MaterialTheme.typography.bodySmall)
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
private fun ParentOnlyApprovalCard(onApprove: (minAge: Int, note: String) -> Unit) {
    var minAge by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    KodomoCard(Modifier.fillMaxWidth()) {
        Text("پخش با تأیید شما (بدون ارزیابی کارشناسی)", style = MaterialTheme.typography.titleMedium)
        Text(
            "اگر خودتان این کارتون را دیده‌اید و مناسب می‌دانید، می‌توانید آن را تأیید کنید. کارتون با برچسب «تأیید والدین» پخش می‌شود و درخواست ارزیابی برای تیم کدومو ارسال می‌شود.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("مناسب از سن: ${minAge.fa()} سال", style = MaterialTheme.typography.labelLarge)
        androidx.compose.material3.Slider(value = minAge.toFloat(), onValueChange = { minAge = it.toInt() }, valueRange = 0f..15f, steps = 14)
        OutlinedTextField(value = note, onValueChange = { note = it.take(500) }, label = { Text("یادداشت برای کارشناسان (اختیاری)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
            Text("این کارتون را دیده‌ام و مسئولیت تأیید آن با من است.", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { onApprove(minAge, note) }, enabled = confirmed, modifier = Modifier.fillMaxWidth()) { Text("تأیید من و ارسال درخواست ارزیابی") }
    }
}

/** The cartoons the family has added, with age and episode count; long list scrolls sideways. */
@Composable
private fun ApprovedStrip(list: List<com.kodomo.app.data.local.entity.ContentEntity>, onRemove: (String) -> Unit) {
    var confirm by remember { mutableStateOf<com.kodomo.app.data.local.entity.ContentEntity?>(null) }
    Text("کارتون‌های افزوده‌شده (${list.size.fa()}) – برای حذف روی هر کدام بزنید", style = MaterialTheme.typography.labelLarge)
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list.size) { i ->
            val e = list[i]
            val episodes = e.episodes?.lines()?.size ?: 1
            Column(
                Modifier
                    .size(width = 150.dp, height = 64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .clickable { confirm = e }
                    .padding(8.dp),
            ) {
                Text(e.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text("${e.minAge.fa()}+ سال • ${episodes.fa()} قسمت" + if (e.parentOnly) " • تأیید والد" else " • کدومو", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    confirm?.let { e ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("حذف «${e.title}»؟") },
            text = { Text("این کارتون دیگر برای کودک نمایش داده نمی‌شود.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { onRemove(e.id); confirm = null }) { Text("حذف") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text("انصراف") } },
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
