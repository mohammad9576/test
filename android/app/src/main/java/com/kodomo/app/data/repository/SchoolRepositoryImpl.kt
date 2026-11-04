package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ClassSessionEntity
import com.kodomo.app.data.local.entity.ExamEntity
import com.kodomo.app.data.local.entity.SchoolTaskEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.ClassSession
import com.kodomo.app.domain.model.Exam
import com.kodomo.app.domain.model.SchoolTask
import com.kodomo.app.domain.model.SchoolTaskKind
import com.kodomo.app.domain.repository.SchoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepositoryImpl @Inject constructor(db: KodomoDatabase) : SchoolRepository {
    private val dao = db.schoolDao()

    override fun observeSchedule(childId: Long): Flow<List<ClassSession>> = dao.observeSchedule(childId).map { l -> l.map { it.toDomain() } }

    override fun observeTasks(childId: Long): Flow<List<SchoolTask>> = dao.observeTasks(childId).map { l -> l.map { it.toDomain() } }

    override fun observeExams(childId: Long): Flow<List<Exam>> = dao.observeExams(childId).map { l -> l.map { it.toDomain() } }

    override suspend fun addClass(childId: Long, day: DayOfWeek, period: Int, subject: String, startTime: String) =
        dao.insertClass(ClassSessionEntity(childId = childId, day = day, period = period, subject = subject.trim(), startTime = startTime.trim()))

    override suspend fun deleteClass(id: Long) = dao.deleteClass(id)

    override suspend fun addTask(childId: Long, kind: SchoolTaskKind, subject: String, title: String, dueDate: LocalDate) =
        dao.insertTask(
            SchoolTaskEntity(childId = childId, kind = kind, subject = subject.trim(), title = title.trim(), dueDate = dueDate, done = false, completedAt = null),
        )

    override suspend fun setTaskDone(id: Long, done: Boolean) {
        val task = dao.getTask(id) ?: return
        val onTime = done && !LocalDate.now().isAfter(task.dueDate)
        dao.setDone(id, done, if (onTime) Instant.now() else null)
    }

    override suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    override suspend fun addExam(childId: Long, subject: String, date: LocalDate, note: String) =
        dao.insertExam(ExamEntity(childId = childId, subject = subject.trim(), date = date, note = note.trim()))

    override suspend fun deleteExam(id: Long) = dao.deleteExam(id)

    override suspend fun countDoneTasks(childId: Long): Int = dao.countDone(childId)
}
