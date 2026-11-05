package com.kodomo.app.ui.kids.music

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

@HiltViewModel
class MusicViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext context: Context,
    private val contentRepository: ContentRepository,
    private val rewardRepository: RewardRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val childId = savedState.childId
    val category = MutableStateFlow<String?>(null)
    private val allowed = contentRepository.observeAllowedForChild(childId, ContentType.MUSIC)
    val categories: StateFlow<List<String>> = combine(allowed, category) { items, _ -> items.map { it.category }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tracks: StateFlow<List<ContentItem>> = combine(allowed, category) { items, c -> items.filter { c == null || it.category == c } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<Set<String>> = contentRepository.observeLikedIds(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val current = MutableStateFlow<ContentItem?>(null)
    val isPlaying = MutableStateFlow(false)
    val progress = MutableStateFlow(0f)

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                this@MusicViewModel.isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) this@MusicViewModel.next()
            }
        })
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                val duration = player.duration
                progress.value = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                delay(500)
            }
        }
    }

    fun play(track: ContentItem) {
        val url = track.mediaUrl ?: return
        MusicBox.stop()
        current.value = track
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        viewModelScope.launch {
            val listened = activityRepository.countCompleted(childId, ActivityCategory.MUSIC, minSeconds = 20)
            if (listened >= 5) rewardRepository.award(childId, AchievementCatalog.MUSIC_LOVER)
        }
    }

    fun pause() { if (player.isPlaying) player.pause() }

    fun toggle() {
        if (player.isPlaying) player.pause() else if (current.value != null) player.play()
    }

    fun next() {
        val list = tracks.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == current.value?.id }
        play(list[(index + 1) % list.size])
    }

    fun toggleFavorite(track: ContentItem) = viewModelScope.launch { contentRepository.toggleLike(childId, track.id) }

    override fun onCleared() {
        player.release()
    }
}

@Composable
fun MusicScreen(onBack: () -> Unit, viewModel: MusicViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val playing by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    LaunchedEffect(current?.id) {
        space.setActivity(ActivityCategory.MUSIC, current?.title ?: "موسیقی", current?.id)
    }

    KidScreenScaffold(title = "موسیقی", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { MascotSpeech("${space.child.name} جان، کدوم آهنگ رو با هم گوش بدیم؟ 🎶", MascotMood.HAPPY) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = category == null, onClick = { viewModel.category.value = null }, label = { Text("همه") }) }
                        items(categories) { c -> FilterChip(selected = category == c, onClick = { viewModel.category.value = c }, label = { Text(c) }) }
                    }
                }
                item { MusicBoxCard(onStart = viewModel::pause) }
                item { XylophoneCard(onStart = viewModel::pause) }
                if (tracks.isEmpty()) item {
                    Text(
                        "آهنگ‌های فارسی بیشتر را مامان و بابا از پنل والدین (ارزیابی‌ها) اضافه می‌کنند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                items(tracks, key = { it.id }) { track ->
                    KodomoCard(onClick = {
                        space.guide(track.title)
                        viewModel.play(track)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiCover(track.emoji, track.colorStart, track.colorEnd, Modifier.size(60.dp), emojiSize = 30, corner = 16.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                                Text("${track.category} • ${track.durationMinutes.fa()} دقیقه", style = MaterialTheme.typography.bodySmall)
                            }
                            if (current?.id == track.id && playing) Equalizer(Modifier.size(32.dp))
                            IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                                Icon(
                                    if (track.id in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "علاقه‌مندی", tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                }
            }
            current?.let { track ->
                NowPlayingBar(track, playing, progress, onToggle = viewModel::toggle, onNext = viewModel::next)
            }
        }
    }
}

@Composable
private fun NowPlayingBar(track: ContentItem, playing: Boolean, progress: Float, onToggle: () -> Unit, onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiCover(track.emoji, track.colorStart, track.colorEnd, Modifier.size(52.dp), emojiSize = 26, corner = 14.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Text(track.category, style = MaterialTheme.typography.labelSmall)
            }
            Equalizer(Modifier.size(width = 54.dp, height = 36.dp), active = playing)
            IconButton(onClick = onToggle) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "پخش/توقف") }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = "بعدی") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

/** Animated equalizer bars; idle bars rest low when playback is paused. */
@Composable
fun Equalizer(modifier: Modifier = Modifier, active: Boolean = true, bars: Int = 5) {
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(900), RepeatMode.Restart), label = "phase")
    val colors = listOf(Color(0xFF5FB98A), Color(0xFFFFB648), Color(0xFFF28AA6), Color(0xFF5AA9E6), Color(0xFF9B7FE6))
    Canvas(modifier) {
        val w = size.width / (bars * 1.6f)
        repeat(bars) { i ->
            val level = if (active) 0.25f + 0.75f * ((sin(phase * (1 + i * 0.37f) + i) + 1f) / 2f) else 0.15f
            val h = size.height * level
            drawRoundRect(
                colors[i % colors.size],
                topLeft = Offset(i * w * 1.6f, size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}
