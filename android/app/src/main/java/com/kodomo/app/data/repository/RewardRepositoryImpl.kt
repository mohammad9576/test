package com.kodomo.app.data.repository

import androidx.room.withTransaction
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.EarnedAchievementEntity
import com.kodomo.app.data.local.entity.RewardRedemptionEntity
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.engine.FamilyRewardCatalog
import com.kodomo.app.domain.model.EarnedAchievement
import com.kodomo.app.domain.model.RewardRedemption
import com.kodomo.app.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepositoryImpl @Inject constructor(private val db: KodomoDatabase) : RewardRepository {
    private val dao = db.rewardDao()
    private val childDao = db.childDao()

    override fun observeAchievements(childId: Long): Flow<List<EarnedAchievement>> = dao.observeEarned(childId).map { earned ->
        val byId = earned.associateBy { it.achievementId }
        AchievementCatalog.all.map { EarnedAchievement(it, byId[it.id]?.earnedAt) }
    }

    override suspend fun award(childId: Long, achievementId: String): Boolean {
        val achievement = AchievementCatalog.byId(achievementId) ?: return false
        return db.withTransaction {
            val rowId = dao.insertEarned(EarnedAchievementEntity(childId, achievementId, Instant.now()))
            if (rowId != -1L) {
                childDao.addStars(childId, achievement.starReward)
                true
            } else {
                false
            }
        }
    }

    override suspend fun redeem(childId: Long, rewardId: String): Boolean {
        val reward = FamilyRewardCatalog.byId(rewardId) ?: return false
        return db.withTransaction {
            if (childDao.spendStars(childId, reward.starCost) == 0) return@withTransaction false
            dao.insertRedemption(RewardRedemptionEntity(childId = childId, rewardId = rewardId, redeemedAt = Instant.now(), fulfilled = false))
            true
        }
    }

    override fun observeRedemptions(): Flow<List<RewardRedemption>> = combine(dao.observeRedemptions(), childDao.observeAll()) { list, children ->
        val names = children.associate { it.id to it.name }
        list.mapNotNull { e ->
            FamilyRewardCatalog.byId(e.rewardId)?.let { RewardRedemption(e.id, e.childId, names[e.childId].orEmpty(), it, e.redeemedAt, e.fulfilled) }
        }
    }

    override fun observeChildRedemptions(childId: Long): Flow<List<RewardRedemption>> = dao.observeChildRedemptions(childId).map { list ->
        list.mapNotNull { e -> FamilyRewardCatalog.byId(e.rewardId)?.let { RewardRedemption(e.id, e.childId, "", it, e.redeemedAt, e.fulfilled) } }
    }

    override suspend fun markFulfilled(redemptionId: Long) = dao.markFulfilled(redemptionId)
}
