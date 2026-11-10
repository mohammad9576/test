package com.kodomo.app.ui.parent.ratings

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType

/**
 * Browsing the kodoumo catalogue: kind (animation, movie, series, book…), genre, age band, evaluation status, sort
 * order and a forgiving Persian search. Pure functions so the list stays fast and is unit-tested.
 */
object RatingsFilter {

    enum class Age(val label: String, val age: Int?) {
        ALL("همه‌ی سنین", null), UNDER_5("زیر ۵ سال", 4), FIVE_7("۵ تا ۷", 6), SEVEN_10("۷ تا ۱۰", 9), TEEN("۱۰ تا ۱۵", 12),
    }

    enum class Sort(val label: String) {
        RECOMMENDED("پیشنهادی"), EDUCATIONAL("آموزنده‌ترین"), CALMEST("کم‌خشونت‌ترین"), YOUNGEST("کم‌سن‌ترها اول"), TITLE("الفبایی"),
    }

    enum class Status(val label: String) { ALL("همه"), RATED("ارزیابی‌شده"), PENDING("در حال ارزیابی"), MINE("انتخاب‌های من") }

    data class Query(
        val text: String = "",
        val kind: String? = null,
        val genre: String? = null,
        val age: Age = Age.ALL,
        val status: Status = Status.ALL,
        val sort: Sort = Sort.RECOMMENDED,
    ) {
        val isDefault get() = this == Query()
    }

    fun isPending(item: ContentItem) = item.rating.minAge > 18

    /** «انیمیشن • کمدی، ماجراجویی» → «انیمیشن»; items without a kodoumo category fall back to their content type. */
    fun kindOf(item: ContentItem): String {
        val first = item.category.substringBefore(" • ").trim()
        return when {
            item.type == ContentType.BOOK || item.type == ContentType.BOOK_REVIEW -> "کتاب"
            item.type == ContentType.GAME_REVIEW -> "بازی"
            item.type == ContentType.TOY_REVIEW -> "اسباب‌بازی"
            item.type == ContentType.MUSIC -> "موسیقی"
            first in setOf("انیمیشن", "فیلم سینمایی", "سریال") -> first
            item.type == ContentType.VIDEO -> "انیمیشن"
            else -> item.type.label
        }
    }

    fun genresOf(item: ContentItem): List<String> =
        item.category.substringAfter(" • ", "").split('،', ',').map { it.trim() }
            .filter { it.isNotEmpty() && it != "در حال ارزیابی" }

    /** Kinds present in the catalogue, most common first, with counts. */
    fun kinds(items: List<ContentItem>): List<Pair<String, Int>> =
        items.groupingBy(::kindOf).eachCount().entries.sortedByDescending { it.value }.map { it.key to it.value }

    /** The most common genres (within the chosen kind). */
    fun genres(items: List<ContentItem>, kind: String?, limit: Int = 14): List<String> =
        items.asSequence().filter { kind == null || kindOf(it) == kind }.flatMap { genresOf(it) }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(limit).map { it.key }

    /** Arabic/Persian letter variants, half-spaces and diacritics are treated the same, so «كتاب» finds «کتاب». */
    fun normalize(s: String): String = buildString(s.length) {
        for (c in s.lowercase()) {
            when (c) {
                'ي', 'ى' -> append('ی')
                'ك' -> append('ک')
                'ة' -> append('ه')
                'أ', 'إ', 'آ' -> append('ا')
                'ۀ' -> append('ه')
                '‌', '‏', '-', '_' -> append(' ')
                in 'ً'..'ْ' -> Unit
                else -> append(c)
            }
        }
    }.replace(Regex("\\s+"), " ").trim()

    fun matchesText(item: ContentItem, text: String): Boolean {
        val words = normalize(text).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val hay = normalize(listOf(item.title, item.category, item.author, item.description, item.expertName).joinToString(" "))
        val hayTight = hay.replace(" ", "")
        return words.all { it in hay || it in hayTight }
    }

    fun apply(items: List<ContentItem>, q: Query, mine: (ContentItem) -> Boolean = { it.mediaUrl != null }): List<ContentItem> {
        val filtered = items.filter { item ->
            (q.kind == null || kindOf(item) == q.kind) &&
                (q.genre == null || q.genre in genresOf(item)) &&
                (q.age.age == null || (!isPending(item) && item.rating.minAge <= q.age.age && item.rating.maxAge >= q.age.age)) &&
                when (q.status) {
                    Status.ALL -> true
                    Status.RATED -> !isPending(item)
                    Status.PENDING -> isPending(item)
                    Status.MINE -> mine(item)
                } &&
                matchesText(item, q.text)
        }
        return when (q.sort) {
            Sort.RECOMMENDED -> filtered.sortedWith(compareBy<ContentItem> { isPending(it) }.thenByDescending { it.mediaUrl != null }.thenByDescending { it.rating.educational + it.rating.familyValues })
            Sort.EDUCATIONAL -> filtered.sortedWith(compareBy<ContentItem> { isPending(it) }.thenByDescending { it.rating.educational })
            Sort.CALMEST -> filtered.sortedWith(compareBy<ContentItem> { isPending(it) }.thenBy { it.rating.violence + it.rating.fear + it.rating.abnormality })
            Sort.YOUNGEST -> filtered.sortedBy { it.rating.minAge }
            Sort.TITLE -> filtered.sortedBy { normalize(it.title) }
        }
    }
}
