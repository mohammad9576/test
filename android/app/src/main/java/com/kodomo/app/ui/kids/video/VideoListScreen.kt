package com.kodomo.app.ui.kids.video

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity

@Composable
fun VideoListScreen(onBack: () -> Unit, onPlay: (String) -> Unit, viewModel: VideoListViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    TrackActivity(ActivityCategory.VIDEO, "فهرست فیلم‌ها")

    KidScreenScaffold(title = "فیلم و انیمیشن", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (space.spec.showCardLabels) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { viewModel.query.value = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text("جستجو در فیلم‌ها...") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = category == null, onClick = { viewModel.category.value = null }, label = { Text("همه") }) }
                    items(categories) { c -> FilterChip(selected = category == c, onClick = { viewModel.category.value = c }, label = { Text(c) }) }
                }
            }
            if (videos.isEmpty()) {
                item { EmptyState("فعلاً فیلمی مناسب سن و تنظیمات تو پیدا نکردم. از والدینت بخواه فهرست را به‌روز کنند.") }
            }
            items(videos, key = { it.id }) { video ->
                VideoCard(video, downloaded = video.id in downloads, onClick = {
                    space.guide(video.title)
                    onPlay(video.id)
                })
            }
        }
    }
}

@Composable
private fun VideoCard(video: ContentItem, downloaded: Boolean, onClick: () -> Unit) {
    KodomoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiCover(video.emoji, video.colorStart, video.colorEnd, Modifier.size(width = 110.dp, height = 130.dp), emojiSize = 52)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, style = MaterialTheme.typography.titleMedium)
                Text("مدت زمان: ${video.durationMinutes.fa()} دقیقه", style = MaterialTheme.typography.bodySmall)
                Text("رده سنی: +${video.rating.minAge.fa()} سال", style = MaterialTheme.typography.bodySmall)
                Text(video.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👍 ${video.peerLikes.fa()}", style = MaterialTheme.typography.labelMedium)
                    Text("👎 ${video.peerDislikes.fa()}", style = MaterialTheme.typography.labelMedium)
                    if (downloaded) Text("📥 آفلاین", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
