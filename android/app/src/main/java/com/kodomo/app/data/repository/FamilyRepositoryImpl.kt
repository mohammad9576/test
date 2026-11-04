package com.kodomo.app.data.repository

import com.kodomo.app.core.security.PinHasher
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ChildEntity
import com.kodomo.app.data.local.entity.FamilyEntity
import com.kodomo.app.data.local.toDomain
import com.kodomo.app.data.local.toEntity
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.model.Family
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.repository.FamilyRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepositoryImpl @Inject constructor(private val db: KodomoDatabase) : FamilyRepository {
    private val familyDao = db.familyDao()
    private val childDao = db.childDao()

    override fun observeFamily(): Flow<Family?> = familyDao.observe().map { it?.toDomain() }

    override fun observeChildren(): Flow<List<Child>> = childDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeChild(childId: Long): Flow<Child?> = childDao.observe(childId).map { it?.toDomain() }

    override suspend fun getChild(childId: Long): Child? = childDao.get(childId)?.toDomain()

    override suspend fun createFamily(familyName: String, fatherName: String, motherName: String, pin: String) {
        val hashed = PinHasher.hash(pin)
        familyDao.upsert(
            FamilyEntity(
                familyName = familyName.trim(),
                fatherName = fatherName.trim(),
                motherName = motherName.trim(),
                pinHash = hashed.hash,
                pinSalt = hashed.salt,
                createdAt = Instant.now(),
            ),
        )
    }

    override suspend fun updateFamily(familyName: String, fatherName: String, motherName: String) {
        val current = familyDao.get() ?: return
        familyDao.upsert(current.copy(familyName = familyName.trim(), fatherName = fatherName.trim(), motherName = motherName.trim()))
    }

    override suspend fun addChild(draft: ChildDraft, dailyLimitMinutes: Int): Long {
        val birthDate = requireNotNull(draft.birthDate) { "birth date is required" }
        return db.withTransaction {
            val id = childDao.insert(
                ChildEntity(
                    name = draft.name.trim(),
                    birthDate = birthDate,
                    gender = draft.gender,
                    avatar = draft.avatar,
                    interests = draft.interests,
                    talents = draft.talents.trim(),
                    abilities = draft.abilities.trim(),
                    createdAt = Instant.now(),
                ),
            )
            db.settingsDao().upsert(
                ParentalSettings.defaults(id, dailyLimitMinutes.coerceIn(ParentalSettings.MIN_LIMIT, ParentalSettings.MAX_LIMIT)).toEntity(),
            )
            id
        }
    }

    override suspend fun updateChild(child: Child) {
        val existing = childDao.get(child.id) ?: return
        childDao.update(
            existing.copy(
                name = child.name.trim(),
                birthDate = child.birthDate,
                gender = child.gender,
                avatar = child.avatar,
                interests = child.interests,
                talents = child.talents.trim(),
                abilities = child.abilities.trim(),
                darkMode = child.darkMode,
            ),
        )
    }

    override suspend fun deleteChild(childId: Long) = childDao.delete(childId)

    override suspend fun verifyPin(pin: String): Boolean {
        val family = familyDao.get() ?: return false
        return PinHasher.verify(pin, PinHasher.HashedPin(family.pinHash, family.pinSalt))
    }

    override suspend fun changePin(oldPin: String, newPin: String): Boolean {
        val family = familyDao.get() ?: return false
        if (!PinHasher.verify(oldPin, PinHasher.HashedPin(family.pinHash, family.pinSalt))) return false
        val hashed = PinHasher.hash(newPin)
        familyDao.upsert(family.copy(pinHash = hashed.hash, pinSalt = hashed.salt))
        return true
    }

    override suspend fun addStars(childId: Long, stars: Int) = childDao.addStars(childId, stars)

    override suspend fun spendStars(childId: Long, stars: Int): Boolean = childDao.spendStars(childId, stars) > 0

    override suspend fun setRemoteAccount(phone: String, remoteId: String) {
        val family = familyDao.get() ?: return
        familyDao.upsert(family.copy(phone = phone, remoteId = remoteId))
    }
}
