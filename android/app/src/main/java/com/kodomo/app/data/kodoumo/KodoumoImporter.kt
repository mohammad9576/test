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

    /** Reads every page of every category (kodoumo has a few hundred reviews), so parents see the full catalogue. */
    suspend fun sync(pagesPerCategory: Int = 60) {
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
                        // Works still under evaluation are listed for parents (marked, never shown to a child).
                        if (item.underEvaluation || item.age == null) {
                            if (db.contentDao().get("kodoumo_${item.id}") == null) db.contentDao().upsertAll(listOf(pendingEntity(category, item)))
                            imported++
                            continue
                        }
                        val known = db.contentDao().get("kodoumo_${item.id}")
                        if (known != null && known.minAge <= 18 && known.expertReview.isNotBlank() && page > 1) { imported++; continue }
                        val review = runCatching { client.review(item.slug) }.getOrNull() ?: continue
                        if (review.underEvaluation) continue
                        val entity = toEntity(category, item, review)
                        if (entity.id != "kodoumo_${item.id}") db.contentDao().delete("kodoumo_${item.id}")
                        val existingMedia = db.contentDao().mediaUrl(entity.id)
                        db.contentDao().upsertAll(listOf(entity.copy(mediaUrl = existingMedia, episodes = db.contentDao().episodes(entity.id))))
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

    /**
     * Saves a parent-approved video (e.g. a Telewebion cartoon page) together with its kodoumo evaluation. Refuses
     * items that kodoumo has not finished rating, so nothing unrated can reach a child.
     */
    suspend fun saveApprovedVideo(
        item: KodoumoListItem, review: KodoumoReview, mediaUrl: String, episodes: List<com.kodomo.app.domain.model.Episode> = emptyList(),
        book: Boolean = false, sourceLabel: String? = null,
    ): ContentEntity {
        require(!item.underEvaluation && !review.underEvaluation) { "این اثر هنوز در کدومو در حال ارزیابی است." }
        require((review.age ?: item.age) != null) { "کدومو هنوز برای این اثر رده‌ی سنی ثبت نکرده است." }
        val category = videoCategoryOf(item.typeSlug + " " + review.typeLabel)
        // Own id per approved page: several cartoons never overwrite each other (search results may lack a review id).
        val base = toEntity(if (book) "books" else category, item, review)
        val entity = base.copy(
            id = approvedId(mediaUrl), type = if (book) ContentType.BOOK else ContentType.VIDEO, mediaUrl = mediaUrl.trim(),
            episodes = encodeEpisodes(episodes), category = sourceLabel ?: base.category,
        )
        db.contentDao().upsertAll(listOf(entity))
        return entity
    }

    /**
     * A title the parent approved although kodoumo has not evaluated it. It is playable for children aged [minAge]+,
     * clearly marked «تأیید والدین؛ در انتظار ارزیابی», and an evaluation request goes to the Kodomo team.
     */
    suspend fun saveParentApprovedVideo(
        title: String, pageUrl: String, minAge: Int, note: String, episodes: List<com.kodomo.app.domain.model.Episode>,
        book: Boolean = false, sourceLabel: String? = null,
    ): ContentEntity {
        val id = approvedId(pageUrl)
        val entity = ContentEntity(
            id = id, type = if (book) ContentType.BOOK else ContentType.VIDEO, title = title.trim().ifBlank { if (book) "کتاب تأییدشده" else "کارتون تأییدشده" },
            description = if (book) "این کتاب را پدر یا مادرت برایت انتخاب کرده‌اند." else "این کارتون را پدر یا مادرت برایت انتخاب کرده‌اند.",
            body = note, category = sourceLabel ?: "انتخاب والدین", emoji = if (book) "📗" else "📺", colorStart = 0xFFFFE0B2L, colorEnd = 0xFFFB8C00L,
            mediaUrl = pageUrl.trim(), durationMinutes = 0, minAge = minAge.coerceIn(0, 18), maxAge = 18,
            violence = 0, fear = 0, educational = 0, abnormality = 0, creativity = 0, familyValues = 0,
            positives = emptyList(), negatives = emptyList(),
            expertReview = "این اثر هنوز توسط کارشناسان ارزیابی نشده و فقط با تأیید والدین قابل پخش است. درخواست ارزیابی برای تیم کدومو ارسال شد.",
            expertName = "تأیید والدین", peerLikes = 0, peerDislikes = 0, interests = setOf(Interest.MOVIES), author = "",
            sourceUrl = null, episodes = encodeEpisodes(episodes), parentOnly = true,
        )
        db.contentDao().upsertAll(listOf(entity))
        return entity
    }

    private fun approvedId(pageUrl: String): String {
        val key = android.net.Uri.parse(pageUrl.trim()).let { (it.host.orEmpty().removePrefix("www.") + it.path.orEmpty()).trimEnd('/') }
        return com.kodomo.app.domain.engine.ContentPolicy.PARENT_APPROVED_PREFIX + Integer.toHexString(key.hashCode())
    }

    private fun encodeEpisodes(episodes: List<com.kodomo.app.domain.model.Episode>): String? =
        episodes.distinctBy { it.url }.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "${it.url}\t${it.title.replace('\t', ' ').replace('\n', ' ')}" }

    private fun videoCategoryOf(type: String) = when {
        "serial" in type || "سریال" in type -> "serials"
        "movie" in type || "سینمایی" in type || "فیلم" in type -> "movies"
        else -> "animations"
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
    /** A work kodoumo is still evaluating: visible in the parent's ratings list with its status; minAge 99 keeps it from every child. */
    private fun pendingEntity(category: String, item: KodoumoListItem) = ContentEntity(
        id = "kodoumo_${item.id}", type = typeOf(category), title = item.title,
        description = item.shortDescription.ifBlank { "این اثر در کدومو در حال ارزیابی است." },
        body = "⏳ کارشناسان کدومو هنوز ارزیابی این اثر را تمام نکرده‌اند. تا پایان ارزیابی، این اثر به کودک نشان داده نمی‌شود.",
        category = labelOf(category) + " • در حال ارزیابی", emoji = emojiOf(category), colorStart = 0xFFECEFF1L, colorEnd = 0xFF90A4AEL,
        mediaUrl = null, durationMinutes = 0, minAge = 99, maxAge = 99,
        violence = 0, fear = 0, educational = 0, abnormality = 0, creativity = 0, familyValues = 0,
        positives = emptyList(), negatives = emptyList(), expertReview = "", expertName = "کدومو (در حال ارزیابی)",
        peerLikes = 0, peerDislikes = 0, interests = emptySet(), author = "", imageUrl = item.image, sourceUrl = item.permalink.ifBlank { null },
    )

    private fun toEntity(category: String, item: KodoumoListItem, r: KodoumoReview): ContentEntity {
        fun level(vararg names: String): Int? = r.indicators.firstOrNull { it.name in names }?.rate
        // kodoumo.ir uses 0..5 bars; 0 means «not present» and is kept as 0 so charts match the site exactly.
        val violence = (level("violence") ?: 0).coerceIn(0, 5)
        val fear = (level("fear") ?: 0).coerceIn(0, 5)
        val abnormality = (level("social_anomalies", "abnormality") ?: 0).coerceIn(0, 5)
        val positive = (level("positive_messages") ?: 0).coerceIn(0, 5)
        val pattern = (level("positive_pattern") ?: 0).coerceIn(0, 5)
        val educational = (level("educational") ?: level("positive_messages") ?: 0).coerceIn(0, 5)
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
