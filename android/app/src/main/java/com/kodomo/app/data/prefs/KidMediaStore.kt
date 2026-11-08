package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kidMediaStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_kid_media")

/** A photo or video the child took (the file itself lives in the phone gallery, under Pictures/Movies › Kodomo). */
data class KidMedia(val uri: String, val video: Boolean, val takenAt: Long)

@Singleton
class KidMediaStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(childId: Long) = stringPreferencesKey("media_$childId")

    fun observe(childId: Long): Flow<List<KidMedia>> = context.kidMediaStore.data.map { p ->
        p[key(childId)].orEmpty().lines().mapNotNull { l ->
            val parts = l.split('|')
            if (parts.size < 3) null else KidMedia(parts[0], parts[1] == "v", parts[2].toLongOrNull() ?: 0)
        }.sortedByDescending { it.takenAt }
    }

    suspend fun add(childId: Long, media: KidMedia) = context.kidMediaStore.edit { p ->
        val line = "${media.uri}|${if (media.video) "v" else "p"}|${media.takenAt}"
        p[key(childId)] = (listOf(line) + p[key(childId)].orEmpty().lines().filter { it.isNotBlank() }).take(500).joinToString("\n")
    }

    suspend fun remove(childId: Long, uri: String) = context.kidMediaStore.edit { p ->
        p[key(childId)] = p[key(childId)].orEmpty().lines().filter { it.isNotBlank() && !it.startsWith("$uri|") }.joinToString("\n")
    }
}
