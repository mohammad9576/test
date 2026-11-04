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
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.navigation.ReadingMode

@Composable
fun BookListScreen(onBack: () -> Unit, onOpen: (String, ReadingMode) -> Unit, viewModel: BookListViewModel = hiltViewModel()) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val space = LocalKidSpace.current
    TrackActivity(ActivityCategory.BOOK, "قفسه‌ی کتاب")
    KidScreenScaffold(title = "دنیای کتاب", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (books.isEmpty()) item { EmptyState("کتابی برای سن تو پیدا نشد.") }
            items(books, key = { it.id }) { book -> BookCard(book, space.spec.showCardLabels, onOpen) }
        }
    }
}

@Composable
private fun BookCard(book: ContentItem, showText: Boolean, onOpen: (String, ReadingMode) -> Unit) {
    KodomoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiCover(book.emoji, book.colorStart, book.colorEnd, Modifier.size(96.dp), emojiSize = 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text(book.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (showText) Text(book.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text("+${book.rating.minAge.fa()} سال • ${book.durationMinutes.fa()} دقیقه", style = MaterialTheme.typography.labelSmall)
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
