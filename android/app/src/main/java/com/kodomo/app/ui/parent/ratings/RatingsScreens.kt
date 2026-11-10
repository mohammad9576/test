package com.kodomo.app.ui.parent.ratings

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.PeerOpinion
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.RadarChart
import com.kodomo.app.ui.components.RatingPill
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.navigation.Routes
import com.kodomo.app.ui.parent.ParentHubViewModel
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RatingsTab(viewModel: ParentHubViewModel, onOpen: (String) -> Unit, onOpenTelewebion: () -> Unit, modifier: Modifier = Modifier, onOpenWatch: () -> Unit = {}) {
    val items by viewModel.ratedContent.collectAsStateWithLifecycle()
    val status by viewModel.kodoumoStatus.collectAsStateWithLifecycle()
    val lastSync by viewModel.kodoumoLastSync.collectAsStateWithLifecycle()
    val w by viewModel.wellbeing.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf(RatingsFilter.Query()) }
    var sortMenu by remember { mutableStateOf(false) }
    // Quiet background refresh (at most twice a day); nothing on this screen waits for it.
    LaunchedEffect(Unit) { viewModel.autoSyncKodoumo() }
    val kinds = remember(items) { RatingsFilter.kinds(items) }
    val genres = remember(items, q.kind) { RatingsFilter.genres(items, q.kind) }
    val shown = remember(items, q) { RatingsFilter.apply(items, q) }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("ارزیابی‌های کدومو", style = MaterialTheme.typography.headlineSmall)
                val running = status is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Running
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append("${items.size.fa()} اثر")
                            append(" • ")
                            append(
                                when {
                                    running -> "در حال به‌روزرسانی در پس‌زمینه…"
                                    status is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Failed && items.isEmpty() -> "kodoumo.ir فقط با اینترنت داخل ایران باز می‌شود"
                                    lastSync == 0L -> "هنوز به‌روزرسانی نشده"
                                    else -> "به‌روزرسانی: " + ago(lastSync)
                                },
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (!running) androidx.compose.material3.TextButton(onClick = { viewModel.syncKodoumo() }) { Text("به‌روزرسانی") }
                }
            }
        }
        if (!w.kodoumoChoiceAsked) {
            item {
                KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("چه چیزی به کودک نشان داده شود؟", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { viewModel.setKodoumoAutoShow(false) }, label = { Text("فقط انتخاب‌های من") })
                        FilterChip(selected = false, onClick = { viewModel.setKodoumoAutoShow(true) }, label = { Text("همه‌ی تأییدشده‌های کدومو") })
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KodomoCard(Modifier.weight(1f), onClick = onOpenTelewebion) {
                    Text("➕ افزودن برای کودک", style = MaterialTheme.typography.titleSmall)
                    Text("تلوبیون، فیلیمو، نماوا، فیلم‌نت…", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                KodomoCard(Modifier.weight(1f), onClick = onOpenWatch) {
                    Text("🍿 تماشای والدین", style = MaterialTheme.typography.titleSmall)
                    Text("بدون محدودیت، با ارزیابی", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 6.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = q.text, onValueChange = { q = q.copy(text = it) }, singleLine = true,
                    placeholder = { Text("جستجو: نام اثر، ژانر، نویسنده…") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (q.text.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { q = q.copy(text = "") }) {
                            androidx.compose.material3.Icon(Icons.Rounded.Close, contentDescription = "پاک کردن")
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = q.kind == null, onClick = { q = q.copy(kind = null, genre = null) }, label = { Text("همه (${items.size.fa()})") }) }
                items(kinds, key = { it.first }) { (k, n) ->
                    FilterChip(selected = q.kind == k, onClick = { q = q.copy(kind = if (q.kind == k) null else k, genre = null) }, label = { Text("$k (${n.fa()})") })
                }
            }
        }
        if (genres.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(genres, key = { it }) { g ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = { q = q.copy(genre = if (q.genre == g) null else g) },
                            label = { Text(if (q.genre == g) "✓ $g" else g) },
                            colors = if (q.genre == g) androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(),
                        )
                    }
                }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RatingsFilter.Age.entries.toList()) { a -> FilterChip(selected = q.age == a, onClick = { q = q.copy(age = a) }, label = { Text(a.label) }) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RatingsFilter.Status.entries.toList()) { st -> FilterChip(selected = q.status == st, onClick = { q = q.copy(status = st) }, label = { Text(st.label) }) }
                }
                Box {
                    androidx.compose.material3.AssistChip(onClick = { sortMenu = true }, label = { Text("↕ " + q.sort.label) })
                    androidx.compose.material3.DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        RatingsFilter.Sort.entries.forEach { so ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(so.label) }, onClick = { q = q.copy(sort = so); sortMenu = false })
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${shown.size.fa()} نتیجه", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (!q.isDefault) androidx.compose.material3.TextButton(onClick = { q = RatingsFilter.Query() }) { Text("پاک کردن فیلترها") }
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    if (items.isEmpty()) "فهرست کدومو هنوز دریافت نشده است. کمی بعد دوباره سر بزنید؛ به‌روزرسانی خودکار در پس‌زمینه انجام می‌شود."
                    else "اثری با این فیلترها پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(shown, key = { it.id }) { item ->
            KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), onClick = { onOpen(item.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.kodomo.app.ui.components.ContentCover(item, Modifier.size(width = 64.dp, height = 88.dp), emojiSize = 32, corner = 14.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        val genresText = RatingsFilter.genresOf(item).take(3).joinToString("، ")
                        Text(
                            RatingsFilter.kindOf(item) + (if (genresText.isNotEmpty()) " • $genresText" else ""),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                        Text(
                            if (RatingsFilter.isPending(item)) "⏳ در حال ارزیابی کدومو" else "سن پیشنهادی: ${item.rating.minAge.fa()} تا ${item.rating.maxAge.fa()} سال" + if (item.mediaUrl != null) " • ✅ تأییدشده برای کودک" else "",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        if (!RatingsFilter.isPending(item)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                RatingPill("خشونت", item.rating.violence)
                                RatingPill("ترس", item.rating.fear)
                                RatingPill("آموزندگی", item.rating.educational, positive = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ago(millis: Long): String {
    val min = ((System.currentTimeMillis() - millis) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "همین الان"
        min < 60 -> "${min.toInt().fa()} دقیقه پیش"
        min < 24 * 60 -> "${(min / 60).toInt().fa()} ساعت پیش"
        else -> "${(min / (24 * 60)).toInt().fa()} روز پیش"
    }
}

@HiltViewModel
class ContentPassportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val aparat: com.kodomo.app.data.kodoumo.AparatSearch,
) : ViewModel() {
    private val id = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())
    val results = kotlinx.coroutines.flow.MutableStateFlow<List<com.kodomo.app.data.kodoumo.AparatVideo>>(emptyList())
    val searching = kotlinx.coroutines.flow.MutableStateFlow(false)
    val searchError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun search(query: String) = viewModelScope.launch {
        searching.value = true
        searchError.value = null
        results.value = runCatching { aparat.search(query) }.getOrElse {
            searchError.value = "جستجو در آپارات انجام نشد؛ اینترنت را بررسی کنید."
            emptyList()
        }
        searching.value = false
    }

    /** Parent approves exactly which video the child may watch for this rated title. */
    fun setVideo(url: String?) = viewModelScope.launch { contentRepository.setMediaUrl(id, url) }
    val item: StateFlow<ContentItem?> = contentRepository.observeItem(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val opinion: StateFlow<PeerOpinion> = contentRepository.observePeerOpinion(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeerOpinion(0, 0, emptyList()))
}

/** «شناسنامه‌ی تربیتی» of a content item with the Kodomo radar of positive & negative indicators. */
@Composable
fun ContentPassportScreen(contentId: String, onBack: () -> Unit, viewModel: ContentPassportViewModel = hiltViewModel()) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val opinion by viewModel.opinion.collectAsStateWithLifecycle()
    KidScreenScaffold(title = "شناسنامه‌ی تربیتی", onBack = onBack) { padding ->
        val i = item ?: return@KidScreenScaffold
        val r = i.rating
        Column(
            Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.kodomo.app.ui.components.ContentCover(i, Modifier.size(width = 96.dp, height = 132.dp), emojiSize = 48)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(i.title, style = MaterialTheme.typography.headlineSmall)
                    Text("${i.type.label} • ${i.category}", style = MaterialTheme.typography.labelLarge, color = KodomoColors.Green)
                    Text(if (r.minAge > 18) "⏳ در حال ارزیابی کدومو؛ هنوز به کودک نشان داده نمی‌شود" else "سن پیشنهادی: ${r.minAge.fa()} تا ${r.maxAge.fa()} سال", style = MaterialTheme.typography.titleSmall)
                    if (i.author.isNotBlank()) Text(i.author, style = MaterialTheme.typography.labelSmall)
                }
            }
            KodomoCard(Modifier.fillMaxWidth()) { Text(i.description, style = MaterialTheme.typography.bodyLarge) }
            i.sourceUrl?.let { url ->
                val context = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.material3.TextButton(onClick = {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text("مشاهده‌ی نقد کامل در kodoumo.ir ↗") }
            }
            if (i.type == ContentType.VIDEO) VideoApprovalCard(i, viewModel)
            KodomoCard(Modifier.fillMaxWidth()) {
                SectionTitle("رادار شاخص‌ها")
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    RadarChart(
                        axes = listOf(
                            "آموزندگی" to r.educational.toFloat(),
                            "خلاقیت" to r.creativity.toFloat(),
                            "ارزش خانوادگی" to r.familyValues.toFloat(),
                            "خشونت" to r.violence.toFloat(),
                            "ترس" to r.fear.toFloat(),
                            "ناهنجاری" to r.abnormality.toFloat(),
                        ),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RatingPill("آموزندگی", r.educational, positive = true)
                    RatingPill("خلاقیت", r.creativity, positive = true)
                    RatingPill("ارزش خانوادگی", r.familyValues, positive = true)
                    RatingPill("خشونت", r.violence)
                    RatingPill("ترس", r.fear)
                    RatingPill("ناهنجاری", r.abnormality)
                }
            }
            if (i.positives.isNotEmpty() || i.negatives.isNotEmpty()) {
                KodomoCard(Modifier.fillMaxWidth()) {
                    if (i.positives.isNotEmpty()) {
                        Text("نقاط مثبت", style = MaterialTheme.typography.titleMedium, color = KodomoColors.Green)
                        i.positives.forEach { Text("✅ $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (i.negatives.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("نکات قابل توجه", style = MaterialTheme.typography.titleMedium, color = KodomoColors.Peach)
                        i.negatives.forEach { Text("⚠️ $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            if (i.body.isNotBlank() && i.sourceUrl != null) {
                KodomoCard(Modifier.fillMaxWidth()) {
                    Text(i.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (i.expertReview.isNotBlank()) {
                KodomoCard(Modifier.fillMaxWidth()) {
                    Text("دیدگاه کارشناس", style = MaterialTheme.typography.titleMedium)
                    Text(i.expertReview, style = MaterialTheme.typography.bodyMedium)
                    Text("— ${i.expertName}", style = MaterialTheme.typography.labelMedium, color = KodomoColors.InkSoft)
                }
            }
            KodomoCard(Modifier.fillMaxWidth()) {
                Text("نظر کودکان", style = MaterialTheme.typography.titleMedium)
                Text("👍 ${opinion.likes.fa()}   👎 ${opinion.dislikes.fa()}   (${opinion.likePercent.fa()}٪ رضایت)", style = MaterialTheme.typography.bodyMedium)
                opinion.recentComments.forEach { Text("💬 $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** Lets the parent attach the full, approved video (Aparat search or a pasted Aparat/YouTube/Telewebion link). */
@Composable
private fun VideoApprovalCard(item: ContentItem, viewModel: ContentPassportViewModel) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val error by viewModel.searchError.collectAsStateWithLifecycle()
    var query by remember(item.id) { mutableStateOf(item.title) }
    var link by remember(item.id) { mutableStateOf(item.mediaUrl.orEmpty()) }
    KodomoCard(Modifier.fillMaxWidth()) {
        SectionTitle("ویدیوی قابل پخش برای کودک")
        Text(
            when {
                item.mediaUrl != null -> "✅ ویدیوی تأییدشده‌ی شما: ${item.mediaUrl}"
                item.trailerUrl != null -> "فعلاً فقط پیش‌نمایش رسمی (آپارات) پخش می‌شود. می‌توانید نسخه‌ی کامل را انتخاب کنید."
                else -> "هنوز ویدیویی برای این اثر انتخاب نشده و در فهرست کودک نمایش داده نمی‌شود."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        androidx.compose.material3.OutlinedTextField(
            value = link, onValueChange = { link = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text("لینک آپارات، یوتیوب یا تلوبیون") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(onClick = { viewModel.setVideo(link) }, enabled = link.startsWith("http")) { Text("تأیید لینک") }
            if (item.mediaUrl != null) androidx.compose.material3.TextButton(onClick = { viewModel.setVideo(null); link = "" }) { Text("حذف") }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.weight(1f), label = { Text("جستجو در آپارات") })
            androidx.compose.material3.FilledTonalButton(onClick = { viewModel.search(query) }, enabled = query.isNotBlank() && !searching) { Text(if (searching) "…" else "جستجو") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        results.forEach { v ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                coil.compose.AsyncImage(model = v.poster, contentDescription = null, modifier = Modifier.size(width = 96.dp, height = 56.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text("${v.channel} • ${(v.durationSeconds / 60).fa()} دقیقه", style = MaterialTheme.typography.labelSmall)
                }
                androidx.compose.material3.TextButton(onClick = {
                    link = v.pageUrl
                    viewModel.setVideo(v.pageUrl)
                }) { Text("انتخاب") }
            }
        }
    }
}
