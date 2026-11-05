package com.kodomo.app.ui.kids.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreDetailViewModel @Inject constructor(savedState: SavedStateHandle, private val content: ContentRepository) : ViewModel() {
    private val childId = savedState.childId
    private val id: String = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())

    val item: StateFlow<ContentItem?> = content.observeItem(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val liked: StateFlow<Boolean> = combine(content.observeLikedIds(childId), item) { ids, _ -> id in ids }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val related: StateFlow<List<ContentItem>> = combine(content.observeAllowedForChild(childId, ContentType.EXPLORE), item) { all, current ->
        all.filter { it.id != id && (current == null || it.category == current.category || it.interests.intersect(current.interests).isNotEmpty()) }.take(4)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleLike() = viewModelScope.launch { content.toggleLike(childId, id) }

    val pages = kotlinx.coroutines.flow.MutableStateFlow<List<com.kodomo.app.domain.model.ContentPage>>(emptyList())
    init { viewModelScope.launch { pages.value = content.getPages(id) } }
}

/** Full card of an Explore post: big cover, the story behind it, voice narration and related discoveries. */
@Composable
fun ExploreDetailScreen(contentId: String, onBack: () -> Unit, onOpen: (String) -> Unit = {}, viewModel: ExploreDetailViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val item by viewModel.item.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val related by viewModel.related.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    TrackActivity(ActivityCategory.EXPLORE, item?.title ?: "کشف", contentId)

    KidScreenScaffold(title = item?.category.orEmpty(), onBack = onBack) { padding ->
        val post = item
        if (post == null) {
            EmptyState("در حال باز کردن...", Modifier.padding(padding))
            return@KidScreenScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(post.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (pages.isEmpty()) {
                EmojiCover(post.emoji, post.colorStart, post.colorEnd, Modifier.fillMaxWidth().height(240.dp), emojiSize = 120, corner = 32.dp)
                Text(post.description, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            } else {
                FactPager(post, pages)
            }
            if (post.body.isNotBlank()) Text(post.body, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { space.voice.speakAll((listOf(post.title) + pages.map { it.text }.ifEmpty { listOf(post.description) }).filter { it.isNotBlank() }) }) {
                    Text("🔊 برایم بخوان")
                }
                FilledTonalButton(onClick = { viewModel.toggleLike() }) {
                    Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(6.dp))
                    Text((post.peerLikes + if (liked) 1 else 0).fa())
                }
            }
            if (related.isNotEmpty()) {
                Text("کشف‌های مرتبط", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                related.forEach { r ->
                    KodomoCard(Modifier.fillMaxWidth(), onClick = { onOpen(r.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiCover(r.emoji, r.colorStart, r.colorEnd, Modifier.size(56.dp), emojiSize = 28, corner = 14.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall)
                                Text(r.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The story behind a discovery as swipeable, colourful cards with page dots. */
@Composable
private fun FactPager(post: ContentItem, pages: List<com.kodomo.app.domain.model.ContentPage>) {
    val pager = androidx.compose.foundation.pager.rememberPagerState { pages.size }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.foundation.pager.HorizontalPager(state = pager, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().height(320.dp)) { i ->
        val page = pages[i]
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(post.colorStart), androidx.compose.ui.graphics.Color(post.colorEnd))),
                    androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(page.emoji, fontSize = 88.sp)
            Text(
                page.text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.22f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(12.dp),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pages.size) { i ->
            Box(
                Modifier
                    .size(if (pager.currentPage == i) 12.dp else 8.dp)
                    .background(
                        if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        }
        Spacer(Modifier.width(12.dp))
        if (pager.currentPage < pages.lastIndex) {
            androidx.compose.material3.TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("بعدی ◀") }
        } else {
            Text("🎉 حالا می‌دانی!", style = MaterialTheme.typography.labelLarge)
        }
    }
}
