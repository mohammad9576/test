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
    APARAT("آپارات", "▶️", "https://www.aparat.com/", com.kodomo.app.ui.kids.video.ContentProvider.APARAT_KIDS),
    SHEYDA("شیدا", "🌈", "https://www.sheyda.com/", com.kodomo.app.ui.kids.video.ContentProvider.SHEYDA),
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
    val state by vm.state.collectAsStateWithLifecycle()
    var web by remember { mutableStateOf<WebView?>(null) }
    var url by remember { mutableStateOf(service.url) }
    var title by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    val lock = remember(service) { WebLock.SameSite(service.url, service.kids.hosts, anyHost = true) }
    BackHandler { if (sheet) sheet = false else if (web?.canGoBack() == true) web?.goBack() else onClose() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(service.label, style = MaterialTheme.typography.titleMedium); Text(Uri.parse(url).host.orEmpty(), style = MaterialTheme.typography.labelSmall) } },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } },
        )
    }, bottomBar = {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { sheet = true; vm.search(cleanTelewebionTitle(title).ifBlank { title }) }) { Text("⭐ ارزیابی کدومو") }
            TextButton(onClick = { openCast(context) }) { Text("📺 تلویزیون") }
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text("↗ برنامه") }
            TextButton(onClick = onAddForKids) { Text("➕ بچه‌ها") }
        }
    }) { pad ->
        LockedWebPlayer(
            lock = lock, modifier = Modifier.fillMaxSize().padding(pad), focusPlayer = false,
            onPageChanged = { u, t -> url = u; title = t }, onWebView = { web = it },
        )
    }
    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("ارزیابی کدومو برای «${cleanTelewebionTitle(title).ifBlank { title }}»", style = MaterialTheme.typography.titleMedium) }
                if (state.searching || state.loadingReview) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                state.error?.let { e -> item { Text("⚠️ $e", color = MaterialTheme.colorScheme.error) } }
                val r = state.review
                if (r != null) {
                    item {
                        Text(r.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (r.underEvaluation || r.age == null) "⏳ در حال ارزیابی" else "سن مناسب از نظر کدومو: ${r.age.fa()}+ سال", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleSmall)
                    }
                    items(r.indicators.filter { it.rate > 0 || it.text.isNotBlank() }) { ind ->
                        Column {
                            Text("${if (ind.negative) "⚠️" else "✅"} ${ind.label}: ${"●".repeat(ind.rate)}${"○".repeat((5 - ind.rate).coerceAtLeast(0))}", style = MaterialTheme.typography.labelLarge)
                            if (ind.text.isNotBlank()) Text(ind.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (r.parentShouldKnow.isNotBlank()) item { Text("آنچه والدین باید بدانند:\n${r.parentShouldKnow}", style = MaterialTheme.typography.bodyMedium) }
                    item { TextButton(onClick = vm::clearSelection) { Text("◀ نتیجه‌های دیگر") } }
                } else {
                    if (state.searched && state.results.isEmpty() && !state.searching) item { Text("این اثر در کدومو پیدا نشد.", style = MaterialTheme.typography.bodyMedium) }
                    items(state.results, key = { it.slug }) { item ->
                        Card(onClick = { vm.select(item) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.image != null) AsyncImage(item.image, contentDescription = null, modifier = Modifier.size(48.dp, 66.dp))
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                    Text(item.age?.let { "${it.fa()}+ سال" } ?: "در حال ارزیابی", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
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
