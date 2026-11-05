package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kodomo.app.domain.model.Buddy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.buddyStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_buddy")

/** Which companion character each child chose. */
@Singleton
class BuddyStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(childId: Long) = stringPreferencesKey("buddy_$childId")

    fun observe(childId: Long): Flow<Buddy> = context.buddyStore.data.map { p ->
        p[key(childId)]?.let { name -> Buddy.entries.firstOrNull { it.name == name } } ?: Buddy.ROBOT
    }

    suspend fun set(childId: Long, buddy: Buddy) {
        context.buddyStore.edit { it[key(childId)] = buddy.name }
    }
}
