package com.kodomo.app

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.KodomoRating
import com.kodomo.app.ui.parent.ratings.RatingsFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingsFilterTest {
    private fun item(id: String, title: String, category: String, min: Int = 5, max: Int = 12, violence: Int = 1, edu: Int = 3, type: ContentType = ContentType.VIDEO) = ContentItem(
        id = id, type = type, title = title, description = "", body = "", category = category, emoji = "", colorStart = 0, colorEnd = 0,
        mediaUrl = null, durationMinutes = 1, rating = KodomoRating(min, max, violence = violence, fear = 0, educational = edu, abnormality = 0, creativity = 0, familyValues = 0),
        positives = emptyList(), negatives = emptyList(), expertReview = "", expertName = "", peerLikes = 0, peerDislikes = 0, interests = emptySet(), author = "",
    )

    private val all = listOf(
        item("a", "شکرستان", "انیمیشن • کمدی، خانوادگی", edu = 5),
        item("b", "پسر دلفینی", "فیلم سینمایی • ماجراجویی", min = 7, violence = 3),
        item("c", "قصه‌های مجید", "سریال • خانوادگی", min = 10, max = 18),
        item("d", "فیلم تازه", "انیمیشن • در حال ارزیابی", min = 99, max = 99),
    )

    @Test fun kindsAndGenres() {
        assertEquals("انیمیشن", RatingsFilter.kindOf(all[0]))
        assertEquals(listOf("کمدی", "خانوادگی"), RatingsFilter.genresOf(all[0]))
        assertTrue(RatingsFilter.genresOf(all[3]).isEmpty())
        assertEquals(listOf("خانوادگی", "کمدی", "ماجراجویی").toSet(), RatingsFilter.genres(all, null).toSet())
    }

    @Test fun filtersCombine() {
        assertEquals(listOf("a", "d"), RatingsFilter.apply(all, RatingsFilter.Query(kind = "انیمیشن", sort = RatingsFilter.Sort.TITLE)).map { it.id }.sorted())
        assertEquals(listOf("c"), RatingsFilter.apply(all, RatingsFilter.Query(age = RatingsFilter.Age.TEEN)).map { it.id })
        assertEquals(listOf("d"), RatingsFilter.apply(all, RatingsFilter.Query(status = RatingsFilter.Status.PENDING)).map { it.id })
        assertEquals(listOf("a", "c"), RatingsFilter.apply(all, RatingsFilter.Query(genre = "خانوادگی")).map { it.id }.sorted())
    }

    @Test fun persianSearchIsForgiving() {
        assertEquals(listOf("c"), RatingsFilter.apply(all, RatingsFilter.Query(text = "قصه هاي مجيد")).map { it.id })
        assertEquals(listOf("b"), RatingsFilter.apply(all, RatingsFilter.Query(text = "دلفيني")).map { it.id })
        assertEquals(listOf("b"), RatingsFilter.apply(all, RatingsFilter.Query(text = "ماجراجویی")).map { it.id })
    }

    @Test fun sortCalmestAndPendingLast() {
        val ids = RatingsFilter.apply(all, RatingsFilter.Query(sort = RatingsFilter.Sort.CALMEST)).map { it.id }
        assertEquals("d", ids.last())
        assertTrue(ids.indexOf("b") > ids.indexOf("a"))
    }
}
