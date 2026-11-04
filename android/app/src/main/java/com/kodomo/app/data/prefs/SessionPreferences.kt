package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_session")

data class SessionState(
    val onboardingCompleted: Boolean,
    val kioskEnabled: Boolean,
    val activeChildId: Long?,
    val authToken: String?,
    val apiBaseUrl: String?,
    val lastSyncMillis: Long,
    val allowedApps: Set<String>,
    val lastKodoumoSyncMillis: Long,
)

@Singleton
class SessionPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KIOSK = booleanPreferencesKey("kiosk_enabled")
        val ACTIVE_CHILD = longPreferencesKey("active_child_id")
        val TOKEN = stringPreferencesKey("auth_token")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val LAST_SYNC = longPreferencesKey("last_sync")
        val ALLOWED_APPS = stringSetPreferencesKey("allowed_apps")
        val KODOUMO_SYNC = longPreferencesKey("kodoumo_sync")
    }

    val state: Flow<SessionState> = context.sessionStore.data.map { p ->
        SessionState(
            onboardingCompleted = p[Keys.ONBOARDING] ?: false,
            kioskEnabled = p[Keys.KIOSK] ?: false,
            activeChildId = p[Keys.ACTIVE_CHILD]?.takeIf { it > 0 },
            authToken = p[Keys.TOKEN],
            apiBaseUrl = p[Keys.API_BASE_URL],
            lastSyncMillis = p[Keys.LAST_SYNC] ?: 0L,
            allowedApps = p[Keys.ALLOWED_APPS] ?: emptySet(),
            lastKodoumoSyncMillis = p[Keys.KODOUMO_SYNC] ?: 0L,
        )
    }

    suspend fun current(): SessionState = state.first()

    suspend fun setOnboardingCompleted(value: Boolean) = context.sessionStore.edit { it[Keys.ONBOARDING] = value }

    suspend fun setKioskEnabled(value: Boolean) = context.sessionStore.edit { it[Keys.KIOSK] = value }

    suspend fun setActiveChild(childId: Long?) = context.sessionStore.edit {
        if (childId == null) it.remove(Keys.ACTIVE_CHILD) else it[Keys.ACTIVE_CHILD] = childId
    }

    suspend fun setAuthToken(token: String?) = context.sessionStore.edit {
        if (token == null) it.remove(Keys.TOKEN) else it[Keys.TOKEN] = token
    }

    suspend fun setApiBaseUrl(url: String?) = context.sessionStore.edit {
        if (url.isNullOrBlank()) it.remove(Keys.API_BASE_URL) else it[Keys.API_BASE_URL] = url.trim()
    }

    suspend fun setAppAllowed(packageName: String, allowed: Boolean) = context.sessionStore.edit {
        val current = it[Keys.ALLOWED_APPS] ?: emptySet()
        it[Keys.ALLOWED_APPS] = if (allowed) current + packageName else current - packageName
    }

    suspend fun setLastKodoumoSync(millis: Long) = context.sessionStore.edit { it[Keys.KODOUMO_SYNC] = millis }

    suspend fun setLastSync(millis: Long) = context.sessionStore.edit { it[Keys.LAST_SYNC] = millis }
}
