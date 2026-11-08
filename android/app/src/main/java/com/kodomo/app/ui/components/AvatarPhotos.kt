package com.kodomo.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import java.io.File

/**
 * Profile photos picked from the gallery, stored privately as small square JPEGs. Children use their id; the father
 * and mother use [FATHER] and [MOTHER]. [version] changes whenever a photo changes so avatars redraw.
 */
object AvatarPhotos {
    const val FATHER = -1L
    const val MOTHER = -2L
    val version = mutableIntStateOf(0)

    fun file(context: Context, id: Long) = File(File(context.filesDir, "avatars").apply { mkdirs() }, "p_$id.jpg")

    fun has(context: Context, id: Long) = file(context, id).exists()

    fun load(context: Context, id: Long): Bitmap? = file(context, id).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }

    /** Copies [uri] (from the photo picker) as a centred square of 320 px. */
    fun save(context: Context, id: Long, uri: Uri): Boolean = runCatching {
        val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return false
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 320, 320, true)
        file(context, id).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        version.intValue++
        true
    }.getOrDefault(false)

    fun delete(context: Context, id: Long) {
        file(context, id).delete()
        version.intValue++
    }
}
