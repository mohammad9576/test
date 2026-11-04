package com.kodomo.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.kodomo.app.domain.model.ContentItem

/** Poster of a catalog item when it has one (kodoumo.ir imports), otherwise the colourful emoji cover. */
@Composable
fun ContentCover(item: ContentItem, modifier: Modifier = Modifier, emojiSize: Int = 40, corner: Dp = 20.dp) {
    val url = item.imageUrl
    if (url.isNullOrBlank()) {
        EmojiCover(item.emoji, item.colorStart, item.colorEnd, modifier, emojiSize, corner)
        return
    }
    Box(modifier.clip(RoundedCornerShape(corner))) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { EmojiCover(item.emoji, item.colorStart, item.colorEnd, Modifier.fillMaxSize(), emojiSize, corner) },
            error = { EmojiCover(item.emoji, item.colorStart, item.colorEnd, Modifier.fillMaxSize(), emojiSize, corner) },
        )
    }
}
