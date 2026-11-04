package com.kodomo.app

import com.kodomo.app.data.kodoumo.KodoumoClient
import com.kodomo.app.data.kodoumo.stripHtml
import com.kodomo.app.domain.model.VideoSource
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KodoumoParsingTest {
    @Test
    fun ageIsParsedFromAnyShape() {
        assertEquals(7, KodoumoClient.parseAge(JsonPrimitive("7")))
        assertEquals(7, KodoumoClient.parseAge(JsonPrimitive("+۷")))
        assertEquals(12, KodoumoClient.parseAge(buildJsonArray { add(buildJsonObject { put("name", "12+") }) }))
        assertEquals(null, KodoumoClient.parseAge(JsonPrimitive("")))
        val badge = "<div category=\"+10\" class=\"age-label-box \"><img alt=\"kodoumo-logo\" width=\"40\" height=\"40\" src=\"x.png\"> <span class=\"age-tax\">7+ </span></div>"
        assertEquals(7, KodoumoClient.parseAge(JsonPrimitive(badge)))
        val waiting = "<div category=\"+10\" class=\"age-label-box\"><img width=\"40\" src=\"x.png\"> <span class=\"age-tax\">منتظر رده‌سنی</span></div>"
        assertEquals(null, KodoumoClient.parseAge(JsonPrimitive(waiting)))
    }

    @Test
    fun htmlIsCleaned() {
        assertEquals("سلام\nدنیا «خوب»", "<p>سلام</p><b>دنیا</b>&nbsp;&#8220;خوب&#8221;".stripHtml())
    }

    @Test
    fun aparatEmbedScriptBecomesEmbedPlayer() {
        val s = VideoSource.parse("https://www.aparat.com/embed/AbC12?data[rnddiv]=1234&data[responsive]=yes")
        assertTrue(s is VideoSource.Embed && s.embedUrl.contains("videohash/AbC12"))
        assertTrue(VideoSource.parse("https://youtu.be/dQw4w9WgXcQ") is VideoSource.Embed)
        assertTrue(VideoSource.parse("https://example.com/a.mp4") is VideoSource.Direct)
    }
}
