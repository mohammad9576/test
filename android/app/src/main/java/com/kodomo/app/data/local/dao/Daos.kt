package com.kodomo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.kodomo.app.data.local.entity.ActivityLogEntity
import com.kodomo.app.data.local.entity.ChildEntity
import com.kodomo.app.data.local.entity.ClassSessionEntity
import com.kodomo.app.data.local.entity.ContentEntity
import com.kodomo.app.data.local.entity.ContentPageEntity
import com.kodomo.app.data.local.entity.CraftProjectEntity
import com.kodomo.app.data.local.entity.EarnedAchievementEntity
import com.kodomo.app.data.local.entity.EncyclopediaEntity
import com.kodomo.app.data.local.entity.ExamEntity
import com.kodomo.app.data.local.entity.FamilyEntity
import com.kodomo.app.data.local.entity.FeedbackEntity
import com.kodomo.app.data.local.entity.ForumPostEntity
import com.kodomo.app.data.local.entity.ForumTopicEntity
import com.kodomo.app.data.local.entity.MemoryPhotoEntity
import com.kodomo.app.data.local.entity.OfflineDownloadEntity
import com.kodomo.app.data.local.entity.ParentalSettingsEntity
import com.kodomo.app.data.local.entity.RewardRedemptionEntity
import com.kodomo.app.data.local.entity.SchoolTaskEntity
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ContentType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family WHERE id = 1")
    fun observe(): Flow<FamilyEntity?>

    @Query("SELECT * FROM family WHERE id = 1")
    suspend fun get(): FamilyEntity?

    @Upsert
    suspend fun upsert(family: FamilyEntity)
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY birthDate ASC")
    fun observeAll(): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children ORDER BY birthDate ASC")
    suspend fun getAll(): List<ChildEntity>

    @Query("SELECT * FROM children WHERE id = :id")
    fun observe(id: Long): Flow<ChildEntity?>

    @Query("SELECT * FROM children WHERE id = :id")
    suspend fun get(id: Long): ChildEntity?

    @Insert
    suspend fun insert(child: ChildEntity): Long

    @Update
    suspend fun update(child: ChildEntity)

    @Query("DELETE FROM children WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE children SET stars = stars + :delta WHERE id = :id")
    suspend fun addStars(id: Long, delta: Int)

    @Query("UPDATE children SET stars = stars - :cost WHERE id = :id AND stars >= :cost")
    suspend fun spendStars(id: Long, cost: Int): Int
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM parental_settings WHERE childId = :childId")
    fun observe(childId: Long): Flow<ParentalSettingsEntity?>

    @Query("SELECT * FROM parental_settings")
    fun observeAll(): Flow<List<ParentalSettingsEntity>>

    @Query("SELECT * FROM parental_settings WHERE childId = :childId")
    suspend fun get(childId: Long): ParentalSettingsEntity?

    @Upsert
    suspend fun upsert(settings: ParentalSettingsEntity)
}

@Dao
abstract class ContentDao {
    @Query("SELECT COUNT(*) FROM content")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM content WHERE type = :type ORDER BY title")
    abstract fun observeByType(type: ContentType): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE id = :id")
    abstract fun observe(id: String): Flow<ContentEntity?>

    @Query("SELECT * FROM content WHERE id = :id")
    abstract suspend fun get(id: String): ContentEntity?

    @Query(
        "SELECT * FROM content WHERE (:type IS NULL OR type = :type) AND " +
            "(title LIKE '%' || :term || '%' OR description LIKE '%' || :term || '%' OR category LIKE '%' || :term || '%') ORDER BY title",
    )
    abstract fun search(term: String, type: ContentType?): Flow<List<ContentEntity>>

    @Upsert
    abstract suspend fun upsertAll(items: List<ContentEntity>)

    @Upsert
    abstract suspend fun upsertPages(pages: List<ContentPageEntity>)

    @Query("UPDATE content SET mediaUrl = :url WHERE id = :id")
    abstract suspend fun setMediaUrl(id: String, url: String?)

    @Query("SELECT mediaUrl FROM content WHERE id = :id")
    abstract suspend fun mediaUrl(id: String): String?

    @Query("SELECT episodes FROM content WHERE id = :id")
    abstract suspend fun episodes(id: String): String?

    @Query("SELECT COUNT(*) FROM content WHERE sourceUrl LIKE 'https://kodoumo.ir%'")
    abstract fun observeImportedCount(): Flow<Int>

    @Query("SELECT * FROM content_pages WHERE contentId = :contentId ORDER BY pageIndex")
    abstract suspend fun pages(contentId: String): List<ContentPageEntity>

    @Transaction
    open suspend fun upsertCatalog(items: List<ContentEntity>, pages: List<ContentPageEntity>) {
        upsertAll(items)
        upsertPages(pages)
    }
}

data class FeedbackTally(val likes: Int, val dislikes: Int)

@Dao
interface FeedbackDao {
    @Upsert
    suspend fun upsert(feedback: FeedbackEntity)

    @Query("SELECT * FROM feedback WHERE childId = :childId AND contentId = :contentId")
    suspend fun get(childId: Long, contentId: String): FeedbackEntity?

    @Query("SELECT * FROM feedback WHERE childId = :childId AND contentId = :contentId")
    fun observe(childId: Long, contentId: String): Flow<FeedbackEntity?>

    @Query("DELETE FROM feedback WHERE childId = :childId AND contentId = :contentId")
    suspend fun delete(childId: Long, contentId: String)

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN liked THEN 1 ELSE 0 END), 0) AS likes, " +
            "COALESCE(SUM(CASE WHEN liked THEN 0 ELSE 1 END), 0) AS dislikes FROM feedback WHERE contentId = :contentId",
    )
    fun observeTally(contentId: String): Flow<FeedbackTally>

    @Query("SELECT comment FROM feedback WHERE contentId = :contentId AND comment != '' ORDER BY createdAt DESC LIMIT 5")
    fun observeComments(contentId: String): Flow<List<String>>

    @Query("SELECT contentId FROM feedback WHERE childId = :childId AND liked = 1")
    fun observeLikedIds(childId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM feedback WHERE childId = :childId")
    suspend fun countForChild(childId: Long): Int
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM offline_downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineDownloadEntity>>

    @Query("SELECT * FROM offline_downloads WHERE contentId = :contentId")
    suspend fun get(contentId: String): OfflineDownloadEntity?

    @Upsert
    suspend fun upsert(entity: OfflineDownloadEntity)

    @Query("DELETE FROM offline_downloads WHERE contentId = :contentId")
    suspend fun delete(contentId: String)
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(log: ActivityLogEntity)

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM activity_log WHERE childId = :childId AND startedAt >= :since AND startedAt < :until")
    suspend fun sumBetween(childId: Long, since: Instant, until: Instant): Long

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM activity_log WHERE childId = :childId AND startedAt >= :since")
    fun observeSumSince(childId: Long, since: Instant): Flow<Long>

    @Query("SELECT * FROM activity_log WHERE childId = :childId AND startedAt >= :since ORDER BY startedAt ASC")
    fun observeSince(childId: Long, since: Instant): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_log WHERE childId = :childId ORDER BY startedAt DESC LIMIT :maxRows")
    fun observeRecent(childId: Long, maxRows: Int): Flow<List<ActivityLogEntity>>

    @Query("SELECT COUNT(DISTINCT COALESCE(contentId, title)) FROM activity_log WHERE childId = :childId AND category = :category AND durationSeconds >= :minSeconds")
    suspend fun countDistinct(childId: Long, category: ActivityCategory, minSeconds: Long): Int
}

@Dao
interface SchoolDao {
    @Query("SELECT * FROM class_schedule WHERE childId = :childId ORDER BY period")
    fun observeSchedule(childId: Long): Flow<List<ClassSessionEntity>>

    @Insert suspend fun insertClass(entity: ClassSessionEntity)

    @Query("DELETE FROM class_schedule WHERE id = :id") suspend fun deleteClass(id: Long)

    @Query("SELECT * FROM school_tasks WHERE childId = :childId ORDER BY done ASC, dueDate ASC")
    fun observeTasks(childId: Long): Flow<List<SchoolTaskEntity>>

    @Insert suspend fun insertTask(entity: SchoolTaskEntity)

    @Query("UPDATE school_tasks SET done = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Instant?)

    @Query("SELECT * FROM school_tasks WHERE id = :id") suspend fun getTask(id: Long): SchoolTaskEntity?

    @Query("DELETE FROM school_tasks WHERE id = :id") suspend fun deleteTask(id: Long)

    @Query("SELECT COUNT(*) FROM school_tasks WHERE childId = :childId AND done = 1 AND completedAt IS NOT NULL")
    suspend fun countDone(childId: Long): Int

    @Query("SELECT * FROM exams WHERE childId = :childId ORDER BY date ASC")
    fun observeExams(childId: Long): Flow<List<ExamEntity>>

    @Insert suspend fun insertExam(entity: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id") suspend fun deleteExam(id: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM craft_projects WHERE childId = :childId ORDER BY completedAt IS NOT NULL, createdAt DESC")
    fun observeForChild(childId: Long): Flow<List<CraftProjectEntity>>

    @Query("SELECT * FROM craft_projects WHERE id = :id")
    fun observe(id: Long): Flow<CraftProjectEntity?>

    @Query("SELECT * FROM craft_projects WHERE id = :id")
    suspend fun get(id: Long): CraftProjectEntity?

    @Insert suspend fun insert(entity: CraftProjectEntity): Long

    @Update suspend fun update(entity: CraftProjectEntity)

    @Query("DELETE FROM craft_projects WHERE id = :id") suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM craft_projects WHERE childId = :childId AND completedAt IS NOT NULL")
    suspend fun countCompleted(childId: Long): Int
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM earned_achievements WHERE childId = :childId")
    fun observeEarned(childId: Long): Flow<List<EarnedAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEarned(entity: EarnedAchievementEntity): Long

    @Insert suspend fun insertRedemption(entity: RewardRedemptionEntity): Long

    @Query("SELECT * FROM reward_redemptions ORDER BY fulfilled ASC, redeemedAt DESC")
    fun observeRedemptions(): Flow<List<RewardRedemptionEntity>>

    @Query("SELECT * FROM reward_redemptions WHERE childId = :childId ORDER BY redeemedAt DESC")
    fun observeChildRedemptions(childId: Long): Flow<List<RewardRedemptionEntity>>

    @Query("UPDATE reward_redemptions SET fulfilled = 1 WHERE id = :id")
    suspend fun markFulfilled(id: Long)
}

@Dao
interface EncyclopediaDao {
    @Query("SELECT COUNT(*) FROM encyclopedia") suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EncyclopediaEntity>)

    @Query("SELECT rowid, * FROM encyclopedia WHERE encyclopedia MATCH :expression AND minAge <= :maxAge")
    fun match(expression: String, maxAge: Int): Flow<List<EncyclopediaEntity>>

    @Query("SELECT rowid, * FROM encyclopedia WHERE minAge <= :maxAge ORDER BY topic, title")
    fun observeAll(maxAge: Int): Flow<List<EncyclopediaEntity>>

    @Query("SELECT rowid, * FROM encyclopedia WHERE rowid = :id")
    suspend fun get(id: Int): EncyclopediaEntity?
}

data class TopicWithStats(
    val id: Long,
    val remoteId: String?,
    val title: String,
    val body: String,
    val authorName: String,
    val authorRole: com.kodomo.app.domain.model.AuthorRole,
    val category: String,
    val createdAt: Instant,
    val replyCount: Int,
    val expertReplies: Int,
)

@Dao
interface ForumDao {
    @Query(
        "SELECT t.*, (SELECT COUNT(*) FROM forum_posts p WHERE p.topicId = t.id) AS replyCount, " +
            "(SELECT COUNT(*) FROM forum_posts p WHERE p.topicId = t.id AND p.authorRole != 'PARENT') AS expertReplies " +
            "FROM forum_topics t ORDER BY t.createdAt DESC",
    )
    fun observeTopics(): Flow<List<TopicWithStats>>

    @Query(
        "SELECT t.*, (SELECT COUNT(*) FROM forum_posts p WHERE p.topicId = t.id) AS replyCount, " +
            "(SELECT COUNT(*) FROM forum_posts p WHERE p.topicId = t.id AND p.authorRole != 'PARENT') AS expertReplies " +
            "FROM forum_topics t WHERE t.id = :id",
    )
    fun observeTopic(id: Long): Flow<TopicWithStats?>

    @Query("SELECT * FROM forum_posts WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun observePosts(topicId: Long): Flow<List<ForumPostEntity>>

    @Insert suspend fun insertTopic(topic: ForumTopicEntity): Long

    @Insert suspend fun insertPost(post: ForumPostEntity): Long

    @Query("SELECT * FROM forum_topics WHERE remoteId = :remoteId") suspend fun topicByRemoteId(remoteId: String): ForumTopicEntity?

    @Query("SELECT * FROM forum_posts WHERE remoteId = :remoteId") suspend fun postByRemoteId(remoteId: String): ForumPostEntity?

    @Query("SELECT * FROM forum_topics WHERE id = :id") suspend fun topic(id: Long): ForumTopicEntity?

    @Query("SELECT * FROM forum_posts WHERE id = :id") suspend fun post(id: Long): ForumPostEntity?

    @Query("UPDATE forum_posts SET reported = 1 WHERE id = :id") suspend fun markReported(id: Long)

    @Query("SELECT COUNT(*) FROM forum_topics") suspend fun countTopics(): Int
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE (:childId IS NULL OR childId = :childId) ORDER BY createdAt DESC")
    fun observe(childId: Long?): Flow<List<MemoryPhotoEntity>>

    @Insert suspend fun insert(entity: MemoryPhotoEntity): Long

    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: Long)
}
