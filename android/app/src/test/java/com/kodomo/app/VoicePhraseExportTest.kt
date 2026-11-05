package com.kodomo.app

import com.kodomo.app.core.voice.VoiceKeys
import com.kodomo.app.data.seed.BookCatalog
import com.kodomo.app.data.seed.EncyclopediaCatalog
import com.kodomo.app.data.seed.LearningCatalog
import com.kodomo.app.data.seed.MediaCatalog
import com.kodomo.app.domain.engine.VoicePhrases
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Exports every sentence the kids space can narrate to `build/voice/phrases.tsv` (key<TAB>text).
 * CI feeds this file to `tools/generate_voice.py`, which records the narration clips bundled under `assets/voice/`.
 */
class VoicePhraseExportTest {

    @Test
    fun exportNarrationPhrases() {
        val phrases = linkedSetOf<String>()
        phrases += VoicePhrases.all()
        BookCatalog.books.forEach { phrases += it.title }
        BookCatalog.pages.forEach { page ->
            phrases += page.text
            phrases += page.text.split(" ").filter { it.isNotBlank() }
        }
        LearningCatalog.lessons.forEach { phrases += it.title; phrases += it.description }
        LearningCatalog.pages.forEach { phrases += it.text }
        LearningCatalog.explore.forEach { phrases += it.title; phrases += it.description }
        EncyclopediaCatalog.entries.forEach { phrases += it.title; phrases += it.summary; phrases += it.body }
        (MediaCatalog.videos + MediaCatalog.music).forEach { phrases += it.title }
        // Bundled text catalogs: titles and whole sentences of book pages / discovery cards (not single words, to keep
        // the APK small). Lesson steps and project steps are read aloud too.
        // Recorded: books for ages up to 8 and the discovery cards (young children cannot read yet). Teen books, lessons
        // and project steps fall back to the device voice to keep the APK size reasonable.
        com.kodomo.app.data.seed.CatalogText.files.forEach { (path, type) ->
            val file = File("src/main/assets/$path")
            if (!file.exists()) return@forEach
            val parsed = com.kodomo.app.data.seed.CatalogText.parse(file.readText(), type)
            parsed.items.forEach { phrases += it.title }
            if (type == com.kodomo.app.domain.model.ContentType.BOOK || type == com.kodomo.app.domain.model.ContentType.EXPLORE) {
                val young = parsed.items.filter { it.rating.minAge <= 8 }.map { it.id }.toSet()
                parsed.pages.filter { it.contentId in young }.forEach { phrases += it.text }
            }
        }
        com.kodomo.app.domain.engine.ProjectTemplates.all.forEach { phrases += it.title }
        com.kodomo.app.domain.model.Buddy.entries.forEach { phrases += it.title }
        com.kodomo.app.ui.kids.games.adventure.AdventureThemes.all.forEach { theme ->
            theme.stages.forEach { phrases += it.title; phrases += it.story }
            theme.sortColors.forEach { phrases += "فقط رنگ ${it.first}" }
        }
        phrases += listOf("دست‌ها را مثل هواپیما باز کن", "یک دستت را بالا ببر", "هر دو دست را بالا ببر")

        val normalized = phrases.map(VoiceKeys::normalize).filter { it.isNotEmpty() }.distinct()
        val out = File("build/voice/phrases.tsv").apply { parentFile?.mkdirs() }
        out.writeText(normalized.joinToString("\n") { "${VoiceKeys.key(it)}\t$it" } + "\n")
        assertTrue(normalized.size > 100)
        println("Exported ${normalized.size} narration phrases to ${out.absolutePath}")
    }

    @Test
    fun keysAreStable() {
        assertTrue(VoiceKeys.key("سلام  دنیا ") == VoiceKeys.key("سلام دنیا"))
    }
}
