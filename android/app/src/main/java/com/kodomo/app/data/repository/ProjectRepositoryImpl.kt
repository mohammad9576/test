package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.CraftProjectEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.CraftProject
import com.kodomo.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(db: KodomoDatabase) : ProjectRepository {
    private val dao = db.projectDao()

    override fun observeProjects(childId: Long): Flow<List<CraftProject>> = dao.observeForChild(childId).map { l -> l.map { it.toDomain() } }

    override fun observeProject(id: Long): Flow<CraftProject?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun create(childId: Long, title: String, emoji: String): Long = dao.insert(
        CraftProjectEntity(
            childId = childId, title = title.trim(), emoji = emoji, currentStep = 0, materials = "", cuttingNotes = "",
            assemblyNotes = "", paintingNotes = "", memoryId = null, createdAt = Instant.now(), completedAt = null,
        ),
    )

    override suspend fun update(project: CraftProject) {
        val existing = dao.get(project.id) ?: return
        dao.update(
            existing.copy(
                currentStep = project.currentStep.coerceIn(0, CraftProject.STEP_COUNT - 1),
                materials = project.materials,
                cuttingNotes = project.cuttingNotes,
                assemblyNotes = project.assemblyNotes,
                paintingNotes = project.paintingNotes,
            ),
        )
    }

    override suspend fun complete(projectId: Long, memoryId: Long) {
        val existing = dao.get(projectId) ?: return
        dao.update(existing.copy(currentStep = CraftProject.STEP_COUNT - 1, memoryId = memoryId, completedAt = Instant.now()))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
