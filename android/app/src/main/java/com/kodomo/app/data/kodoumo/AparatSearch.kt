package com.kodomo.app.data.kodoumo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class AparatVideo(val uid: String, val title: String, val poster: String?, val durationSeconds: Int, val channel: String) {
    val pageUrl: String get() = "https://www.aparat.com/v/$uid"
}

/** Aparat's public search API, used by parents to attach the full approved video to a kodoumo.ir-rated title. */
@Singleton
class AparatSearch @Inject constructor(private val http: OkHttpClient, private val json: Json) {

    suspend fun search(query: String): List<AparatVideo> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val request = Request.Builder().url("https://www.aparat.com/etc/api/videoBySearch/text/$q/perpage/20").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Aparat HTTP ${response.code}")
            val root = json.parseToJsonElement(response.body?.string().orEmpty()) as? JsonObject ?: return@use emptyList()
            val list = (root["videobysearch"] as? JsonArray) ?: return@use emptyList()
            list.mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val uid = s("uid").ifBlank { return@mapNotNull null }
                AparatVideo(uid, s("title"), s("small_poster").ifBlank { s("big_poster") }.ifBlank { null }, s("duration").toIntOrNull() ?: 0, s("sender_name"))
            }
        }
    }
}
