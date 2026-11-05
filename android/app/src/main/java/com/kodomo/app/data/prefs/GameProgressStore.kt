package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_games")

/** Per-child, per-game level (1..[MAX_LEVEL]) and best score. A passed level unlocks the next one. */
@Singleton
class GameProgressStore @Inject constructor(@ApplicationContext private val context: Context) {

    private fun levelKey(childId: Long, game: String) = intPreferencesKey("level_${childId}_$game")
    private fun bestKey(childId: Long, game: String) = intPreferencesKey("best_${childId}_$game")
    private fun playsKey(childId: Long, game: String) = intPreferencesKey("plays_${childId}_$game")

    fun observeLevels(childId: Long): Flow<Map<String, Int>> = context.gameStore.data.map { p ->
        p.asMap().entries
            .filter { it.key.name.startsWith("level_${childId}_") }
            .associate { it.key.name.removePrefix("level_${childId}_") to (it.value as? Int ?: 1) }
    }

    suspend fun level(childId: Long, game: String): Int = context.gameStore.data.first()[levelKey(childId, game)] ?: 1

    /** Records a play. Returns the new level (unchanged when the level was not passed). */
    suspend fun record(childId: Long, game: String, score: Int, passed: Boolean): Int {
        var newLevel = 1
        context.gameStore.edit { p ->
            val current = p[levelKey(childId, game)] ?: 1
            newLevel = if (passed) (current + 1).coerceAtMost(MAX_LEVEL) else current
            p[levelKey(childId, game)] = newLevel
            p[bestKey(childId, game)] = maxOf(p[bestKey(childId, game)] ?: 0, score)
            p[playsKey(childId, game)] = (p[playsKey(childId, game)] ?: 0) + 1
        }
        return newLevel
    }

    suspend fun setLevel(childId: Long, game: String, level: Int) {
        context.gameStore.edit { it[levelKey(childId, game)] = level.coerceIn(1, MAX_LEVEL) }
    }

    companion object { const val MAX_LEVEL = 10 }
}
