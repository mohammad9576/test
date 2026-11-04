package com.kodomo.app.data.kodoumo

import android.util.Log
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ContentEntity
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Interest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports reviews from kodoumo.ir into the local catalog. Only finished evaluations are imported («در حال ارزیابی» is
 * skipped), and every item keeps the site's age rating and indicator scores so ContentPolicy can filter it per child.
 * A parent-chosen full video (mediaUrl) is preserved across re-imports.
 */
@Singleton
class KodoumoImporter @Inject constructor(
    private val client: KodoumoClient,
    private val db: KodomoDatabase,
    private val session: SessionPreferences,
) {
    sealed interface Status {
        data object Idle : Status
        data class Running(val done: Int, val category: String) : Status
        data class Finished(val imported: Int) : Status
        data class Failed(val message: String) : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status.asStateFlow()
    val importedCount: Flow<Int> = db.contentDao().observeImportedCount()
    private val mutex = Mutex()

    /** Runs at most every [minIntervalMillis]; returns immediately if a sync is already running. */
    suspend fun syncIfStale(minIntervalMillis: Long = 12 * 3_600_000L) {
        if (System.currentTimeMillis() - session.current().lastKodoumoSyncMillis < minIntervalMillis) return
        sync()
    }

    suspend fun sync(pagesPerCategory: Int = 4) {
        if (!mutex.tryLock()) return
        try {
            var imported = 0
            var failures = 0
            for (category in KodoumoClient.categories) {
                for (page in 1..pagesPerCategory) {
                    _status.value = Status.Running(imported, labelOf(category))
                    val result = runCatching { client.category(category, page) }
                    val error = result.exceptionOrNull()
                    if (error != null) {
                        failures++
                        Log.w(TAG, "category $category page $page failed", error)
                        break
                    }
                    val (items, pages) = result.getOrThrow()
                    for (item in items) {
                        // Only finished evaluations with a real age rating may reach a child.
                        if (item.underEvaluation || item.age == null) continue
                        val review = runCatching { client.review(item.slug) }.getOrNull() ?: continue
                        if (review.underEvaluation) continue
                        val entity = toEntity(category, item, review)
                        val existingMedia = db.contentDao().mediaUrl(entity.id)
                        db.contentDao().upsertAll(listOf(entity.copy(mediaUrl = existingMedia)))
                        imported++
                        _status.value = Status.Running(imported, labelOf(category))
                    }
                    if (page >= pages) break
                }
            }
            if (imported == 0 && failures > 0) {
                _status.value = Status.Failed("اتصال به kodoumo.ir برقرار نشد. این سایت فقط از اینترنت داخل ایران و بدون فیلترشکن در دسترس است.")
            } else {
                session.setLastKodoumoSync(System.currentTimeMillis())
                _status.value = Status.Finished(imported)
            }
        } finally {
            mutex.unlock()
        }
    }

    private fun labelOf(category: String) = when (category) {
        "animations" -> "انیمیشن"
        "movies" -> "فیلم سینمایی"
        "serials" -> "سریال"
        "books" -> "کتاب"
        "console-games" -> "بازی کنسولی"
        "mobile-games" -> "بازی موبایل"
        else -> "اسباب‌بازی"
    }

    private fun typeOf(category: String) = when (category) {
        "animations", "anime", "movies", "serials" -> ContentType.VIDEO
        "books" -> ContentType.BOOK_REVIEW
        "console-games", "mobile-games" -> ContentType.GAME_REVIEW
        else -> ContentType.TOY_REVIEW
    }

    private fun emojiOf(category: String) = when (category) {
        "animations" -> "🎞️"
        "movies" -> "🎬"
        "serials" -> "📺"
        "books" -> "📖"
        "console-games" -> "🎮"
        "mobile-games" -> "📱"
        else -> "🧸"
    }

    /** Maps the site's 0..5 indicator bars to the Kodomo rating (1..5; 0 = «none» becomes the minimum 1). */
    private fun toEntity(category: String, item: KodoumoListItem, r: KodoumoReview): ContentEntity {
        fun level(vararg names: String): Int? = r.indicators.firstOrNull { it.name in names }?.rate
        val violence = (level("violence") ?: 1).coerceIn(1, 5)
        val fear = (level("fear") ?: 1).coerceIn(1, 5)
        val abnormality = (level("social_anomalies", "abnormality") ?: 1).coerceIn(1, 5)
        val positive = (level("positive_messages") ?: 3).coerceIn(1, 5)
        val pattern = (level("positive_pattern") ?: 3).coerceIn(1, 5)
        val educational = (level("educational", "positive_messages") ?: 3).coerceIn(1, 5)
        val age = r.age ?: item.age ?: 12
        val positives = r.indicators.filter { !it.negative && it.text.isNotBlank() }.map { "${it.label}: ${it.text}" }
        val negatives = r.indicators.filter { it.negative && it.rate > 0 && it.text.isNotBlank() }.map { "${it.label}: ${it.text}" }
        val body = buildString {
            if (r.special.isNotBlank()) appendLine(r.special).appendLine()
            if (r.parentShouldKnow.isNotBlank()) appendLine("آنچه لازم است والدین بدانند:").appendLine(r.parentShouldKnow).appendLine()
            if (r.sayToChildren.isNotBlank()) appendLine("با فرزندان خود صحبت کنید درباره:").appendLine(r.sayToChildren).appendLine()
            if (r.story.isNotBlank()) appendLine("داستان اثر:").appendLine(r.story).appendLine()
            if (r.itemDetail.isNotBlank()) appendLine("جزئیات اثر:").appendLine(r.itemDetail)
        }.trim()
        val title = r.title.ifBlank { item.title }
        return ContentEntity(
            id = "kodoumo_${r.id.takeIf { it > 0 } ?: item.id}",
            type = typeOf(category),
            title = title,
            description = r.shortContent.ifBlank { item.shortDescription },
            body = body,
            category = buildString {
                append(labelOf(category))
                if (r.genres.isNotEmpty()) append(" • ").append(r.genres.joinToString("، "))
            },
            emoji = emojiOf(category),
            colorStart = 0xFFB9E4CBL,
            colorEnd = 0xFF2E7D5BL,
            mediaUrl = null,
            durationMinutes = Regex("\\d+").find(com.kodomo.app.core.util.PersianDigits.normalize(r.durationText))?.value?.toIntOrNull() ?: 0,
            minAge = age.coerceIn(0, 18),
            maxAge = 18,
            violence = violence,
            fear = fear,
            educational = educational,
            abnormality = abnormality,
            creativity = positive,
            familyValues = pattern,
            positives = positives,
            negatives = negatives,
            expertReview = r.parentShouldKnow.ifBlank { r.shortContent },
            expertName = if (r.authorName.isNotBlank()) "${r.authorName} - ارزیاب کدومو" else "ارزیاب کدومو",
            peerLikes = 0,
            peerDislikes = 0,
            interests = interestsOf(category, r.genres),
            author = listOf(r.latinName, r.year).filter { it.isNotBlank() }.joinToString(" • "),
            imageUrl = r.thumbnail ?: item.image,
            trailerUrl = r.trailerScript,
            sourceUrl = item.permalink.ifBlank { "https://kodoumo.ir/" },
        )
    }

    private fun interestsOf(category: String, genres: List<String>): Set<Interest> = buildSet {
        when (category) {
            "animations", "movies", "serials" -> add(Interest.MOVIES)
            "books" -> add(Interest.BOOKS)
            else -> add(Interest.GAMES)
        }
        val g = genres.joinToString(" ")
        if ("علمی" in g || "فضا" in g) add(Interest.ASTRONOMY)
        if ("ورزش" in g) add(Interest.SPORT)
        if ("موزیکال" in g || "موسیقی" in g) add(Interest.MUSIC)
        if ("حیوان" in g || "طبیعت" in g) add(Interest.ANIMALS)
        if ("آموزش" in g) add(Interest.SCIENCE)
    }

    private companion object { const val TAG = "KodoumoImporter" }
}
