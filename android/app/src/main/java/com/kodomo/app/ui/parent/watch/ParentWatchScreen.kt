package com.kodomo.app.ui.parent.watch

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import com.kodomo.app.ui.parent.ratings.TitleMatch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.video.LockedWebPlayer
import com.kodomo.app.ui.kids.video.WebLock
import com.kodomo.app.ui.parent.ratings.TelewebionPickerViewModel
import com.kodomo.app.ui.parent.ratings.cleanTelewebionTitle

/** The full services for grown-ups (not only their kids sections). */
enum class WatchService(val label: String, val emoji: String, val url: String, val kids: com.kodomo.app.ui.kids.video.ContentProvider) {
    TELEWEBION("تلوبیون", "📺", "https://telewebion.ir/", com.kodomo.app.ui.kids.video.ContentProvider.TELEWEBION),
    FILIMO("فیلیمو", "🎞️", "https://www.filimo.com/", com.kodomo.app.ui.kids.video.ContentProvider.FILIMO),
    NAMAVA("نماوا", "🍿", "https://www.namava.ir/", com.kodomo.app.ui.kids.video.ContentProvider.NAMAVA),
    FILMNET("فیلم‌نت", "🎬", "https://filmnet.ir/", com.kodomo.app.ui.kids.video.ContentProvider.FILMNET),
    SHEYDA("شیدا", "🌈", "https://www.sheyda.com/", com.kodomo.app.ui.kids.video.ContentProvider.SHEYDA),
    APARAT("آپارات", "▶️", "https://www.aparat.com/", com.kodomo.app.ui.kids.video.ContentProvider.APARAT_KIDS),
    TAAGHCHE("طاقچه", "📗", "https://taaghche.com/", com.kodomo.app.ui.kids.video.ContentProvider.TAAGHCHE),
    FIDIBO("فیدیبو", "📘", "https://fidibo.com/", com.kodomo.app.ui.kids.video.ContentProvider.FIDIBO),
}

/**
 * «تماشا برای والدین»: the complete Telewebion, Filimo, Namava… for the parents themselves — no locks at all. A tap
 * shows kodoumo's rating of whatever page is open, the phone can send the picture to the TV (Smart View / Cast), the
 * page can open in the service's own app, and a title can be added for the children from here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentWatchScreen(onBack: () -> Unit, onAddForKids: (com.kodomo.app.ui.kids.video.ContentProvider) -> Unit) {
    var service by remember { mutableStateOf<WatchService?>(null) }
    val s = service
    if (s == null) {
        Scaffold(topBar = {
            TopAppBar(title = { Text("تماشا برای والدین") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } })
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("نسخه‌ی کامل سرویس‌ها برای خودتان، بدون هیچ محدودیتی. هر جا خواستید ارزیابی کدومو را ببینید، روی تلویزیون پخش کنید یا برای بچه‌ها اضافه کنید.", style = MaterialTheme.typography.bodyMedium)
                LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(WatchService.entries.toList()) { w ->
                        Card(onClick = { service = w }) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Emoji(w.emoji, 52.dp)
                                Text(w.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        return
    }
    WatchBrowser(s, onClose = { service = null }, onAddForKids = { onAddForKids(s.kids) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchBrowser(service: WatchService, onClose: () -> Unit, onAddForKids: () -> Unit) {
    val context = LocalContext.current
    val vm: TelewebionPickerViewModel = hiltViewModel()
    vm.provider = service.kids
    val state by vm.state.collectAsStateWithLifecycle()
    var web by remember { mutableStateOf<WebView?>(null) }
    var url by remember { mutableStateOf(service.url) }
    var title by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var poster by remember { mutableStateOf<String?>(null) }
    var episodes by remember { mutableStateOf<List<com.kodomo.app.domain.model.Episode>>(emptyList()) }
    var sheet by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    val lock = remember(service) { WebLock.SameSite(service.url, service.kids.hosts, anyHost = true) }
    val onItem = service.kids.isItem(url)

    /** Reads the page's real name (og:title / heading), its poster and episode links, then searches kodoumo. */
    fun check(open: Boolean) {
        val page = url
        val w = web
        if (w == null) { query = cleanTelewebionTitle(title); vm.search(query); if (open) sheet = true; return }
        w.evaluateJavascript(com.kodomo.app.ui.parent.ratings.PAGE_INFO_JS) { raw ->
            val info = com.kodomo.app.ui.parent.ratings.parsePageInfo(raw)
            poster = info?.poster
            query = listOf(info?.title, info?.heading, title).map { cleanTelewebionTitle(it.orEmpty()) }.firstOrNull { it.length >= 2 } ?: title
            vm.search(query)
        }
        w.evaluateJavascript(com.kodomo.app.ui.parent.ratings.COLLECT_EPISODES_JS) { raw ->
            val found = com.kodomo.app.ui.parent.ratings.parseEpisodeLinks(raw, service.kids)
            episodes = if (found.none { com.kodomo.app.ui.kids.video.WebLockRules.samePage(it.url, page) }) listOf(com.kodomo.app.domain.model.Episode(page, title)) + found else found
        }
        if (open) sheet = true
    }

    // Opening a film or series page checks kodoumo by itself; the result shows on the bar below without a tap.
    androidx.compose.runtime.LaunchedEffect(url, onItem) {
        if (onItem) { kotlinx.coroutines.delay(1_500); vm.reset(); check(open = false) } else vm.reset()
    }
    LaunchedEffect(state.saved) { state.saved?.let { savedNote = it; sheet = false } }

    BackHandler { if (sheet) sheet = false else if (web?.canGoBack() == true) web?.goBack() else onClose() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(service.label, style = MaterialTheme.typography.titleMedium); Text(Uri.parse(url).host.orEmpty(), style = MaterialTheme.typography.labelSmall) } },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } },
        )
    }, bottomBar = {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            if (onItem) {
                // The kodoumo verdict for the film that is open, always visible.
                val best = state.results.firstOrNull()?.takeIf { TitleMatch.score(query, it.title) >= 0.5 }
                Card(onClick = { if (state.results.isEmpty() && !state.searching) check(open = true) else sheet = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (best?.image != null) AsyncImage(best.image, contentDescription = null, modifier = Modifier.size(36.dp, 50.dp))
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                when {
                                    state.searching -> "در حال یافتن ارزیابی کدومو…"
                                    best != null -> "کدومو: «${best.title}» • " + (best.age?.let { "مناسب ${it.fa()}+ سال" } ?: "در حال ارزیابی")
                                    state.results.isNotEmpty() -> "چند اثر مشابه در کدومو پیدا شد؛ برای انتخاب بزنید"
                                    state.searched -> "این اثر در کدومو پیدا نشد"
                                    else -> "ارزیابی کدومو"
                                },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text("برای جزئیات و نمایش برای فرزندتان بزنید", style = MaterialTheme.typography.labelSmall)
                        }
                        if (state.searching) androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
            savedNote?.let { Text(it, color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp)) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { check(open = true) }) { Text("⭐ ارزیابی و افزودن") }
                TextButton(onClick = { openCast(context) }) { Text("📺 تلویزیون") }
                TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text("↗ برنامه") }
                TextButton(onClick = onAddForKids) { Text("🧸 بخش کودک") }
            }
        }
    }) { pad ->
        LockedWebPlayer(
            lock = lock, modifier = Modifier.fillMaxSize().padding(pad), focusPlayer = false,
            onPageChanged = { u, t -> url = u; title = t }, onWebView = { web = it },
        )
    }
    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Text("ارزیابی کدومو و نمایش برای فرزندان", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            com.kodomo.app.ui.parent.ratings.CheckPanel(
                state = state,
                pageUrl = url,
                pageTitle = title,
                query = query,
                onQuery = { query = it },
                onSearch = { vm.search(query) },
                onSelect = vm::select,
                onClear = vm::clearSelection,
                episodes = episodes.ifEmpty { listOf(com.kodomo.app.domain.model.Episode(url, title)) },
                onApprove = { vm.approve(url, episodes.ifEmpty { listOf(com.kodomo.app.domain.model.Episode(url, title)) }) },
                onApproveWithoutRating = { minAge, note ->
                    vm.approveWithoutRating(query.ifBlank { cleanTelewebionTitle(title) }, url, minAge, note, episodes.ifEmpty { listOf(com.kodomo.app.domain.model.Episode(url, title)) })
                },
                modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                pagePoster = poster,
                book = service.kids.book,
                kidsChooser = {
                    val kids by vm.children.collectAsStateWithLifecycle()
                    val chosen by vm.chosen.collectAsStateWithLifecycle()
                    com.kodomo.app.ui.parent.ratings.KidsChooser(kids, chosen ?: kids.map { it.id }.toSet(), vm::toggleChild)
                },
            )
        }
    }
}

/** Opens the phone's screen-casting (Smart View on Samsung, «Cast» / «Screen mirroring» elsewhere). */
private fun openCast(context: android.content.Context) {
    val tries = listOf(
        Intent("android.settings.CAST_SETTINGS"),
        Intent().setClassName("com.samsung.android.smartmirroring", "com.samsung.android.smartmirroring.CaptureActivity"),
        Intent("com.xiaomi.mirror.action.START"),
        Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
    )
    for (i in tries) if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
}
