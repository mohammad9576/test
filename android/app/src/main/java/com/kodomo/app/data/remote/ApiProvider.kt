package com.kodomo.app.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kodomo.app.BuildConfig
import com.kodomo.app.data.prefs.SessionPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the Retrofit API lazily against the base URL the parent configured (or the BuildConfig default),
 * attaching the bearer token stored after OTP login.
 */
@Singleton
class ApiProvider @Inject constructor(
    private val baseClient: OkHttpClient,
    private val json: Json,
    private val session: SessionPreferences,
) {
    @Volatile private var cached: Pair<String, KodomoApi>? = null

    val authInterceptor = Interceptor { chain ->
        val token = runBlocking { session.state.first().authToken }
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    suspend fun baseUrl(): String = normalize(session.current().apiBaseUrl ?: BuildConfig.API_BASE_URL)

    suspend fun api(): KodomoApi {
        val url = baseUrl()
        cached?.takeIf { it.first == url }?.let { return it.second }
        val client = baseClient.newBuilder().addInterceptor(authInterceptor).build()
        val api = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KodomoApi::class.java)
        cached = url to api
        return api
    }

    suspend fun webSocketUrl(): String {
        val base = baseUrl().removeSuffix("/").removeSuffix("/v1")
        return base.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/live"
    }

    private fun normalize(url: String): String = if (url.endsWith("/")) url else "$url/"
}
