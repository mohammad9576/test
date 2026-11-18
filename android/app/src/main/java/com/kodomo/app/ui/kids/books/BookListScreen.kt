package com.kodomo.app.ui.kids.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.Search
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
    var choose by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ContentItem?>(null) }
    KidScreenScaffold(title = "کتاب‌ها", onBack = onBack) { padding ->
        // V5 (D_CHILD «کتاب‌ها»): search, shelves, big covers in two columns; one tap, then how to read it.
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val full = androidx.compose.foundation.lazy.grid.GridItemSpan(2)
            item(span = { full }) {
                androidx.compose.material3.OutlinedTextField(
                    value = query, onValueChange = { query = it.take(40) }, singleLine = true,
                    placeholder = { Text("جستجوی کتاب…") }, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Search, contentDescription = null) },
                )
            }
            item(span = { full }) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { androidx.compose.material3.FilterChip(selected = shelf == null, onClick = { shelf = null }, label = { Text("همه") }) }
                    items(shelves.size) { i ->
                        val c = shelves[i]
                        androidx.compose.material3.FilterChip(selected = shelf == c, onClick = { shelf = c }, label = { Text(c) })
                    }
                }
            }
            if (shown.isEmpty()) item(span = { full }) { EmptyState("کتابی پیدا نشد.") }
            items(shown.size, key = { shown[it].id }) { i ->
                val b = shown[i]
                Column(Modifier.bouncyClick { space.guide(b.title); choose = b }) {
                    ContentCover(b, Modifier.fillMaxWidth().aspectRatio(0.82f), emojiSize = 64, corner = 20.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(b.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    Text("+${b.rating.minAge.fa()} سال", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (printed.isNotEmpty() && query.isBlank()) {
                item(span = { full }) { Text("کتاب‌های چاپی پیشنهادی", style = MaterialTheme.typography.titleMedium) }
                item(span = { full }) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(printed.size.coerceAtMost(40)) { i ->
                            val b = printed[i]
                            Column(Modifier.width(110.dp).bouncyClick { printedOpen = b }, horizontalAlignment = Alignment.CenterHorizontally) {
                                ContentCover(b, Modifier.size(width = 110.dp, height = 146.dp), emojiSize = 44, corner = 16.dp)
                                Text(b.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
    choose?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { choose = null },
            title = { Text(b.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadingMode.entries.forEach { mode ->
                        FilledTonalButton(
                            onClick = { choose = null; onOpen(b.id, mode) },
                            modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(18.dp),
                        ) { Text("${mode.emoji}  ${mode.label}", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { choose = null }) { Text("بستن") } },
        )
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
