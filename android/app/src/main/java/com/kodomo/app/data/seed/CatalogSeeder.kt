package com.kodomo.app.data.seed

import androidx.room.withTransaction
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.toEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Populates the local catalog on first launch so the app is fully usable offline before any cloud sync. */
@Singleton
class CatalogSeeder @Inject constructor(private val db: KodomoDatabase) {

    suspend fun seedIfEmpty() {
        db.withTransaction {
            if (db.contentDao().count() == 0) {
                val items = MediaCatalog.videos + MediaCatalog.music + BookCatalog.books + LearningCatalog.lessons +
                    LearningCatalog.explore + ParentCatalog.articles + ParentCatalog.weekendIdeas
                val pages = BookCatalog.pages + LearningCatalog.pages
                db.contentDao().upsertCatalog(items.map { it.toEntity() }, pages.map { it.toEntity() })
            }
            if (db.encyclopediaDao().count() == 0) {
                db.encyclopediaDao().insertAll(EncyclopediaCatalog.entries)
            }
            if (db.forumDao().countTopics() == 0) {
                ParentCatalog.forumSeed().forEach { (topic, posts) ->
                    val topicId = db.forumDao().insertTopic(topic)
                    posts.forEach { db.forumDao().insertPost(it.copy(topicId = topicId)) }
                }
            }
        }
    }
}
