package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.EncyclopediaEntry
import com.kodomo.app.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Offline, curated knowledge base backed by SQLite FTS4 – the child never reaches the open web. */
@Singleton
class EncyclopediaRepositoryImpl @Inject constructor(db: KodomoDatabase) : EncyclopediaRepository {
    private val dao = db.encyclopediaDao()

    override fun search(query: String, maxAge: Int): Flow<List<EncyclopediaEntry>> {
        val match = buildMatch(query) ?: return flowOf(emptyList())
        return dao.match(match, maxAge).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTopics(maxAge: Int): Flow<List<EncyclopediaEntry>> = dao.observeAll(maxAge).map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Int): EncyclopediaEntry? = dao.get(id)?.toDomain()

    /** Turns free text into a safe FTS MATCH expression with prefix search: «سیاره زمین» -> `سیاره* زمین*`. */
    internal fun buildMatch(query: String): String? {
        val tokens = query
            .replace('ي', 'ی').replace('ك', 'ک')
            .split(Regex("[\\s\\p{Punct}«»؟،؛]+"))
            .map { token -> token.filter { it.isLetterOrDigit() } }
            .filter { it.length >= 2 }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
