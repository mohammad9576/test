package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.ParentalSettings

/** Applies the Kodomo rating against a child's age and the parent's tolerance thresholds. */
object ContentPolicy {

    fun isAllowed(item: ContentItem, age: Int, settings: ParentalSettings?): Boolean {
        val r = item.rating
        if (age < r.minAge || age > r.maxAge) return false
        if (settings == null) return r.violence <= 2 && r.fear <= 2 && r.abnormality <= 1
        return r.violence <= settings.maxViolence &&
            r.fear <= settings.maxFear &&
            r.abnormality <= settings.maxAbnormality
    }

    /** Ranks by matching interests first, then by educational score and peer approval. */
    fun rankForChild(items: List<ContentItem>, interests: Set<Interest>): List<ContentItem> =
        items.sortedWith(
            compareByDescending<ContentItem> { it.interests.intersect(interests).size }
                .thenByDescending { it.rating.educational }
                .thenByDescending { it.peerLikes - it.peerDislikes },
        )
}
