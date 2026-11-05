package com.kodomo.app.data.seed

import androidx.room.withTransaction
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.toEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Populates the local catalog on first launch so the app is fully usable offline before any cloud sync. */
@Singleton
class CatalogSeeder @Inject constructor(
    private val db: KodomoDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {

    /** Imports the bundled text catalogs (books, lessons, explore) when they are new or updated. */
    private suspend fun seedAssetCatalogs() {
        val prefs = context.getSharedPreferences("kodomo_catalog", android.content.Context.MODE_PRIVATE)
        if (prefs.getInt("asset_version", 0) >= CatalogText.VERSION) return
        val items = mutableListOf<com.kodomo.app.domain.model.ContentItem>()
        val pages = mutableListOf<com.kodomo.app.domain.model.ContentPage>()
        CatalogText.files.forEach { (path, type) ->
            val text = runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull() ?: return@forEach
            val parsed = CatalogText.parse(text, type)
            items += parsed.items
            pages += parsed.pages
        }
        if (items.isEmpty()) return
        db.withTransaction { db.contentDao().upsertCatalog(items.map { it.toEntity() }, pages.map { it.toEntity() }) }
        prefs.edit().putInt("asset_version", CatalogText.VERSION).apply()
    }

    suspend fun seedIfEmpty() {
        seedAssetCatalogs()
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
