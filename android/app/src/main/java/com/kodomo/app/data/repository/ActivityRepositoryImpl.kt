package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ActivityLogEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ActivityLog
import com.kodomo.app.domain.model.CategoryUsage
import com.kodomo.app.domain.model.DailyUsage
import com.kodomo.app.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(db: KodomoDatabase) : ActivityRepository {
    private val dao = db.activityDao()
    private val zone: ZoneId get() = ZoneId.systemDefault()

    override suspend fun log(
        childId: Long,
        category: ActivityCategory,
        title: String,
        contentId: String?,
        startedAt: Instant,
        durationSeconds: Long,
    ) {
        if (durationSeconds <= 0) return
        dao.insert(ActivityLogEntity(childId = childId, category = category, title = title, contentId = contentId, startedAt = startedAt, durationSeconds = durationSeconds))
    }

    override suspend fun usedSecondsOn(childId: Long, date: LocalDate): Long {
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        return dao.sumBetween(childId, from, to)
    }

    override fun observeUsedSecondsToday(childId: Long): Flow<Long> =
        dao.observeSumSince(childId, LocalDate.now().atStartOfDay(zone).toInstant())

    override fun observeDailyUsage(childId: Long, days: Int): Flow<List<DailyUsage>> {
        val today = LocalDate.now()
        val firstDay = today.minusDays((days - 1).toLong())
        return dao.observeSince(childId, firstDay.atStartOfDay(zone).toInstant()).map { logs ->
            val byDay = logs.groupBy { it.startedAt.atZone(zone).toLocalDate() }
            (0 until days).map { offset ->
                val date = firstDay.plusDays(offset.toLong())
                val dayLogs = byDay[date].orEmpty()
                DailyUsage(
                    date = date,
                    byCategory = dayLogs.groupBy { it.category }
                        .map { (category, entries) -> CategoryUsage(category, entries.sumOf { it.durationSeconds }) }
                        .sortedByDescending { it.seconds },
                )
            }
        }
    }

    override fun observeTimeline(childId: Long, limit: Int): Flow<List<ActivityLog>> =
        dao.observeRecent(childId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun countCompleted(childId: Long, category: ActivityCategory, minSeconds: Long): Int =
        dao.countDistinct(childId, category, minSeconds)
}
