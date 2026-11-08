package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.approvalsStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_approvals")

/**
 * Which children may see each title the parents approved (a cartoon can be right for the 9-year-old but not for the
 * 4-year-old). A title without an entry (approved before this existed) is shown to every child of a suitable age.
 */
@Singleton
class ApprovalsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(contentId: String) = stringSetPreferencesKey("kids_$contentId")

    /** contentId → allowed child ids. */
    val all: Flow<Map<String, Set<Long>>> = context.approvalsStore.data.map { p ->
        p.asMap().mapNotNull { (k, v) ->
            if (!k.name.startsWith("kids_")) null
            else k.name.removePrefix("kids_") to (v as? Set<*>).orEmpty().mapNotNull { (it as? String)?.toLongOrNull() }.toSet()
        }.toMap()
    }

    suspend fun childrenFor(contentId: String): Set<Long>? = all.first()[contentId]

    suspend fun set(contentId: String, children: Set<Long>) = context.approvalsStore.edit { it[key(contentId)] = children.map { c -> c.toString() }.toSet() }

    suspend fun remove(contentId: String) = context.approvalsStore.edit { it.remove(key(contentId)) }
}
