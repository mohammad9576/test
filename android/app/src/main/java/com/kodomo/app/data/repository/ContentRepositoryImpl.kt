package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.FeedbackEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.data.local.toEntity
import com.kodomo.app.domain.engine.ContentPolicy
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.PeerOpinion
import com.kodomo.app.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    db: KodomoDatabase,
    private val cloudSync: CloudSyncRepository,
    private val wellbeing: com.kodomo.app.data.prefs.WellbeingStore,
) : ContentRepository {
    private val contentDao = db.contentDao()
    private val feedbackDao = db.feedbackDao()
    private val childDao = db.childDao()
    private val settingsDao = db.settingsDao()

    override fun observeByType(type: ContentType): Flow<List<ContentItem>> =
        contentDao.observeByType(type).map { list -> list.map { it.toDomain() } }

    override fun observeAllowedForChild(childId: Long, type: ContentType): Flow<List<ContentItem>> = combine(
        contentDao.observeByType(type),
        childDao.observe(childId).filterNotNull(),
        settingsDao.observe(childId),
        wellbeing.state,
    ) { all, childEntity, settingsEntity, w ->
        // Unless the parent chose «show everything kodoumo approved», kodoumo's catalogue is for the parent only.
        val items = if (w.kodoumoAutoShow) all else all.filterNot { it.id.startsWith("kodoumo_") }
        val child = childEntity.toDomain()
        val age = child.ageYears()
        val settings = settingsEntity?.toDomain()
        ContentPolicy.rankForChild(
            items.map { it.toDomain() }.filter { ContentPolicy.isAllowed(it, age, settings) },
            child.interests,
        )
    }

    override fun observeItem(id: String): Flow<ContentItem?> = contentDao.observe(id).map { it?.toDomain() }

    override suspend fun getItem(id: String): ContentItem? = contentDao.get(id)?.toDomain()

    override suspend fun getPages(contentId: String): List<ContentPage> = contentDao.pages(contentId).map { it.toDomain() }

    override fun search(query: String, type: ContentType?): Flow<List<ContentItem>> =
        contentDao.search(query.trim(), type).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertCatalog(items: List<ContentItem>, pages: List<ContentPage>) =
        contentDao.upsertCatalog(items.map { it.toEntity() }, pages.map { it.toEntity() })

    override suspend fun submitFeedback(childId: Long, contentId: String, liked: Boolean, comment: String) {
        val existing = feedbackDao.get(childId, contentId)
        feedbackDao.upsert(
            FeedbackEntity(
                id = existing?.id ?: 0,
                childId = childId,
                contentId = contentId,
                liked = liked,
                comment = comment.trim().take(280),
                createdAt = Instant.now(),
            ),
        )
        val child = childDao.get(childId)?.toDomain()
        if (child != null) cloudSync.pushFeedback(contentId, liked, comment.trim(), child.ageYears())
    }

    override fun observePeerOpinion(contentId: String): Flow<PeerOpinion> = combine(
        contentDao.observe(contentId),
        feedbackDao.observeTally(contentId),
        feedbackDao.observeComments(contentId),
    ) { content, tally, comments ->
        PeerOpinion(
            likes = (content?.peerLikes ?: 0) + tally.likes,
            dislikes = (content?.peerDislikes ?: 0) + tally.dislikes,
            recentComments = comments,
        )
    }

    override fun observeChildFeedback(childId: Long, contentId: String): Flow<Boolean?> =
        feedbackDao.observe(childId, contentId).map { it?.liked }

    override fun observeLikedIds(childId: Long): Flow<Set<String>> = feedbackDao.observeLikedIds(childId).map { it.toSet() }

    override suspend fun setMediaUrl(contentId: String, url: String?) = contentDao.setMediaUrl(contentId, url?.trim()?.ifBlank { null })

    override suspend fun toggleLike(childId: Long, contentId: String) {
        val existing = feedbackDao.get(childId, contentId)
        if (existing?.liked == true) {
            feedbackDao.delete(childId, contentId)
        } else {
            feedbackDao.upsert(
                FeedbackEntity(id = existing?.id ?: 0, childId = childId, contentId = contentId, liked = true, comment = "", createdAt = Instant.now()),
            )
        }
    }
}
