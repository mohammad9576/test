package com.kodomo.app.data.seed

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.KodomoRating

internal fun content(
    id: String,
    type: ContentType,
    title: String,
    description: String,
    category: String,
    emoji: String,
    colors: Pair<Long, Long>,
    rating: KodomoRating,
    body: String = "",
    mediaUrl: String? = null,
    durationMinutes: Int = 0,
    positives: List<String> = emptyList(),
    negatives: List<String> = emptyList(),
    expertReview: String = "",
    expertName: String = "",
    peerLikes: Int = 0,
    peerDislikes: Int = 0,
    interests: Set<Interest> = emptySet(),
    author: String = "",
) = ContentItem(
    id = id, type = type, title = title, description = description, body = body, category = category, emoji = emoji,
    colorStart = colors.first, colorEnd = colors.second, mediaUrl = mediaUrl, durationMinutes = durationMinutes,
    rating = rating, positives = positives, negatives = negatives, expertReview = expertReview, expertName = expertName,
    peerLikes = peerLikes, peerDislikes = peerDislikes, interests = interests, author = author,
)

internal fun rating(
    minAge: Int,
    maxAge: Int = 18,
    violence: Int = 1,
    fear: Int = 1,
    educational: Int = 3,
    abnormality: Int = 1,
    creativity: Int = 3,
    familyValues: Int = 3,
) = KodomoRating(minAge, maxAge, violence, fear, educational, abnormality, creativity, familyValues)

internal fun pages(contentId: String, vararg items: Pair<String, String>): List<ContentPage> =
    items.mapIndexed { index, (emoji, text) -> ContentPage(contentId, index, text, emoji) }

internal object Palette {
    val mint = 0xFFB9E4CBL to 0xFF5FB98AL
    val sky = 0xFFBFE3FFL to 0xFF5AA9E6L
    val sun = 0xFFFFE6A7L to 0xFFFFB648L
    val rose = 0xFFFFD1DCL to 0xFFF28AA6L
    val lavender = 0xFFE2D6FFL to 0xFF9B7FE6L
    val peach = 0xFFFFD9C2L to 0xFFF59E6CL
    val night = 0xFF283A5BL to 0xFF101B33L
    val forest = 0xFFCFE8C5L to 0xFF6BAE5CL
}
