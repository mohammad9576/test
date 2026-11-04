package com.kodomo.app.domain.repository

import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ActivityLog
import com.kodomo.app.domain.model.AuthorRole
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.model.ClassSession
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.CraftProject
import com.kodomo.app.domain.model.DailyUsage
import com.kodomo.app.domain.model.EarnedAchievement
import com.kodomo.app.domain.model.EncyclopediaEntry
import com.kodomo.app.domain.model.Exam
import com.kodomo.app.domain.model.Family
import com.kodomo.app.domain.model.ForumPost
import com.kodomo.app.domain.model.ForumTopic
import com.kodomo.app.domain.model.MemoryPhoto
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.model.OfflineDownload
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.model.PeerOpinion
import com.kodomo.app.domain.model.RewardRedemption
import com.kodomo.app.domain.model.SchoolTask
import com.kodomo.app.domain.model.SchoolTaskKind
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

interface FamilyRepository {
    fun observeFamily(): Flow<Family?>
    fun observeChildren(): Flow<List<Child>>
    fun observeChild(childId: Long): Flow<Child?>
    suspend fun getChild(childId: Long): Child?
    suspend fun createFamily(familyName: String, fatherName: String, motherName: String, pin: String)
    suspend fun updateFamily(familyName: String, fatherName: String, motherName: String)
    suspend fun addChild(draft: ChildDraft, dailyLimitMinutes: Int): Long
    suspend fun updateChild(child: Child)
    suspend fun deleteChild(childId: Long)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun changePin(oldPin: String, newPin: String): Boolean
    suspend fun addStars(childId: Long, stars: Int)
    suspend fun spendStars(childId: Long, stars: Int): Boolean
    suspend fun setRemoteAccount(phone: String, remoteId: String)
}

interface SettingsRepository {
    fun observeSettings(childId: Long): Flow<ParentalSettings>
    fun observeAllSettings(): Flow<List<ParentalSettings>>
    suspend fun getSettings(childId: Long): ParentalSettings
    suspend fun saveSettings(settings: ParentalSettings)
}

interface ContentRepository {
    fun observeByType(type: ContentType): Flow<List<ContentItem>>
    fun observeAllowedForChild(childId: Long, type: ContentType): Flow<List<ContentItem>>
    fun observeItem(id: String): Flow<ContentItem?>
    suspend fun getItem(id: String): ContentItem?
    suspend fun getPages(contentId: String): List<ContentPage>
    fun search(query: String, type: ContentType?): Flow<List<ContentItem>>
    suspend fun upsertCatalog(items: List<ContentItem>, pages: List<ContentPage>)
    suspend fun submitFeedback(childId: Long, contentId: String, liked: Boolean, comment: String)
    fun observePeerOpinion(contentId: String): Flow<PeerOpinion>
    fun observeChildFeedback(childId: Long, contentId: String): Flow<Boolean?>
    fun observeLikedIds(childId: Long): Flow<Set<String>>
    suspend fun toggleLike(childId: Long, contentId: String)
    suspend fun setMediaUrl(contentId: String, url: String?)
}

interface DownloadRepository {
    fun observeDownloads(): Flow<List<OfflineDownload>>
    suspend fun localFileFor(contentId: String): String?
    fun download(contentId: String, url: String): Flow<Float>
    suspend fun delete(contentId: String)
}

interface ActivityRepository {
    suspend fun log(childId: Long, category: ActivityCategory, title: String, contentId: String?, startedAt: Instant, durationSeconds: Long)
    suspend fun usedSecondsOn(childId: Long, date: LocalDate): Long
    fun observeUsedSecondsToday(childId: Long): Flow<Long>
    fun observeDailyUsage(childId: Long, days: Int): Flow<List<DailyUsage>>
    fun observeTimeline(childId: Long, limit: Int): Flow<List<ActivityLog>>
    suspend fun countCompleted(childId: Long, category: ActivityCategory, minSeconds: Long): Int
}

interface SchoolRepository {
    fun observeSchedule(childId: Long): Flow<List<ClassSession>>
    fun observeTasks(childId: Long): Flow<List<SchoolTask>>
    fun observeExams(childId: Long): Flow<List<Exam>>
    suspend fun addClass(childId: Long, day: DayOfWeek, period: Int, subject: String, startTime: String)
    suspend fun deleteClass(id: Long)
    suspend fun addTask(childId: Long, kind: SchoolTaskKind, subject: String, title: String, dueDate: LocalDate)
    suspend fun setTaskDone(id: Long, done: Boolean)
    suspend fun deleteTask(id: Long)
    suspend fun addExam(childId: Long, subject: String, date: LocalDate, note: String)
    suspend fun deleteExam(id: Long)
    suspend fun countDoneTasks(childId: Long): Int
}

interface ProjectRepository {
    fun observeProjects(childId: Long): Flow<List<CraftProject>>
    fun observeProject(id: Long): Flow<CraftProject?>
    suspend fun create(childId: Long, title: String, emoji: String): Long
    suspend fun update(project: CraftProject)
    suspend fun complete(projectId: Long, memoryId: Long)
    suspend fun delete(id: Long)
}

interface RewardRepository {
    fun observeAchievements(childId: Long): Flow<List<EarnedAchievement>>
    /** Returns true if this call newly awarded the achievement. */
    suspend fun award(childId: Long, achievementId: String): Boolean
    suspend fun redeem(childId: Long, rewardId: String): Boolean
    fun observeRedemptions(): Flow<List<RewardRedemption>>
    fun observeChildRedemptions(childId: Long): Flow<List<RewardRedemption>>
    suspend fun markFulfilled(redemptionId: Long)
}

interface EncyclopediaRepository {
    fun search(query: String, maxAge: Int): Flow<List<EncyclopediaEntry>>
    fun observeTopics(maxAge: Int): Flow<List<EncyclopediaEntry>>
    suspend fun get(id: Int): EncyclopediaEntry?
}

interface ForumRepository {
    fun observeTopics(): Flow<List<ForumTopic>>
    fun observeTopic(id: Long): Flow<ForumTopic?>
    fun observePosts(topicId: Long): Flow<List<ForumPost>>
    suspend fun createTopic(title: String, body: String, category: String, authorName: String): Long
    suspend fun reply(topicId: Long, body: String, authorName: String, role: AuthorRole)
    suspend fun report(postId: Long)
    suspend fun refreshFromServer()
}

interface MemoryRepository {
    fun observeMemories(childId: Long?): Flow<List<MemoryPhoto>>
    suspend fun saveJpeg(childId: Long, jpeg: ByteArray, caption: String, source: MemorySource): Long
    suspend fun decrypt(memory: MemoryPhoto): ByteArray
    suspend fun delete(memory: MemoryPhoto)
}
