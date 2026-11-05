package com.kodomo.app.data.seed

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Interest

/**
 * Compact text format for the bundled catalogs in the `assets/catalog` text files (books, lessons, explore cards):
 *
 * ```
 * @id | title | category | emoji | minAge-maxAge | INTEREST,INTEREST | short description
 * 🐘> first page / step
 * 🌧️> second page / step
 * ```
 * Lines starting with `#` are comments. Everything is original Kodomo writing or retellings of public-domain tales.
 */
object CatalogText {
    private val palettes = listOf(Palette.mint, Palette.sky, Palette.sun, Palette.rose, Palette.lavender, Palette.peach, Palette.forest)

    data class Parsed(val items: List<ContentItem>, val pages: List<ContentPage>)

    fun parse(text: String, type: ContentType, author: String = "تیم نویسندگان کدومو"): Parsed {
        val items = mutableListOf<ContentItem>()
        val pages = mutableListOf<ContentPage>()
        var current: ContentItem? = null
        var index = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
            if (line.startsWith("@")) {
                val f = line.removePrefix("@").split("|").map { it.trim() }
                if (f.size < 7) return@forEach
                val (minAge, maxAge) = f[4].split("-").map { it.trim().toIntOrNull() ?: 0 }.let { it.getOrElse(0) { 0 } to it.getOrElse(1) { 18 } }
                val interests = f[5].split(",").mapNotNull { n -> Interest.entries.firstOrNull { it.name == n.trim() } }.toSet()
                val item = content(
                    id = f[0], type = type, title = f[1], description = f[6], category = f[2], emoji = f[3],
                    colors = palettes[(f[0].hashCode() and 0x7fffffff) % palettes.size],
                    rating = rating(minAge = minAge, maxAge = maxAge, violence = 0, fear = 0, educational = 4, abnormality = 0, creativity = 4, familyValues = 4),
                    durationMinutes = 5, expertName = "تیم محتوای کدومو", author = author, interests = interests,
                    expertReview = f[6],
                )
                items += item
                current = item
                index = 0
            } else {
                val c = current ?: return@forEach
                val split = line.indexOf('>')
                val (emoji, body) = if (split in 1..12) line.substring(0, split).trim() to line.substring(split + 1).trim() else c.emoji to line
                pages += ContentPage(c.id, index++, body, emoji)
            }
        }
        // Reading time from the amount of text.
        val words = pages.groupBy { it.contentId }.mapValues { (_, p) -> p.sumOf { it.text.split(' ').size } }
        return Parsed(items.map { it.copy(durationMinutes = ((words[it.id] ?: 0) / 60 + 2).coerceAtMost(30)) }, pages)
    }

    /** Asset files and their content type. */
    val files = listOf(
        "catalog/books_toddler.txt" to ContentType.BOOK,
        "catalog/books_kid.txt" to ContentType.BOOK,
        "catalog/books_teen.txt" to ContentType.BOOK,
        "catalog/lessons_toddler.txt" to ContentType.LEARNING,
        "catalog/lessons_kid.txt" to ContentType.LEARNING,
        "catalog/lessons_teen.txt" to ContentType.LEARNING,
        "catalog/explore.txt" to ContentType.EXPLORE,
    )

    /** Bump whenever an asset catalog changes so installed apps re-import it. */
    const val VERSION = 1
}
