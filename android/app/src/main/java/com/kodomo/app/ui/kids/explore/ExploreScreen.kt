package com.kodomo.app.ui.kids.explore

import com.kodomo.app.ui.components.bouncyClick
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(savedState: SavedStateHandle, private val contentRepository: ContentRepository) : ViewModel() {
    private val childId = savedState.childId
    val feed: StateFlow<List<ContentItem>> = contentRepository.observeAllowedForChild(childId, ContentType.EXPLORE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liked: StateFlow<Set<String>> = contentRepository.observeLikedIds(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleLike(id: String) = viewModelScope.launch { contentRepository.toggleLike(childId, id) }
}

/**
 * Curated discovery feed (no outbound links, no user content): «fact of the day» hero, topic chips and a colourful
 * grid ordered by the child's interests.
 */
@Composable
fun ExploreScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: ExploreViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    var topic by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    TrackActivity(ActivityCategory.EXPLORE, "کشف محتوا")

    val interests = space.child.interests
    val ordered = androidx.compose.runtime.remember(feed, interests) {
        feed.sortedByDescending { it.interests.intersect(interests).size * 10 + (it.id.hashCode() and 7) }
    }
    val topics = androidx.compose.runtime.remember(feed) { feed.map { it.category }.distinct() }
    val today = androidx.compose.runtime.remember(feed) { feed.takeIf { it.isNotEmpty() }?.get((java.time.LocalDate.now().toEpochDay() % feed.size).toInt()) }
    val shown = ordered.filter { topic == null || it.category == topic }

    KidScreenScaffold(title = "کشف محتوا", onBack = onBack) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            today?.let { fact ->
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(fact.colorStart), androidx.compose.ui.graphics.Color(fact.colorEnd))),
                                androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                            )
                            .bouncyClick { space.guide(fact.title); onOpen(fact.id) }
                            .padding(18.dp),
                    ) {
                        Text("✨ دانستنی امروز", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fact.emoji, fontSize = 64.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(fact.title, style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
                                Text(fact.description, style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f))
                            }
                        }
                    }
                }
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { androidx.compose.material3.FilterChip(selected = topic == null, onClick = { topic = null }, label = { Text("همه") }) }
                    items(topics.size) { i ->
                        val t = topics[i]
                        androidx.compose.material3.FilterChip(selected = topic == t, onClick = { topic = t }, label = { Text(t) })
                    }
                }
            }
            items(shown, key = { it.id }) { post ->
                val tall = (post.id.hashCode() and 1) == 0
                val forYou = post.interests.intersect(interests).isNotEmpty()
                KodomoCard(Modifier.fillMaxWidth(), onClick = {
                    space.guide(post.title)
                    onOpen(post.id)
                }) {
                    Box {
                        EmojiCover(post.emoji, post.colorStart, post.colorEnd, Modifier.fillMaxWidth().height(if (tall) 150.dp else 104.dp), emojiSize = if (tall) 66 else 48, corner = 18.dp)
                        if (forYou) {
                            Text(
                                "💚 مخصوص تو",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(post.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    if (space.spec.showCardLabels) Text(post.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleLike(post.id) }) {
                            Icon(if (post.id in liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "پسندیدن", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Text((post.peerLikes + if (post.id in liked) 1 else 0).fa(), style = MaterialTheme.typography.labelMedium)
                        Column(Modifier.weight(1f)) {}
                        IconButton(onClick = { space.voice.speakAll(listOf(post.title, post.description)) }) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "بخوان")
                        }
                    }
                }
            }
        }
    }
}
