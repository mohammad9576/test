package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sitesStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_sites")

/** A website the parent approved for the kids browser. */
data class SafeSite(val host: String, val title: String, val emoji: String) {
    val homeUrl: String get() = if (host.contains('/')) "https://$host" else "https://$host/"
    fun encode() = listOf(host, title, emoji).joinToString("\u001F")

    companion object {
        fun decode(raw: String): SafeSite? = raw.split('\u001F').takeIf { it.size == 3 }?.let { SafeSite(it[0], it[1], it[2]) }

        /** Strips scheme, «www.» and trailing slashes: «https://www.Example.com/» → «example.com». */
        fun normalizeHost(input: String): String = input.trim().lowercase()
            .removePrefix("https://").removePrefix("http://").removePrefix("www.")
            .substringBefore('?').substringBefore('#').trimEnd('/')
    }
}

/** Approved sites and the child's pending «may I open this site?» requests. */
@Singleton
class SafeSitesStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val sitesKey = stringSetPreferencesKey("sites")
    private val requestsKey = stringSetPreferencesKey("requests")
    private val bookmarksKey = stringSetPreferencesKey("bookmarks")
    private val historyKey = androidx.datastore.preferences.core.stringPreferencesKey("history")
    private val seededKey = androidx.datastore.preferences.core.booleanPreferencesKey("seeded_v1")

    /** Page bookmarks of the child, «url␟title». */
    val bookmarks: Flow<List<Pair<String, String>>> = context.sitesStore.data.map { p ->
        p[bookmarksKey].orEmpty().mapNotNull { raw -> raw.split('\u001F').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.sortedBy { it.second }
    }

    /** Last visited pages (newest first) for the parent: «epochMillis␟url␟title». */
    val history: Flow<List<Triple<Long, String, String>>> = context.sitesStore.data.map { p ->
        p[historyKey].orEmpty().lines().mapNotNull { l -> l.split('\u001F').takeIf { it.size == 3 }?.let { Triple(it[0].toLongOrNull() ?: 0L, it[1], it[2]) } }
    }

    suspend fun toggleBookmark(url: String, title: String) = context.sitesStore.edit { p ->
        val current = p[bookmarksKey].orEmpty()
        val existing = current.firstOrNull { it.startsWith(url + "\u001F") }
        p[bookmarksKey] = if (existing != null) current - existing else current + (url + "\u001F" + title.ifBlank { url }.take(60))
    }

    suspend fun logVisit(url: String, title: String) = context.sitesStore.edit { p ->
        val lines = p[historyKey].orEmpty().lines().filter { it.isNotBlank() }
        if (lines.firstOrNull()?.split('\u001F')?.getOrNull(1) == url) return@edit
        p[historyKey] = (listOf("${System.currentTimeMillis()}\u001F$url\u001F${title.replace('\n', ' ').take(80)}") + lines).take(200).joinToString("\n")
    }

    suspend fun clearHistory() = context.sitesStore.edit { it.remove(historyKey) }

    /** On first use a small set of well-known children's sites is approved; the parent can remove any of them. */
    suspend fun seedDefaults() = context.sitesStore.edit { p ->
        if (p[seededKey] == true) return@edit
        p[seededKey] = true
        p[sitesKey] = p[sitesKey].orEmpty() + defaults.map { it.copy(host = SafeSite.normalizeHost(it.host)).encode() }
    }

    val sites: Flow<List<SafeSite>> = context.sitesStore.data.map { p -> p[sitesKey].orEmpty().mapNotNull(SafeSite::decode).sortedBy { it.title } }
    val requests: Flow<List<String>> = context.sitesStore.data.map { p -> p[requestsKey].orEmpty().sorted() }

    suspend fun add(site: SafeSite) = context.sitesStore.edit { p ->
        val host = SafeSite.normalizeHost(site.host)
        p[sitesKey] = p[sitesKey].orEmpty().filterNot { SafeSite.decode(it)?.host == host }.toSet() + site.copy(host = host).encode()
        p[requestsKey] = p[requestsKey].orEmpty() - host
    }

    suspend fun remove(host: String) = context.sitesStore.edit { p ->
        p[sitesKey] = p[sitesKey].orEmpty().filterNot { SafeSite.decode(it)?.host == host }.toSet()
    }

    suspend fun request(hostOrUrl: String) = context.sitesStore.edit { p ->
        p[requestsKey] = p[requestsKey].orEmpty() + SafeSite.normalizeHost(hostOrUrl).substringBefore('/')
    }

    suspend fun dismissRequest(host: String) = context.sitesStore.edit { p -> p[requestsKey] = p[requestsKey].orEmpty() - host }

    companion object {
        /** Approved on first use (removable in the parent hub). */
        val defaults = listOf(
            SafeSite("global-asp.github.io", "کتاب‌های مصور جهان (فارسی)", "📗"),
            SafeSite("kids.nationalgeographic.com", "نشنال جئوگرافیک کودکان", "🌍"),
            SafeSite("spaceplace.nasa.gov", "ناسا برای بچه‌ها", "🚀"),
            SafeSite("scratch.mit.edu", "اسکرچ – برنامه‌نویسی تصویری", "🐱"),
        )

        /** One-tap suggestions in the parent screen (the parent still decides; nothing is allowed by default). */
        val suggestions = listOf(
            SafeSite("kodoumo.ir", "کدومو – نقد و رده‌بندی محتوا", "🛡️"),
            SafeSite("scratch.mit.edu", "اسکرچ – برنامه‌نویسی تصویری", "🐱"),
            SafeSite("www.khanacademy.org", "خان آکادمی – ریاضی و علوم", "📐"),
            SafeSite("kids.nationalgeographic.com", "نشنال جئوگرافیک کودکان", "🌍"),
            SafeSite("spaceplace.nasa.gov", "ناسا برای بچه‌ها", "🚀"),
            SafeSite("www.typingclub.com", "آموزش تایپ", "⌨️"),
        )
    }
}
