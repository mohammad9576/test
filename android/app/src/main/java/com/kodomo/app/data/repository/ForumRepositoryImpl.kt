package com.kodomo.app.data.repository

import android.util.Log
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ForumPostEntity
import com.kodomo.app.data.local.entity.ForumTopicEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.data.remote.ApiProvider
import com.kodomo.app.data.remote.CreatePostDto
import com.kodomo.app.data.remote.CreateTopicDto
import com.kodomo.app.data.remote.ReportDto
import com.kodomo.app.domain.model.AuthorRole
import com.kodomo.app.domain.model.ForumPost
import com.kodomo.app.domain.model.ForumTopic
import com.kodomo.app.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForumRepositoryImpl @Inject constructor(
    db: KodomoDatabase,
    private val apiProvider: ApiProvider,
    private val session: SessionPreferences,
) : ForumRepository {
    private val dao = db.forumDao()

    override fun observeTopics(): Flow<List<ForumTopic>> = dao.observeTopics().map { l -> l.map { it.toDomain() } }

    override fun observeTopic(id: Long): Flow<ForumTopic?> = dao.observeTopic(id).map { it?.toDomain() }

    override fun observePosts(topicId: Long): Flow<List<ForumPost>> = dao.observePosts(topicId).map { l -> l.map { it.toDomain() } }

    override suspend fun createTopic(title: String, body: String, category: String, authorName: String): Long {
        val remote = whenOnline { it.createTopic(CreateTopicDto(title.trim(), body.trim(), category)) }
        return dao.insertTopic(
            ForumTopicEntity(
                remoteId = remote?.id, title = title.trim(), body = body.trim(), authorName = authorName,
                authorRole = AuthorRole.PARENT, category = category, createdAt = Instant.now(),
            ),
        )
    }

    override suspend fun reply(topicId: Long, body: String, authorName: String, role: AuthorRole) {
        val topic = dao.topic(topicId) ?: return
        val remote = topic.remoteId?.let { remoteTopicId -> whenOnline { it.createPost(remoteTopicId, CreatePostDto(body.trim())) } }
        dao.insertPost(
            ForumPostEntity(
                topicId = topicId, remoteId = remote?.id, body = body.trim(), authorName = authorName, authorRole = role,
                createdAt = Instant.now(), reported = false,
            ),
        )
    }

    override suspend fun report(postId: Long) {
        val post = dao.post(postId) ?: return
        dao.markReported(postId)
        post.remoteId?.let { remoteId -> whenOnline { it.reportPost(remoteId, ReportDto("reported_by_parent")) } }
    }

    override suspend fun refreshFromServer() {
        val topics = whenOnline { it.forumTopics() } ?: return
        for (dto in topics) {
            val localTopicId = dao.topicByRemoteId(dto.id)?.id ?: dao.insertTopic(
                ForumTopicEntity(
                    remoteId = dto.id, title = dto.title, body = dto.body, authorName = dto.authorName,
                    authorRole = roleOf(dto.authorRole), category = dto.category, createdAt = parseInstant(dto.createdAt),
                ),
            )
            for (post in dto.posts) {
                if (dao.postByRemoteId(post.id) != null) continue
                dao.insertPost(
                    ForumPostEntity(
                        topicId = localTopicId, remoteId = post.id, body = post.body, authorName = post.authorName,
                        authorRole = roleOf(post.authorRole), createdAt = parseInstant(post.createdAt), reported = false,
                    ),
                )
            }
        }
    }

    private suspend fun <T> whenOnline(block: suspend (com.kodomo.app.data.remote.KodomoApi) -> T): T? {
        if (session.current().authToken == null) return null
        return try {
            block(apiProvider.api())
        } catch (e: Exception) {
            Log.w("ForumRepository", "Remote forum call failed: ${e.message}")
            null
        }
    }

    private fun roleOf(value: String): AuthorRole = AuthorRole.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AuthorRole.PARENT

    private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }.getOrElse { Instant.now() }
}
