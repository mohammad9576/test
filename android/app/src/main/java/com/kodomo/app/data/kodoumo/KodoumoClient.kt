package com.kodomo.app.data.kodoumo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** One indicator bar of a kodoumo.ir review («شاخص‌های اثر»), 0..5. */
data class KodoumoIndicator(val name: String, val label: String, val rate: Int, val negative: Boolean, val text: String)

data class KodoumoListItem(
    val id: Long,
    val slug: String,
    val title: String,
    val image: String?,
    val shortDescription: String,
    val age: Int?,
    val rate: Float?,
    val underEvaluation: Boolean,
    val permalink: String,
    val typeSlug: String,
)

data class KodoumoReview(
    val id: Long,
    val title: String,
    val latinName: String,
    val typeLabel: String,
    val shortContent: String,
    val age: Int?,
    val rate: Float?,
    val thumbnail: String?,
    val authorName: String,
    val genres: List<String>,
    val durationText: String,
    val year: String,
    val special: String,
    val parentShouldKnow: String,
    val sayToChildren: String,
    val story: String,
    val itemDetail: String,
    val indicators: List<KodoumoIndicator>,
    val galleryImages: List<String>,
    val trailerScript: String?,
    val underEvaluation: Boolean,
)

/**
 * Reads the same JSON API the kodoumo.ir website itself uses (`/wp-json/api/v2/...`). The site is reachable only from
 * inside Iran, so the import runs on the family's phone. Parsing is lenient: unknown/missing fields are tolerated.
 */
@Singleton
class KodoumoClient @Inject constructor(private val http: OkHttpClient, private val json: Json) {

    private suspend fun get(url: String): JsonElement = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", "KodomoApp/1.0 (Android)").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("kodoumo.ir HTTP ${response.code}")
            json.parseToJsonElement(response.body?.string().orEmpty())
        }
    }

    suspend fun category(slug: String, page: Int): Pair<List<KodoumoListItem>, Int> {
        val url = "$BASE/reviews-category/$slug".toHttpUrl().newBuilder().apply {
            if (page > 1) addQueryParameter("page", page.toString())
        }.build().toString()
        val root = get(url).obj
        val items = (root["data"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.toListItem() }
        val pages = root["max_num_pages"].int ?: 1
        return items to pages
    }

    suspend fun review(slug: String): KodoumoReview {
        val root = get("$BASE/reviews/${URLEncoder.encode(java.net.URLDecoder.decode(slug, "UTF-8"), "UTF-8")}").obj
        return parseReview(root)
    }

    /** Parses a review payload; `data` may be an object or a one-element array depending on the endpoint version. */
    internal fun parseReview(root: JsonObject): KodoumoReview {
        val d = when (val data = root["data"]) {
            is JsonObject -> data
            is JsonArray -> data.firstOrNull() as? JsonObject ?: root
            else -> root
        }
        val gallery = d["reviewsGallery"] as? JsonObject
        return KodoumoReview(
            id = d["reviewsID"].long ?: d["id"].long ?: 0L,
            title = d["reviewsTitle"].str,
            latinName = d["reviewsLatinName"].str,
            typeLabel = d["reviewsType"].str,
            shortContent = d["reviewsShortContent"].str.stripHtml(),
            age = parseAge(d["reviewsAgeCategory"]),
            rate = d["reviewsRate"].float,
            thumbnail = d["reviewsThumbnailUrl"].str.ifBlank { null },
            authorName = d["authorName"].str,
            genres = (d["genres"] as? JsonArray).orEmpty().map { it.str }.filter { it.isNotBlank() },
            durationText = d["reviewsDurationTime"].str.stripHtml(),
            year = d["reviewsYearBuild"].str,
            special = d["reviewsSpecial"].str.stripHtml(),
            parentShouldKnow = d["reviewsParentShouldKnow"].str.stripHtml(),
            sayToChildren = d["reviewsSayToChildren"].str.stripHtml(),
            story = d["reviewsItemStory"].str.stripHtml(),
            itemDetail = d["reviewsItemDetail"].str.stripHtml(),
            indicators = findIndicators(d),
            galleryImages = (gallery?.get("images") as? JsonArray).orEmpty().map { it.str }.filter { it.startsWith("http") },
            trailerScript = (gallery?.get("video") as? JsonObject)?.get("src")?.str?.ifBlank { null },
            underEvaluation = d["under_evaluation"].bool,
        )
    }

    /** Category listings use the `reviews*` field names; the home/sidebar widgets use short names – accept both. */
    private fun JsonObject.toListItem(): KodoumoListItem? {
        val slug = this["slug"].str.ifBlank { return null }
        val typeSlug = this["reviewsType"].str
        return KodoumoListItem(
            id = this["id"].long ?: 0L,
            slug = slug,
            title = this["reviewsTitle"].str.ifBlank { this["title"].str }.trim(),
            image = this["reviewsThumbnailUrl"].str.ifBlank { this["image"].str }.ifBlank { null },
            shortDescription = this["reviewsShortContent"].str.ifBlank { this["shortDescription"].str }.stripHtml(),
            age = parseAge(this["reviewsAgeCategory"] ?: this["age"]),
            rate = (this["reviewsRate"] ?: this["rate"]).float,
            underEvaluation = this["under_evaluation"].bool,
            permalink = this["permalink"].str.ifBlank { "https://kodoumo.ir/reviews/${pathOf(typeSlug)}/$slug/" },
            typeSlug = typeSlug,
        )
    }

    /**
     * Indicator bars («شاخص‌های اثر»). Normally `shakhesHa: [{name,label,rate,color,shakhesText}]`, but the list may be
     * keyed by name or nested; every object in the payload that carries a rate and a known name/label is accepted.
     */
    private fun findIndicators(d: JsonObject): List<KodoumoIndicator> {
        val found = linkedMapOf<String, KodoumoIndicator>()
        fun visit(e: JsonElement, keyHint: String?) {
            when (e) {
                is JsonArray -> e.forEach { visit(it, null) }
                is JsonObject -> {
                    toIndicator(e, keyHint)?.let { found.putIfAbsent(it.name, it) }
                    e.forEach { (k, v) -> if (v is JsonObject || v is JsonArray) visit(v, k) }
                }
                else -> Unit
            }
        }
        (d["shakhesHa"] ?: d["shakhes"])?.let { visit(it, null) }
        if (found.isEmpty()) visit(d, null)
        return found.values.toList()
    }

    private fun toIndicator(o: JsonObject, keyHint: String?): KodoumoIndicator? {
        val rateElement = o["rate"] ?: o["value"] ?: o["score"] ?: return null
        val label = (o["label"] ?: o["title"]).str.stripHtml()
        val name = normalizeIndicator(o["name"].str.ifBlank { keyHint.orEmpty() }, label) ?: return null
        val color = o["color"].str
        return KodoumoIndicator(
            name = name,
            label = label.ifBlank { defaultLabels[name].orEmpty() },
            rate = (rateElement.int ?: 0).coerceIn(0, 5),
            negative = if (color.isNotBlank()) color == "red" else name in negativeIndicators,
            text = (o["shakhesText"] ?: o["text"] ?: o["description"]).str.stripHtml(),
        )
    }

    companion object {
        const val BASE = "https://kodoumo.ir/wp-json/api/v2"

        /** Category slugs used by the site (path segments of /reviews-category/...). */
        val categories = listOf("animations", "movies", "serials", "books", "console-games", "mobile-games", "toys")

        private val digits = Regex("\\d+")

        private val negativeIndicators = setOf("violence", "fear", "social_anomalies")
        private val defaultLabels = mapOf(
            "violence" to "خشونت", "fear" to "ترس", "social_anomalies" to "ناهنجاری اجتماعی",
            "positive_messages" to "پیام مثبت", "positive_pattern" to "الگو پذیری", "educational" to "نکات آموزشی", "easy_use" to "سهولت استفاده",
        )

        /** Maps English keys or Persian labels of an indicator to a canonical name. */
        internal fun normalizeIndicator(name: String, label: String): String? {
            val n = name.lowercase().trim()
            val known = setOf("violence", "fear", "social_anomalies", "positive_messages", "positive_pattern", "educational", "easy_use")
            if (n in known) return n
            val t = "$n $label"
            return when {
                "خشونت" in t || "violen" in t -> "violence"
                "ترس" in t || "fear" in t || "danger" in t -> "fear"
                "ناهنجار" in t || "anomal" in t -> "social_anomalies"
                "پیام" in t || "message" in t -> "positive_messages"
                "الگو" in t || "pattern" in t -> "positive_pattern"
                "آموزش" in t || "educat" in t -> "educational"
                "سهولت" in t || "easy" in t -> "easy_use"
                else -> null
            }
        }

        /** URL path segment of a review type (anime lives under animations/, games under games/). */
        fun pathOf(typeSlug: String) = when (typeSlug) {
            "anime" -> "animations/anime"
            "console-games", "mobile-games" -> "games/$typeSlug"
            "" -> "reviews"
            else -> typeSlug
        }

        private val ageTax = Regex("""age-tax[^>]*>([^<]*)<""")

        /**
         * The age arrives as plain text ("7") or as the site's HTML badge, e.g.
         * `<div category="+10" …><img width="40" …><span class="age-tax">7+ </span></div>` – only the age-tax text is the
         * rating (the `category` attribute and image sizes are not). «منتظر رده‌سنی» (awaiting rating) yields null.
         */
        internal fun parseAge(e: JsonElement?): Int? {
            val raw = when (e) {
                null, JsonNull -> return null
                is JsonPrimitive -> e.contentOrNull.orEmpty()
                is JsonArray -> e.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.get("name")?.str }.orEmpty()
                is JsonObject -> (e["name"] ?: e["slug"] ?: e["age"])?.str.orEmpty()
            }
            val text = ageTax.find(raw)?.groupValues?.get(1) ?: raw.stripHtml()
            return digits.find(com.kodomo.app.core.util.PersianDigits.normalize(text))?.value?.toIntOrNull()?.takeIf { it in 0..21 }
        }
    }
}

private val JsonElement.obj: JsonObject get() = this as? JsonObject ?: JsonObject(emptyMap())
private val JsonElement?.str: String get() = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
private val JsonElement?.int: Int? get() = str.let(com.kodomo.app.core.util.PersianDigits::normalize).toDoubleOrNull()?.toInt()
private val JsonElement?.long: Long? get() = str.toLongOrNull()
private val JsonElement?.float: Float? get() = str.toFloatOrNull()?.takeIf { it > 0f }
private val JsonElement?.bool: Boolean get() = str.let { it == "true" || it == "1" }

private val tags = Regex("<[^>]+>")
private val spaces = Regex("[ \\t\\u00A0]+")
private val entities = mapOf("&nbsp;" to " ", "&amp;" to "&", "&quot;" to "\"", "&#8220;" to "«", "&#8221;" to "»", "&hellip;" to "…", "&#8230;" to "…", "&lt;" to "<", "&gt;" to ">", "&#039;" to "'", "&zwnj;" to "‌")

internal fun String.stripHtml(): String {
    var s = replace(Regex("<br\\s*/?>|</p>|</li>", RegexOption.IGNORE_CASE), "\n").replace(tags, "")
    entities.forEach { (k, v) -> s = s.replace(k, v) }
    return s.lines().joinToString("\n") { it.replace(spaces, " ").trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
}
