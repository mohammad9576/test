package com.kodomo.app.data.repository

import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.data.local.toEntity
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(db: KodomoDatabase) : SettingsRepository {
    private val dao = db.settingsDao()

    override fun observeSettings(childId: Long): Flow<ParentalSettings> =
        dao.observe(childId).map { it?.toDomain() ?: ParentalSettings.defaults(childId) }

    override fun observeAllSettings(): Flow<List<ParentalSettings>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getSettings(childId: Long): ParentalSettings = dao.get(childId)?.toDomain() ?: ParentalSettings.defaults(childId)

    override suspend fun saveSettings(settings: ParentalSettings) {
        dao.upsert(
            settings.copy(
                dailyLimitMinutes = settings.dailyLimitMinutes.coerceIn(5, ParentalSettings.MAX_LIMIT),
                maxViolence = settings.maxViolence.coerceIn(1, 5),
                maxFear = settings.maxFear.coerceIn(1, 5),
                maxAbnormality = settings.maxAbnormality.coerceIn(1, 5),
                bedtimeStartMinutes = settings.bedtimeStartMinutes.coerceIn(0, 24 * 60 - 1),
                bedtimeEndMinutes = settings.bedtimeEndMinutes.coerceIn(0, 24 * 60 - 1),
            ).toEntity(),
        )
    }
}
