package com.kodomo.app.ui.parent.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.AuthorRole
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ForumPost
import com.kodomo.app.domain.model.ForumTopic
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.ForumRepository
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.navigation.Routes
import com.kodomo.app.ui.parent.ParentHubViewModel
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@Composable
fun CommunityTab(viewModel: ParentHubViewModel, onOpenArticle: (String) -> Unit, onOpenTopic: (Long) -> Unit, modifier: Modifier = Modifier) {
    val sections = listOf("مقالات و نشریه‌ها", "تالار گفتگو", "دورهمی آخر هفته")
    var section by rememberSaveable { mutableIntStateOf(0) }
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val ideas by viewModel.weekendIdeas.collectAsStateWithLifecycle()
    var newTopic by remember { mutableStateOf(false) }

    Column(modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
            sections.forEachIndexed { i, label ->
                SegmentedButton(selected = section == i, onClick = { section = i }, shape = SegmentedButtonDefaults.itemShape(i, sections.size)) {
                    Text(label, maxLines = 1, fontSize = 12.sp)
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (section) {
                0 -> items(articles, key = { it.id }) { a -> ArticleRow(a) { onOpenArticle(a.id) } }
                1 -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("پرسش‌های والدین و پاسخ کارشناسان تأییدشده", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::refreshForum) { Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی") }
                            IconButton(onClick = { newTopic = true }) { Icon(Icons.Rounded.Add, contentDescription = "تاپیک جدید") }
                        }
                    }
                    items(topics, key = { it.id }) { t -> TopicRow(t) { onOpenTopic(t.id) } }
                }
                else -> items(ideas, key = { it.id }) { idea ->
                    KodomoCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiCover(idea.emoji, idea.colorStart, idea.colorEnd, Modifier.size(64.dp), emojiSize = 32, corner = 16.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(idea.title, style = MaterialTheme.typography.titleMedium)
                                Text("${idea.category} • حدود ${idea.durationMinutes.fa()} دقیقه • بدون صفحه‌نمایش", style = MaterialTheme.typography.labelSmall, color = KodomoColors.Green)
                            }
                        }
                        Text(idea.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    if (newTopic) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("عمومی") }
        AlertDialog(
            onDismissRequest = { newTopic = false },
            title = { Text("تاپیک جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it.take(120) }, label = { Text("عنوان") })
                    OutlinedTextField(category, { category = it.take(30) }, label = { Text("دسته") }, singleLine = true)
                    OutlinedTextField(body, { body = it.take(2000) }, label = { Text("متن پرسش") }, minLines = 4)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && body.isNotBlank(), onClick = {
                    viewModel.createTopic(title, body, category)
                    newTopic = false
                }) { Text("انتشار") }
            },
            dismissButton = { TextButton(onClick = { newTopic = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun ArticleRow(article: ContentItem, onClick: () -> Unit) {
    KodomoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiCover(article.emoji, article.colorStart, article.colorEnd, Modifier.size(64.dp), emojiSize = 30, corner = 16.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium)
                Text(article.category, style = MaterialTheme.typography.labelMedium, color = KodomoColors.Green)
                Text(article.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

@Composable
private fun TopicRow(topic: ForumTopic, onClick: () -> Unit) {
    KodomoCard(onClick = onClick) {
        Text(topic.title, style = MaterialTheme.typography.titleMedium)
        Text(topic.body, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${topic.authorName} • ${topic.category}", style = MaterialTheme.typography.labelSmall)
            Text("💬 ${topic.replyCount.fa()}", style = MaterialTheme.typography.labelSmall)
            if (topic.hasExpertAnswer) {
                Row(
                    Modifier.background(KodomoColors.Mint, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = KodomoColors.Green, modifier = Modifier.size(14.dp))
                    Text(" پاسخ کارشناس", style = MaterialTheme.typography.labelSmall, color = KodomoColors.GreenDark)
                }
            }
        }
    }
}

@HiltViewModel
class ParentArticleViewModel @Inject constructor(savedState: SavedStateHandle, contentRepository: ContentRepository) : ViewModel() {
    private val id = android.net.Uri.decode(savedState.get<String>(Routes.ARG_ID).orEmpty())
    val article: StateFlow<ContentItem?> = contentRepository.observeItem(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ParentArticleScreen(contentId: String, onBack: () -> Unit, viewModel: ParentArticleViewModel = hiltViewModel()) {
    val article by viewModel.article.collectAsStateWithLifecycle()
    KidScreenScaffold(title = article?.category.orEmpty(), onBack = onBack) { padding ->
        val a = article ?: return@KidScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiCover(a.emoji, a.colorStart, a.colorEnd, Modifier.fillMaxWidth().size(160.dp), emojiSize = 72)
            Text(a.title, style = MaterialTheme.typography.headlineSmall)
            Text("${a.author} • ${a.durationMinutes.fa()} دقیقه مطالعه", style = MaterialTheme.typography.labelMedium, color = KodomoColors.Green)
            Text(a.description, style = MaterialTheme.typography.titleMedium)
            Text(a.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@HiltViewModel
class ForumTopicViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val forumRepository: ForumRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    private val topicId = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val topic: StateFlow<ForumTopic?> = forumRepository.observeTopic(topicId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val posts: StateFlow<List<ForumPost>> = forumRepository.observePosts(topicId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun reply(body: String) = viewModelScope.launch {
        val family = familyRepository.observeFamily().first()
        val name = family?.let { f -> f.motherName.ifBlank { f.fatherName } }.orEmpty().ifBlank { "والد" }
        forumRepository.reply(topicId, body, "والد ($name)", AuthorRole.PARENT)
    }

    fun report(postId: Long) = viewModelScope.launch { forumRepository.report(postId) }
}

@Composable
fun ForumTopicScreen(topicId: Long, onBack: () -> Unit, viewModel: ForumTopicViewModel = hiltViewModel()) {
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    var reply by remember { mutableStateOf("") }
    KidScreenScaffold(title = "تالار گفتگو", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topic?.let { t ->
                    item {
                        KodomoCard(Modifier.fillMaxWidth()) {
                            Text(t.title, style = MaterialTheme.typography.titleLarge)
                            Text("${t.authorName} • ${PersianCalendar.format(t.createdAt.atZone(ZoneId.systemDefault()).toLocalDate())}", style = MaterialTheme.typography.labelSmall)
                            Text(t.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                items(posts, key = { it.id }) { post ->
                    val expert = post.authorRole.isExpert
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(if (expert) KodomoColors.Mint else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (expert) Icon(Icons.Rounded.Verified, contentDescription = "کارشناس تأییدشده", tint = KodomoColors.Green, modifier = Modifier.size(18.dp))
                            Text(" ${post.authorName} • ${post.authorRole.label}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (!expert) {
                                IconButton(onClick = { viewModel.report(post.id) }, enabled = !post.reported) {
                                    Icon(Icons.Rounded.Flag, contentDescription = "گزارش تخلف", tint = if (post.reported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text(post.body, style = MaterialTheme.typography.bodyMedium)
                        if (post.reported) Text("گزارش شد و در صف بررسی ناظران است.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(reply, { reply = it.take(2000) }, placeholder = { Text("پاسخ شما...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp))
                IconButton(onClick = {
                    viewModel.reply(reply)
                    reply = ""
                }, enabled = reply.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "ارسال") }
            }
        }
    }
}
