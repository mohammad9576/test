package com.kodomo.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class Family(
    val familyName: String,
    val fatherName: String,
    val motherName: String,
    val phone: String?,
    val remoteId: String?,
)

data class Child(
    val id: Long,
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val avatar: AvatarKind,
    val interests: Set<Interest>,
    val talents: String,
    val abilities: String,
    val stars: Int,
    val darkMode: Boolean,
) {
    fun ageYears(today: LocalDate = LocalDate.now()): Int = AgeGroup.ageInYears(birthDate, today)
    fun ageGroup(today: LocalDate = LocalDate.now()): AgeGroup = AgeGroup.fromAge(ageYears(today))
}

data class ChildDraft(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val gender: Gender = Gender.BOY,
    val avatar: AvatarKind = AvatarKind.BOY_SHORT,
    val interests: Set<Interest> = emptySet(),
    val talents: String = "",
    val abilities: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank() && birthDate != null
}

data class ParentalSettings(
    val childId: Long,
    val dailyLimitMinutes: Int,
    val bedtimeEnabled: Boolean,
    val bedtimeStartMinutes: Int,
    val bedtimeEndMinutes: Int,
    val maxViolence: Int,
    val maxFear: Int,
    val maxAbnormality: Int,
) {
    companion object {
        const val MIN_LIMIT = 5
        const val MAX_LIMIT = 120
        fun defaults(childId: Long, dailyLimit: Int = 60) = ParentalSettings(
            childId = childId,
            dailyLimitMinutes = dailyLimit,
            bedtimeEnabled = true,
            bedtimeStartMinutes = 21 * 60,
            bedtimeEndMinutes = 7 * 60,
            maxViolence = 2,
            maxFear = 2,
            maxAbnormality = 1,
        )
    }
}

/** The five-axis Kodomo pedagogical rating plus two positive indicators used by the radar chart. */
data class KodomoRating(
    val minAge: Int,
    val maxAge: Int,
    val violence: Int,
    val fear: Int,
    val educational: Int,
    val abnormality: Int,
    val creativity: Int,
    val familyValues: Int,
)

data class ContentItem(
    val id: String,
    val type: ContentType,
    val title: String,
    val description: String,
    val body: String,
    val category: String,
    val emoji: String,
    val colorStart: Long,
    val colorEnd: Long,
    val mediaUrl: String?,
    val durationMinutes: Int,
    val rating: KodomoRating,
    val positives: List<String>,
    val negatives: List<String>,
    val expertReview: String,
    val expertName: String,
    val peerLikes: Int,
    val peerDislikes: Int,
    val interests: Set<Interest>,
    val author: String,
)

data class ContentPage(val contentId: String, val index: Int, val text: String, val emoji: String)

data class Feedback(val childId: Long, val contentId: String, val liked: Boolean, val comment: String, val createdAt: Instant)

data class PeerOpinion(val likes: Int, val dislikes: Int, val recentComments: List<String>) {
    val likePercent: Int get() = if (likes + dislikes == 0) 0 else (likes * 100) / (likes + dislikes)
}

data class ActivityLog(
    val id: Long,
    val childId: Long,
    val category: ActivityCategory,
    val title: String,
    val contentId: String?,
    val startedAt: Instant,
    val durationSeconds: Long,
)

data class CategoryUsage(val category: ActivityCategory, val seconds: Long)

data class DailyUsage(val date: LocalDate, val byCategory: List<CategoryUsage>) {
    val totalSeconds: Long get() = byCategory.sumOf { it.seconds }
}

data class ClassSession(val id: Long, val childId: Long, val day: DayOfWeek, val period: Int, val subject: String, val startTime: String)

data class SchoolTask(
    val id: Long,
    val childId: Long,
    val kind: SchoolTaskKind,
    val subject: String,
    val title: String,
    val dueDate: LocalDate,
    val done: Boolean,
)

data class Exam(val id: Long, val childId: Long, val subject: String, val date: LocalDate, val note: String)

data class CraftProject(
    val id: Long,
    val childId: Long,
    val title: String,
    val emoji: String,
    val currentStep: Int,
    val materials: String,
    val cuttingNotes: String,
    val assemblyNotes: String,
    val paintingNotes: String,
    val memoryId: Long?,
    val completedAt: Instant?,
) {
    val isCompleted: Boolean get() = completedAt != null

    companion object {
        const val STEP_COUNT = 5
        val stepTitles = listOf("وسایل", "برش", "سرهم‌بندی", "رنگ‌آمیزی", "عکاسی")
        val stepEmojis = listOf("🧰", "✂️", "🧩", "🎨", "📸")
    }
}

data class Achievement(val id: String, val title: String, val emoji: String, val description: String, val starReward: Int)

data class EarnedAchievement(val achievement: Achievement, val earnedAt: Instant?)

data class FamilyReward(val id: String, val title: String, val emoji: String, val description: String, val starCost: Int)

data class RewardRedemption(
    val id: Long,
    val childId: Long,
    val childName: String,
    val reward: FamilyReward,
    val redeemedAt: Instant,
    val fulfilled: Boolean,
)

data class EncyclopediaEntry(val id: Int, val title: String, val summary: String, val body: String, val topic: String, val emoji: String, val minAge: Int)

data class ForumTopic(
    val id: Long,
    val title: String,
    val body: String,
    val authorName: String,
    val authorRole: AuthorRole,
    val category: String,
    val createdAt: Instant,
    val replyCount: Int,
    val hasExpertAnswer: Boolean,
)

data class ForumPost(
    val id: Long,
    val topicId: Long,
    val body: String,
    val authorName: String,
    val authorRole: AuthorRole,
    val createdAt: Instant,
    val reported: Boolean,
)

data class MemoryPhoto(
    val id: Long,
    val childId: Long,
    val encryptedPath: String,
    val caption: String,
    val source: MemorySource,
    val createdAt: Instant,
)

data class OfflineDownload(val contentId: String, val localPath: String, val sizeBytes: Long)
