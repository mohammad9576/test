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
