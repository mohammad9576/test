package com.kodomo.app.ui.kids.video

import androidx.compose.foundation.background
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
import com.kodomo.app.ui.components.ContentCover
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity

@Composable
fun VideoListScreen(onBack: () -> Unit, onPlay: (String) -> Unit, onLiveTv: () -> Unit = {}, viewModel: VideoListViewModel = hiltViewModel()) {
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
                item {
                    val asked by viewModel.asked.collectAsStateWithLifecycle()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        EmptyState(if (query.isNotBlank() || category != null) "فیلمی با این جستجو پیدا نکردم." else "هنوز مامان و بابا کارتونی برایت انتخاب نکرده‌اند.")
                        androidx.compose.material3.Button(onClick = onLiveTv, modifier = Modifier.fillMaxWidth()) { Text("📺 تماشای شبکه‌ی پویا (پخش زنده)") }
                        androidx.compose.material3.OutlinedButton(onClick = { viewModel.askForCartoons(space.child.name) }, enabled = !asked, modifier = Modifier.fillMaxWidth()) {
                            Text(if (asked) "✅ به مامان و بابا خبر دادم" else "🙋 از مامان و بابا بخواه کارتون اضافه کنند")
                        }
                    }
                }
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
            ContentCover(video, Modifier.size(width = 96.dp, height = 128.dp), emojiSize = 48, corner = 16.dp)
            Spacer(Modifier.width(14.dp))
            // V5: a calm card — the title big, one quiet line of facts, one badge at most.
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    listOfNotNull(
                        "+${video.rating.minAge.fa()} سال",
                        video.episodes.size.takeIf { it > 1 }?.let { "${it.fa()} قسمت" },
                        video.durationMinutes.takeIf { it > 0 }?.let { "${it.fa()} دقیقه" },
                        "آفلاین".takeIf { downloaded },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val badge = when {
                    video.sourceUrl?.startsWith("https://kodoumo.ir") == true -> "تأییدشده‌ی کدومو"
                    video.parentOnly -> "انتخاب مامان و بابا"
                    video.mediaUrl == null && video.trailerUrl != null -> "پیش‌نمایش"
                    else -> null
                }
                if (badge != null) {
                    Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }
    }
}
