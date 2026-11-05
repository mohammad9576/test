package com.kodomo.app.ui.fx

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 3D illustrations (Microsoft Fluent Emoji, MIT) bundled in `assets/emoji3d`, used instead of the phone's emoji font
 * wherever the app shows an emoji as a picture. Falls back to the font for anything not bundled.
 */
object Emoji3D {
    @Volatile private var available: Set<String>? = null
    private val cache = LruCache<String, ImageBitmap>(160)
    private val missing = HashSet<String>()

    fun key(emoji: String): String = emoji.codePoints().toArray().filter { it != 0xFE0F && it != 0x200D }.joinToString("_") { Integer.toHexString(it) }

    private fun names(context: Context): Set<String> = available ?: synchronized(this) {
        available ?: (runCatching { context.assets.list("emoji3d")?.map { it.substringBefore('.') }?.toSet() }.getOrNull().orEmpty()).also { available = it }
    }

    fun has(context: Context, emoji: String) = key(emoji) in names(context)

    fun bitmap(context: Context, emoji: String): ImageBitmap? {
        val k = key(emoji)
        cache.get(k)?.let { return it }
        if (k in missing || k !in names(context)) return null
        val bmp = runCatching { context.assets.open("emoji3d/$k.webp").use { BitmapFactory.decodeStream(it) } }.getOrNull()
        if (bmp == null) { missing += k; return null }
        return bmp.asImageBitmap().also { cache.put(k, it) }
    }
}

/** An emoji drawn as a 3D picture (or the font emoji if no picture is bundled). */
@Composable
fun Emoji(emoji: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image = remember(emoji) { Emoji3D.bitmap(context, emoji) }
    if (image != null) Image(image, contentDescription = null, modifier = modifier.size(size))
    else Text(emoji, fontSize = (size.value * 0.8f).sp, modifier = modifier)
}

/** Remembers a lookup for drawing emoji pictures inside a Canvas. */
@Composable
fun rememberEmojiPainter(): (String) -> ImageBitmap? {
    val context = LocalContext.current
    return remember { { e: String -> Emoji3D.bitmap(context, e) } }
}

/** Draws [image] centred at [center] with the given side length in pixels. */
fun DrawScope.drawEmojiImage(image: ImageBitmap, center: Offset, sizePx: Float, alpha: Float = 1f) {
    val s = sizePx.toInt().coerceAtLeast(1)
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((center.x - s / 2f).toInt(), (center.y - s / 2f).toInt()),
        dstSize = IntSize(s, s),
        alpha = alpha,
    )
}

internal val DefaultEmojiSize = 40.dp
