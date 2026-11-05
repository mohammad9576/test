package com.kodomo.app

import com.kodomo.app.ui.kids.music.BoxSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBoxTest {
    @Test fun parsesNotes() {
        val t = BoxSong.parse("C4 D#5/0.5 -/2 A4/1.5")
        assertEquals(listOf(60, 75, null, 69), t.map { it.midi })
        assertEquals(listOf(1f, 0.5f, 2f, 1.5f), t.map { it.beats })
    }

    @Test fun everySongParses() {
        BoxSong.entries.forEach { s -> assertTrue(s.name, s.tones.size >= 20 && s.tones.all { (it.midi ?: 60) in 50..90 }) }
    }
}
