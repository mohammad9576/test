package com.kodomo.app.data.repository

import android.content.Context
import com.kodomo.app.core.security.SecureFileCipher
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.MemoryPhotoEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.MemoryPhoto
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.repository.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Private memories gallery. Photos live encrypted in no-backup internal storage, visible only through the parent hub. */
@Singleton
class MemoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    db: KodomoDatabase,
    private val cipher: SecureFileCipher,
) : MemoryRepository {
    private val dao = db.memoryDao()
    private val dir: File get() = File(context.noBackupFilesDir, "memories").apply { mkdirs() }

    override fun observeMemories(childId: Long?): Flow<List<MemoryPhoto>> = dao.observe(childId).map { l -> l.map { it.toDomain() } }

    override suspend fun saveJpeg(childId: Long, jpeg: ByteArray, caption: String, source: MemorySource): Long = withContext(Dispatchers.IO) {
        val file = File(dir, "${UUID.randomUUID()}.kmem")
        cipher.encryptToFile(jpeg, file)
        dao.insert(MemoryPhotoEntity(childId = childId, encryptedPath = file.absolutePath, caption = caption, source = source, createdAt = Instant.now()))
    }

    override suspend fun decrypt(memory: MemoryPhoto): ByteArray = withContext(Dispatchers.IO) { cipher.decryptFile(File(memory.encryptedPath)) }

    override suspend fun delete(memory: MemoryPhoto) = withContext(Dispatchers.IO) {
        File(memory.encryptedPath).delete()
        dao.delete(memory.id)
    }
}
