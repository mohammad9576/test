package com.kodomo.app.ui.parent.ratings

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

@Composable
fun RatingsTab(viewModel: ParentHubViewModel, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val items by viewModel.ratedContent.collectAsStateWithLifecycle()
    val status by viewModel.kodoumoStatus.collectAsStateWithLifecycle()
    val imported by viewModel.kodoumoCount.collectAsStateWithLifecycle()
    var type by remember { mutableStateOf<ContentType?>(null) }
    var query by remember { mutableStateOf("") }
    val types = listOf(
        ContentType.VIDEO, ContentType.BOOK_REVIEW, ContentType.GAME_REVIEW, ContentType.TOY_REVIEW,
        ContentType.BOOK, ContentType.MUSIC, ContentType.LEARNING,
    )
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("سامانه ارزیابی تربیتی کدومو", style = MaterialTheme.typography.headlineSmall)
            Text("شناسنامه‌ی تخصصی هر اثر: سن پیشنهادی، شاخص‌های مثبت و منفی و دیدگاه کارشناسان.", style = MaterialTheme.typography.bodySmall)
        }
        item {
            KodomoCard(Modifier.fillMaxWidth()) {
                Text("ارزیابی‌های kodoumo.ir", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${imported.fa()} اثر با رده‌بندی سنی و شاخص‌های کدومو دریافت شده است. فیلم‌ها و انیمیشن‌ها فقط در صورتی به کودک نشان داده می‌شوند که سن و سطح خشونت/ترس/ناهنجاری آن‌ها با تنظیمات شما بخواند.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    when (val s = status) {
                        is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Running -> "⏳ در حال دریافت ${s.category}… (${s.done.fa()} اثر)"
                        is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Finished -> "✅ به‌روزرسانی انجام شد (${s.imported.fa()} اثر)"
                        is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Failed -> "⚠️ ${s.message}"
                        com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Idle -> "به‌روزرسانی خودکار: روزی دو بار"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                androidx.compose.material3.FilledTonalButton(
                    onClick = { viewModel.syncKodoumo() },
                    enabled = status !is com.kodomo.app.data.kodoumo.KodoumoImporter.Status.Running,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("دریافت تازه‌ترین ارزیابی‌ها از کدومو") }
            }
        }
        item {
            androidx.compose.material3.OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text("جستجوی عنوان…") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = type == null, onClick = { type = null }, label = { Text("همه") }) }
                items(types) { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) }) }
            }
        }
        items(items.filter { (type == null || it.type == type) && (query.isBlank() || it.title.contains(query.trim())) }, key = { it.id }) { item ->
            KodomoCard(onClick = { onOpen(item.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.kodomo.app.ui.components.ContentCover(item, Modifier.size(width = 64.dp, height = 88.dp), emojiSize = 32, corner = 14.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text("${item.type.label} • سن پیشنهادی: ${item.rating.minAge.fa()} تا ${item.rating.maxAge.fa()} سال", style = MaterialTheme.typography.labelMedium)
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
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.kodomo.app.ui.components.ContentCover(i, Modifier.size(width = 96.dp, height = 132.dp), emojiSize = 48)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(i.title, style = MaterialTheme.typography.headlineSmall)
                    Text("${i.type.label} • ${i.category}", style = MaterialTheme.typography.labelLarge, color = KodomoColors.Green)
                    Text("سن پیشنهادی: ${r.minAge.fa()} تا ${r.maxAge.fa()} سال", style = MaterialTheme.typography.titleSmall)
                    if (i.author.isNotBlank()) Text(i.author, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(i.description, style = MaterialTheme.typography.bodyLarge)
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
