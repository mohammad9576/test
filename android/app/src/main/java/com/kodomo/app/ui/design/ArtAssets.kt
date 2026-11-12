package com.kodomo.app.ui.design

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

/**
 * Artist-made assets in `assets/art/` (see docs/design/ASSET_REQUEST_FA.md). When a file exists it replaces the
 * drawn-in-code version, so new artwork can be dropped in without touching the screens.
 */
object ArtAssets {
    private val cache = LruCache<String, ImageBitmap>(48)
    @Volatile private var index: Set<String>? = null

    private fun files(context: Context): Set<String> = index ?: synchronized(this) {
        index ?: buildSet {
            listOf("characters", "robot", "buddies", "icons", "scenes", "games", "quests", "play", "states", "crafts", "heroes", "onboarding", "parent").forEach { dir ->
                runCatching { context.assets.list("art/$dir") }.getOrNull()?.forEach { add("$dir/${it.substringBeforeLast('.')}") }
            }
        }.also { index = it }
    }

    fun has(context: Context, name: String) = name in files(context)

    fun load(context: Context, name: String): ImageBitmap? {
        if (!has(context, name)) return null
        cache.get(name)?.let { return it }
        val file = listOf("png", "webp").firstNotNullOfOrNull { ext -> runCatching { context.assets.open("art/$name.$ext") }.getOrNull() } ?: return null
        return file.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()?.also { cache.put(name, it) }
    }
}

/** Shows `assets/art/<name>` if present, otherwise [fallback]. */
@Composable
fun Art(name: String, size: Dp, modifier: Modifier = Modifier, fallback: @Composable () -> Unit) {
    val context = LocalContext.current
    val img = remember(name) { ArtAssets.load(context, name) }
    if (img != null) Image(img, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier.size(size)) else fallback()
}
