package com.kodomo.app.data.repository

import android.util.Log
import com.kodomo.app.core.util.PersianDigits
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.local.entity.ContentEntity
import com.kodomo.app.data.local.entity.ContentPageEntity
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.data.remote.ApiProvider
import com.kodomo.app.data.remote.ChildProfileDto
import com.kodomo.app.data.remote.ContentDto
import com.kodomo.app.data.remote.FamilySyncDto
import com.kodomo.app.data.remote.FeedbackDto
import com.kodomo.app.data.remote.OtpRequestDto
import com.kodomo.app.data.remote.OtpVerifyDto
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.ParentalSettings
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Cloud account (OTP), profile backup and catalog pull against the Kodomo backend. */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val db: KodomoDatabase,
    private val apiProvider: ApiProvider,
    private val session: SessionPreferences,
) {
    sealed interface Result<out T> {
        data class Success<T>(val value: T) : Result<T>
        data class Failure(val message: String) : Result<Nothing>
    }

    suspend fun requestOtp(phone: String): Result<String> = call {
        apiProvider.api().requestOtp(OtpRequestDto(normalizePhone(phone))).requestId
    }

    suspend fun verifyOtp(requestId: String, phone: String, code: String): Result<Unit> = call {
        val normalized = normalizePhone(phone)
        val token = apiProvider.api().verifyOtp(OtpVerifyDto(requestId, normalized, PersianDigits.digitsOnly(code)))
        session.setAuthToken(token.accessToken)
        val family = db.familyDao().get()
        if (family != null) db.familyDao().upsert(family.copy(phone = normalized, remoteId = token.familyId))
    }

    suspend fun logout() {
        session.setAuthToken(null)
    }

    suspend fun syncFamily(): Result<Unit> = call {
        val family = db.familyDao().get() ?: error("خانواده‌ای ثبت نشده است")
        val children = db.childDao().getAll().map { child ->
            val s = db.settingsDao().get(child.id) ?: ParentalSettings.defaults(child.id).let {
                com.kodomo.app.data.local.entity.ParentalSettingsEntity(
                    it.childId, it.dailyLimitMinutes, it.bedtimeEnabled, it.bedtimeStartMinutes, it.bedtimeEndMinutes,
                    it.maxViolence, it.maxFear, it.maxAbnormality,
                )
            }
            ChildProfileDto(
                localId = child.id, name = child.name, birthDate = child.birthDate.toString(), gender = child.gender.name,
                avatar = child.avatar.name, interests = child.interests.map { it.name }, talents = child.talents,
                abilities = child.abilities, stars = child.stars, dailyLimitMinutes = s.dailyLimitMinutes,
                bedtimeEnabled = s.bedtimeEnabled, bedtimeStartMinutes = s.bedtimeStartMinutes, bedtimeEndMinutes = s.bedtimeEndMinutes,
                maxViolence = s.maxViolence, maxFear = s.maxFear, maxAbnormality = s.maxAbnormality,
            )
        }
        apiProvider.api().syncFamily(FamilySyncDto(family.familyName, family.fatherName, family.motherName, children))
        session.setLastSync(System.currentTimeMillis())
    }

    suspend fun pullCatalog(): Result<Int> = call {
        val since = session.current().lastSyncMillis.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() }
        val response = apiProvider.api().catalog(since)
        val items = response.items.mapNotNull { it.toEntityOrNull() }
        val pages = response.items.flatMap { dto -> dto.pages.map { ContentPageEntity(dto.id, it.index, it.text, it.emoji) } }
        db.contentDao().upsertCatalog(items, pages)
        session.setLastSync(System.currentTimeMillis())
        items.size
    }

    suspend fun pushFeedback(contentId: String, liked: Boolean, comment: String, childAge: Int) {
        if (session.current().authToken == null) return
        call { apiProvider.api().sendFeedback(FeedbackDto(contentId, liked, comment, childAge)) }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: retrofit2.HttpException) {
        Log.w(TAG, "HTTP ${e.code()}", e)
        Result.Failure(if (e.code() == 401) "نشست منقضی شده است؛ دوباره وارد شوید." else "خطای سرور (${e.code()})")
    } catch (e: java.io.IOException) {
        Log.w(TAG, "Network failure", e)
        Result.Failure("اتصال به سرور کدومو برقرار نشد. اینترنت را بررسی کنید.")
    } catch (e: IllegalStateException) {
        Result.Failure(e.message ?: "خطای نامشخص")
    } catch (e: kotlinx.serialization.SerializationException) {
        Result.Failure("پاسخ سرور قابل خواندن نبود.")
    }

    private fun normalizePhone(phone: String): String {
        val digits = PersianDigits.digitsOnly(phone)
        return when {
            digits.startsWith("98") -> "+$digits"
            digits.startsWith("0") -> "+98" + digits.drop(1)
            else -> "+98$digits"
        }
    }

    private fun ContentDto.toEntityOrNull(): ContentEntity? {
        val contentType = ContentType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: return null
        return ContentEntity(
            id = id, type = contentType, title = title, description = description, body = body, category = category,
            emoji = emoji, colorStart = colorStart, colorEnd = colorEnd, mediaUrl = mediaUrl, durationMinutes = durationMinutes,
            minAge = rating.minAge, maxAge = rating.maxAge, violence = rating.violence, fear = rating.fear,
            educational = rating.educational, abnormality = rating.abnormality, creativity = rating.creativity,
            familyValues = rating.familyValues, positives = positives, negatives = negatives, expertReview = expertReview,
            expertName = expertName, peerLikes = peerLikes, peerDislikes = peerDislikes,
            interests = interests.mapNotNull { name -> Interest.entries.firstOrNull { it.name == name } }.toSet(),
            author = author,
        )
    }

    private companion object { const val TAG = "CloudSync" }
}
