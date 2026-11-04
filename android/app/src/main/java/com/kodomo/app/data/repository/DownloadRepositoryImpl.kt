package com.kodomo.app.data.repository

import android.content.Context
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.OfflineDownloadEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.OfflineDownload
import com.kodomo.app.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Offline download of approved videos into app-private storage, emitting progress in [0, 1]. */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    db: KodomoDatabase,
    private val client: OkHttpClient,
) : DownloadRepository {
    private val dao = db.downloadDao()
    private val dir: File get() = File(context.filesDir, "offline").apply { mkdirs() }

    override fun observeDownloads(): Flow<List<OfflineDownload>> = dao.observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun localFileFor(contentId: String): String? =
        dao.get(contentId)?.localPath?.takeIf { File(it).exists() }

    override fun download(contentId: String, url: String): Flow<Float> = flow {
        val target = File(dir, "$contentId.media")
        val partial = File(dir, "$contentId.part")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            val total = body.contentLength()
            var read = 0L
            var lastEmitted = -1
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = ((read * 100) / total).toInt()
                            if (percent != lastEmitted) {
                                lastEmitted = percent
                                emit(percent / 100f)
                            }
                        }
                    }
                }
            }
        }
        if (!partial.renameTo(target)) throw IOException("Unable to finalize download")
        dao.upsert(OfflineDownloadEntity(contentId, target.absolutePath, target.length(), Instant.now()))
        emit(1f)
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(contentId: String) {
        dao.get(contentId)?.let { File(it.localPath).delete() }
        dao.delete(contentId)
    }
}
