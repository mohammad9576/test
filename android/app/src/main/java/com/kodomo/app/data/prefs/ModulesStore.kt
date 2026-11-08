package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.modulesStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_modules")

/** Which kids-home sections each child sees (chosen by the parents). Stored as the set of visible keys. */
@Singleton
class ModulesStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(childId: Long) = stringSetPreferencesKey("visible_$childId")

    /** null = the parents have not chosen yet (use the defaults). */
    fun visible(childId: Long): Flow<Set<String>?> = context.modulesStore.data.map { it[key(childId)] }

    suspend fun setVisible(childId: Long, keys: Set<String>) = context.modulesStore.edit { it[key(childId)] = keys }
}
