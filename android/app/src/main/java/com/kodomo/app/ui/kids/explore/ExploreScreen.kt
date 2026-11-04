package com.kodomo.app.ui.kids.explore

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

/** Social-style but 100% curated discovery feed – no outbound links, no user-generated content. */
@Composable
fun ExploreScreen(onBack: () -> Unit, viewModel: ExploreViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    TrackActivity(ActivityCategory.EXPLORE, "کشف محتوا")

    KidScreenScaffold(title = "کشف محتوا", onBack = onBack) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                MascotSpeech("همه چیز برای تو: امن، تازه و سرگرم‌کننده! 🧭", MascotMood.HAPPY)
            }
            items(feed, key = { it.id }) { post ->
                val tall = post.id.hashCode() % 2 == 0
                KodomoCard(Modifier.fillMaxWidth()) {
                    EmojiCover(post.emoji, post.colorStart, post.colorEnd, Modifier.fillMaxWidth().height(if (tall) 150.dp else 100.dp), emojiSize = if (tall) 64 else 46, corner = 16.dp)
                    Text(post.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    if (space.spec.showCardLabels) Text(post.description, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleLike(post.id) }) {
                            Icon(if (post.id in liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "پسندیدن", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Text((post.peerLikes + if (post.id in liked) 1 else 0).fa(), style = MaterialTheme.typography.labelMedium)
                        Column(Modifier.weight(1f)) {}
                        IconButton(onClick = { space.voice.speak("${post.title}. ${post.description}") }) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "بخوان")
                        }
                    }
                }
            }
        }
    }
}
