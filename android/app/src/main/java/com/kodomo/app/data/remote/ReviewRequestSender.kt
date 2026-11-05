package com.kodomo.app.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends «please evaluate this title» requests to the Kodomo team (admin panel → درخواست ارزیابی). When the server is
 * unreachable the request is kept in a small on-device queue and retried at the next app start.
 */
@Singleton
class ReviewRequestSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiProvider: ApiProvider,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val queueFile get() = File(context.filesDir, "pending_review_requests.jsonl")

    @SuppressLint("HardwareIds")
    private fun deviceId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    /** Returns true when the request reached the server now (false = queued for later). */
    suspend fun request(title: String, kind: String, url: String, note: String, childAge: Int): Boolean {
        val dto = ReviewRequestDto(title.take(200), kind, url.take(1000), note.take(1000), childAge.coerceIn(0, 18), deviceId())
        val sent = runCatching { apiProvider.api().requestReview(dto) }.onFailure { Log.w(TAG, "queued review request", it) }.isSuccess
        if (!sent) enqueue(dto)
        return sent
    }

    suspend fun flush() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = queueFile
            if (!file.exists()) return@withContext
            val remaining = file.readLines().filter { it.isNotBlank() }.filterNot { line ->
                val dto = runCatching { json.decodeFromString<ReviewRequestDto>(line) }.getOrNull() ?: return@filterNot true
                runCatching { apiProvider.api().requestReview(dto) }.isSuccess
            }
            if (remaining.isEmpty()) file.delete() else file.writeText(remaining.joinToString("\n", postfix = "\n"))
        }
    }

    private suspend fun enqueue(dto: ReviewRequestDto) = mutex.withLock {
        withContext(Dispatchers.IO) { queueFile.appendText(json.encodeToString(dto) + "\n") }
    }

    private companion object { const val TAG = "ReviewRequests" }
}
