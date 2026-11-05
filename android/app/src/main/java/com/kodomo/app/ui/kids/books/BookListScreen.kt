package com.kodomo.app.ui.kids.books

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import com.kodomo.app.ui.components.ContentCover
import com.kodomo.app.ui.components.bouncyClick
import androidx.compose.runtime.setValue
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.navigation.ReadingMode

@Composable
fun BookListScreen(onBack: () -> Unit, onOpen: (String, ReadingMode) -> Unit, viewModel: BookListViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val printed by viewModel.printed.collectAsStateWithLifecycle()
    val space = LocalKidSpace.current
    var shelf by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var query by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var printedOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ContentItem?>(null) }
    TrackActivity(ActivityCategory.BOOK, "قفسه‌ی کتاب")
    val shelves = androidx.compose.runtime.remember(books) { books.map { it.category }.distinct() }
    val illustrated = books.filter { it.imageUrl != null }
    val shown = books.filter { (shelf == null || it.category == shelf) && (query.isBlank() || it.title.contains(query.trim())) }
    KidScreenScaffold(title = "دنیای کتاب", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (illustrated.isNotEmpty() && query.isBlank() && shelf == null) {
                item { Text("📗 کتاب‌های مصور واقعی", style = MaterialTheme.typography.titleLarge) }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(illustrated.size) { i ->
                            val b = illustrated[i]
                            Column(
                                Modifier.width(130.dp).bouncyClick { space.guide(b.title); onOpen(b.id, ReadingMode.AUDIO) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ContentCover(b, Modifier.size(width = 130.dp, height = 130.dp), emojiSize = 52, corner = 22.dp)
                                Text(b.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
            item {
                androidx.compose.material3.OutlinedTextField(
                    value = query, onValueChange = { query = it.take(40) }, singleLine = true,
                    placeholder = { Text("دنبال کدام کتاب می‌گردی؟ 🔎") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { androidx.compose.material3.FilterChip(selected = shelf == null, onClick = { shelf = null }, label = { Text("همه (${books.size.fa()})") }) }
                    items(shelves.size) { i ->
                        val c = shelves[i]
                        androidx.compose.material3.FilterChip(selected = shelf == c, onClick = { shelf = c }, label = { Text(c) })
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyState("کتابی پیدا نشد.") }
            items(shown, key = { it.id }) { book -> BookCard(book, space.spec.showCardLabels, onOpen) }
            if (printed.isNotEmpty() && query.isBlank()) {
                item { Text("🛒 کتاب‌های چاپی پیشنهادی (ارزیابی‌شده در کدومو)", style = MaterialTheme.typography.titleLarge) }
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(printed.size.coerceAtMost(40)) { i ->
                            val b = printed[i]
                            Column(Modifier.width(120.dp).bouncyClick { printedOpen = b }, horizontalAlignment = Alignment.CenterHorizontally) {
                                ContentCover(b, Modifier.size(width = 120.dp, height = 160.dp), emojiSize = 48, corner = 16.dp)
                                Text(b.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
    printedOpen?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { printedOpen = null },
            title = { Text(b.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContentCover(b, Modifier.size(width = 120.dp, height = 160.dp), emojiSize = 48, corner = 16.dp)
                    Text("مناسب ${b.rating.minAge.fa()} سال به بالا", style = MaterialTheme.typography.labelLarge)
                    Text(b.description, style = MaterialTheme.typography.bodyMedium)
                    Text("این کتاب چاپی را از مامان و بابا بخواه تا برایت بگیرند 📚", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { printedOpen = null }) { Text("باشه") } },
        )
    }
}

@Composable
private fun BookCard(book: ContentItem, showText: Boolean, onOpen: (String, ReadingMode) -> Unit) {
    KodomoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContentCover(book, Modifier.size(96.dp), emojiSize = 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text(book.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (showText) Text(book.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text("+${book.rating.minAge.fa()} سال • ${book.durationMinutes.fa()} دقیقه", style = MaterialTheme.typography.labelSmall)
                if (book.author.isNotBlank() && book.imageUrl != null) Text(book.author, style = MaterialTheme.typography.labelSmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingMode.entries.forEach { mode ->
                FilledTonalButton(
                    onClick = { onOpen(book.id, mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mode.emoji)
                        Text(mode.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
