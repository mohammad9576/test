package com.kodomo.app.ui.kids.video

import android.net.Uri

/**
 * Iranian services whose kids sections parents can pick from. Families sign in with their own account inside the
 * picker (the login stays in the app's web storage); every title is checked against kodoumo.ir before a child sees it,
 * and the child only ever gets the single approved page.
 */
enum class ContentProvider(
    val label: String,
    val emoji: String,
    val startUrl: String,
    /** The service's own domains (site, login, player, CDN). */
    val hosts: List<String>,
    val book: Boolean = false,
    /** Paths that are a title page (series, film, episode or book) rather than a listing. */
    val itemPath: Regex,
    val needsAccount: Boolean = true,
) {
    TELEWEBION("تلوبیون کودک", "📺", "https://telewebion.ir/kids", listOf("telewebion.ir", "telewebion.com", "twcdn.ir", "telewebion.net"),
    FILIMO("فیلیمو کودک", "🎞️", "https://www.filimo.com/kids", listOf("filimo.com", "filimo.cloud", "sabaidea.com", "sabaidea.cloud", "filimocdn.com", "abrarvan.net"),
    NAMAVA("نماوا کودک", "🍿", "https://www.namava.ir/kids", listOf("namava.ir", "namava.com", "namavacdn.ir", "abrarvan.net"),
    FILMNET("فیلم‌نت کودک", "🎬", "https://filmnet.ir/kids", listOf("filmnet.ir", "filmnet.com", "filmnetcdn.ir", "abrarvan.net"),
    SHEYDA("شیدا", "🌈", "https://www.sheyda.com/t/Free-Animations", listOf("sheyda.com", "sheyda.ir", "sheydacdn.com", "abrarvan.net"),
    APARAT_KIDS("آپارات کودک", "🧸", "https://www.aparatkids.com/", listOf("aparatkids.com", "aparat.com", "aparat.cloud", "aparatcdn.com", "sabaidea.com", "sabaidea.cloud", "abrarvan.net"),
    TAAGHCHE("طاقچه", "📗", "https://taaghche.com/", listOf("taaghche.com", "taaghche.ir", "taaghchecdn.com", "abrarvan.net"), book = true,
        itemPath = Regex("/(book|audiobook)/")),
    FIDIBO("فیدیبو", "📘", "https://fidibo.com/", listOf("fidibo.com", "fidibo.net", "fidibocdn.com", "abrarvan.net"), book = true,
        itemPath = Regex("/(book|audiobook)/")),
    ;

    fun owns(url: String): Boolean = WebLockRules.hostMatches(Uri.parse(url).host.orEmpty().removePrefix("www."), hosts)

    fun isItem(url: String): Boolean = owns(url) && itemPath.containsMatchIn(Uri.parse(url).path.orEmpty())

    companion object {
        val videos = entries.filter { !it.book }
        val books = entries.filter { it.book }
        fun forUrl(url: String): ContentProvider? = entries.firstOrNull { it.owns(url) }

        /** Order of preference for families (Persian-dubbed services first; Aparat Kids is mostly original-language). */
        fun rank(url: String): Int = forUrl(url)?.ordinal ?: entries.size
    }
}
