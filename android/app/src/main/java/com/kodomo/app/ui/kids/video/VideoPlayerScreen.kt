package com.kodomo.app.ui.kids.video

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.VideoSource
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.kids.TrackActivity
import kotlinx.coroutines.delay
import java.io.File

/**
 * Kodomo video player on Media3/ExoPlayer with a child-proof touch lock: when locked every touch is swallowed;
 * the parent unlocks by holding the lock button for 3 seconds.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(contentId: String, onBack: () -> Unit, onFinished: () -> Unit, viewModel: VideoPlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val context = LocalContext.current
    var touchLocked by remember { mutableStateOf(false) }
    var unlockProgress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }

    TrackActivity(ActivityCategory.VIDEO, item?.title ?: "فیلم", contentId)
    BackHandler(enabled = touchLocked) { }

    val source = state.source
    if (item == null || source == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (item != null) EmptyState("این فیلم هنوز فایل پخش ندارد.") else CircularProgressIndicator()
        }
        return
    }

    when (val parsed = if (source.startsWith("/")) null else VideoSource.parse(source)) {
        is VideoSource.Embed -> {
            EmbeddedVideoScreen(item.title, parsed.platform.label, onBack, onFinished) { EmbeddedVideoPlayer(parsed, it) }
            return
        }
        is VideoSource.LockedPage -> {
            if (item.episodes.size > 1) {
                SeriesPlayerScreen(item.title, parsed.platform.label, item.episodes, parsed.pageUrl, onBack, onFinished)
            } else {
                EmbeddedVideoScreen(item.title, parsed.platform.label, onBack, onFinished) { LockedWebPlayer(WebLock.Pinned(parsed.pageUrl), it) }
            }
            return
        }
        else -> Unit
    }

    val player = remember(source) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (source.startsWith("/")) android.net.Uri.fromFile(File(source)) else android.net.Uri.parse(source)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onFinished()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(holding) {
        if (!holding) {
            unlockProgress = 0f
            return@LaunchedEffect
        }
        val start = System.currentTimeMillis()
        while (holding && unlockProgress < 1f) {
            unlockProgress = ((System.currentTimeMillis() - start) / 3000f).coerceAtMost(1f)
            delay(30)
        }
        if (unlockProgress >= 1f) {
            touchLocked = false
            holding = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { view -> view.useController = !touchLocked; if (touchLocked) view.hideController() },
            modifier = Modifier.fillMaxSize(),
        )
        if (touchLocked) {
            // Swallow every touch on the video surface.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } },
            )
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!touchLocked) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                }
            } else {
                Box(Modifier.size(48.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (state.offline) Text("پخش آفلاین", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                state.error?.let { Text(it, color = Color(0xFFFFB4A9), style = MaterialTheme.typography.labelSmall) }
            }
            Row {
                if (!touchLocked) {
                    val progress = state.downloadProgress
                    IconButton(onClick = viewModel::download, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
                        when {
                            progress != null -> Text("${(progress * 100).toInt().fa()}٪", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            state.offline -> Icon(Icons.Rounded.DownloadDone, contentDescription = "ذخیره شده", tint = Color.White)
                            else -> Icon(Icons.Rounded.Download, contentDescription = "دانلود آفلاین", tint = Color.White)
                        }
                    }
                }
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                        .background(if (touchLocked) Color(0xFF2E7D5B) else Color.Black.copy(alpha = 0.4f), CircleShape)
                        .pointerInput(touchLocked) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                if (!touchLocked) {
                                    touchLocked = true
                                } else {
                                    holding = true
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                    holding = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (touchLocked && unlockProgress > 0f) {
                        CircularProgressIndicator(progress = { unlockProgress }, color = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Icon(if (touchLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, contentDescription = "قفل لمس صفحه", tint = Color.White)
                }
            }
        }
        if (touchLocked) {
            Text(
                "صفحه قفل است • برای باز کردن، دکمه‌ی قفل را ۳ ثانیه نگه دارید",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Full-screen frame around the locked web embed: title bar, back button and an explicit «finished» action. */
@Composable
internal fun EmbeddedVideoScreen(
    title: String,
    platformLabel: String,
    onBack: () -> Unit,
    onFinished: (() -> Unit)?,
    player: @Composable (Modifier) -> Unit,
) {
    var full by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (full) {
        // Kodomo's own fullscreen: sideways, nothing but the player and a back button.
        androidx.activity.compose.BackHandler { full = false }
        Landscape()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            player(Modifier.fillMaxSize())
            FullscreenBackButton({ full = false }, Modifier.align(Alignment.TopStart))
        }
        return
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("پخش امن از $platformLabel • قفل‌شده روی همین صفحه", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { full = true }, modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)) {
                Text("⛶", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        player(Modifier.fillMaxWidth().weight(1f))
        if (onFinished != null) {
            androidx.compose.material3.Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("تماشا تمام شد؛ نظرم را بگویم ✅")
            }
        }
    }
}

/**
 * A series approved by the parent: the child chooses an episode from Kodomo's own list (never from the website), and
 * the player is pinned to that one episode page. «Next episode» moves along the approved list.
 */
@Composable
private fun SeriesPlayerScreen(
    title: String,
    platformLabel: String,
    episodes: List<com.kodomo.app.domain.model.Episode>,
    startUrl: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    var index by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(episodes.indexOfFirst { WebLockRules.samePage(it.url, startUrl) }.coerceAtLeast(0))
    }
    val episode = episodes[index]
    EmbeddedVideoScreen("$title • ${episode.title.ifBlank { "قسمت ${(index + 1).fa()}" }}", platformLabel, onBack, onFinished) { modifier ->
        Column(modifier) {
            androidx.compose.runtime.key(episode.url) {
                LockedWebPlayer(WebLock.Pinned(episode.url), Modifier.fillMaxWidth().weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { index-- }, enabled = index > 0) { Text("◀ قبلی", color = Color.White) }
                Text(
                    "قسمت ${(index + 1).fa()} از ${episodes.size.fa()}",
                    color = Color.White,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                androidx.compose.material3.Button(onClick = { index++ }, enabled = index < episodes.lastIndex) { Text("بعدی ▶") }
            }
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                items(episodes.size) { i ->
                    val selected = i == index
                    Column(
                        Modifier
                            .size(width = 120.dp, height = 64.dp)
                            .background(if (selected) com.kodomo.app.ui.theme.KodomoColors.Green else Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .clickable { index = i }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("قسمت ${(i + 1).fa()}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        if (episodes[i].title.isNotBlank()) {
                            Text(episodes[i].title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
