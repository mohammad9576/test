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
