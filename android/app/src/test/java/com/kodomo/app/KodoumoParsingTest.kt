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
    fun indicatorsAreReadFromReviewPayload() {
        val client = KodoumoClient(okhttp3.OkHttpClient(), kotlinx.serialization.json.Json)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"status":true,"data":{"reviewsID":66344,"reviewsTitle":"ماهی اخمو","reviewsAgeCategory":"<span class=\"age-tax\">7+ </span>",
            "shakhesHa":[{"name":"violence","label":"خشونت","rate":"2","color":"red","shakhesText":"<p>فانتزی</p>"},
                         {"name":"fear","label":"ترس","rate":3,"color":"red","shakhesText":"دلهره"},
                         {"label":"پیام مثبت","rate":"4","color":"green","shakhesText":"دوستی"}],
            "reviewsGallery":{"images":["https://kodoumo.ir/a.jpg"],"video":{"number":"1","src":"https://www.aparat.com/embed/XyZ12?data[rnddiv]=1"}}}}""",
        ) as kotlinx.serialization.json.JsonObject
        val r = client.parseReview(json)
        assertEquals(7, r.age)
        assertEquals(listOf("violence" to 2, "fear" to 3, "positive_messages" to 4), r.indicators.map { it.name to it.rate })
        assertTrue(r.trailerScript!!.contains("aparat.com/embed/XyZ12"))
    }

    @Test
    fun realKodoumoIndicatorNamesAreRecognised() {
        val client = KodoumoClient(okhttp3.OkHttpClient(), kotlinx.serialization.json.Json)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"data":{"id":66344,"reviewsTitle":"ماهی اخمو","reviewsLatinName":"The Pout-Pout Fish",
            "reviewsAgeCategory":"<div category=\"+10\"><img width=\"40\"> <span class=\"age-tax\">7+ </span></div>","reviewsDurationTime":"92 دقیقه",
            "genres":["ماجراجویی"],"under_evaluation":false,
            "shakhesHa":[{"name":"violence-rate","rate":0,"label":"خشونت","color":"red","shakhesText":"فانتزی"},
              {"name":"danger-section","rate":1,"label":"ترس","color":"red","shakhesText":"دلهره"},
              {"name":"Social-anomalies","rate":1,"label":"ناهنجاری‌ اجتماعی","color":"red","shakhesText":"تمسخر"},
              {"name":"Positive-message","rate":5,"label":"پیام مثبت","color":"green","shakhesText":"دوستی"},
              {"name":"Positive-pattern","rate":4,"label":"الگو پذیری","color":"green","shakhesText":"پشتکار"}],
            "reviewsGallery":{"images":["https://kodoumo.ir/a.jpg"]}}}""",
        ) as kotlinx.serialization.json.JsonObject
        val r = client.parseReview(json)
        assertEquals(66344L, r.id)
        assertEquals(7, r.age)
        assertEquals(
            listOf("violence" to 0, "fear" to 1, "social_anomalies" to 1, "positive_messages" to 5, "positive_pattern" to 4),
            r.indicators.map { it.name to it.rate },
        )
        assertEquals(listOf(true, true, true, false, false), r.indicators.map { it.negative })
        assertEquals(null, r.trailerScript)
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
        assertTrue(VideoSource.parse("https://telewebion.ir/live/pooya") is VideoSource.LockedPage)
        assertTrue(VideoSource.parse("https://www.telewebion.com/episode/0x1234") is VideoSource.LockedPage)
        assertTrue(VideoSource.parse("https://nottelewebion.ir.example.com/x") is VideoSource.Direct)
    }

    @Test
    fun searchResultsAreCollectedFromAnyShape() {
        val root = buildJsonObject {
            put("data", buildJsonObject {
                put("reviews", buildJsonArray {
                    add(buildJsonObject { put("slug", "pout-pout"); put("reviewsTitle", "ماهی اخمو"); put("reviewsType", "animations"); put("reviewsAgeCategory", "<span class=\"age-tax\">3+ </span>") })
                    add(buildJsonObject { put("slug", "x"); put("title", "") })
                })
            })
        }
        val items = KodoumoClient(okhttp3.OkHttpClient(), kotlinx.serialization.json.Json).parseSearch(root)
        assertEquals(1, items.size)
        assertEquals("ماهی اخمو", items[0].title)
        assertEquals(3, items[0].age)
    }

    @Test
    fun telewebionTitlesBecomeSearchTerms() {
        assertEquals("ماهی اخمو", com.kodomo.app.ui.parent.ratings.cleanTelewebionTitle("انیمیشن ماهی اخمو - قسمت ۳ | تلوبیون"))
    }

    @Test
    fun mathQuestionsAlwaysContainTheAnswer() {
        val r = kotlin.random.Random(7)
        repeat(200) {
            val q = com.kodomo.app.ui.kids.games.brain.mathQuestion(1 + it % 10, r)
            assertTrue(q.answer in q.options)
            assertEquals(4, q.options.size)
        }
    }
}
