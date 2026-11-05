package com.kodomo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.AuthorRole
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Gender
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.model.SchoolTaskKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "family")
data class FamilyEntity(
    @PrimaryKey val id: Int = SINGLE_FAMILY_ID,
    val familyName: String,
    val fatherName: String,
    val motherName: String,
    val pinHash: String,
    val pinSalt: String,
    val phone: String? = null,
    val remoteId: String? = null,
    val createdAt: Instant,
) {
    companion object { const val SINGLE_FAMILY_ID = 1 }
}

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val avatar: AvatarKind,
    val interests: Set<Interest>,
    val talents: String,
    val abilities: String,
    val stars: Int = 0,
    val darkMode: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "parental_settings",
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class ParentalSettingsEntity(
    @PrimaryKey val childId: Long,
    val dailyLimitMinutes: Int,
    val bedtimeEnabled: Boolean,
    val bedtimeStartMinutes: Int,
    val bedtimeEndMinutes: Int,
    val maxViolence: Int,
    val maxFear: Int,
    val maxAbnormality: Int,
)

@Entity(tableName = "content", indices = [Index("type"), Index("category")])
data class ContentEntity(
    @PrimaryKey val id: String,
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
    val minAge: Int,
    val maxAge: Int,
    val violence: Int,
    val fear: Int,
    val educational: Int,
    val abnormality: Int,
    val creativity: Int,
    val familyValues: Int,
    val positives: List<String>,
    val negatives: List<String>,
    val expertReview: String,
    val expertName: String,
    val peerLikes: Int,
    val peerDislikes: Int,
    val interests: Set<Interest>,
    val author: String,
    val imageUrl: String? = null,
    val trailerUrl: String? = null,
    val sourceUrl: String? = null,
    /** Episode pages of a series, one «url\ttitle» per line (Telewebion series approved by a parent). */
    val episodes: String? = null,
    /** True when a parent approved the title without an expert (kodoumo) evaluation; an evaluation was requested. */
    val parentOnly: Boolean = false,
)

@Entity(
    tableName = "content_pages",
    primaryKeys = ["contentId", "pageIndex"],
    foreignKeys = [ForeignKey(entity = ContentEntity::class, parentColumns = ["id"], childColumns = ["contentId"], onDelete = ForeignKey.CASCADE)],
)
data class ContentPageEntity(
    val contentId: String,
    val pageIndex: Int,
    val text: String,
    val emoji: String,
    /** Illustration of the page (e.g. `file:///android_asset/catalog/img/0001/01.jpg`). */
    val image: String? = null,
)

@Entity(
    tableName = "feedback",
    indices = [Index(value = ["childId", "contentId"], unique = true), Index("contentId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val contentId: String,
    val liked: Boolean,
    val comment: String,
    val createdAt: Instant,
)

@Entity(tableName = "offline_downloads")
data class OfflineDownloadEntity(
    @PrimaryKey val contentId: String,
    val localPath: String,
    val sizeBytes: Long,
    val downloadedAt: Instant,
)

@Entity(
    tableName = "activity_log",
    indices = [Index(value = ["childId", "startedAt"])],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val category: ActivityCategory,
    val title: String,
    val contentId: String?,
    val startedAt: Instant,
    val durationSeconds: Long,
)

@Entity(
    tableName = "class_schedule",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val day: DayOfWeek,
    val period: Int,
    val subject: String,
    val startTime: String,
)

@Entity(
    tableName = "school_tasks",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class SchoolTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val kind: SchoolTaskKind,
    val subject: String,
    val title: String,
    val dueDate: LocalDate,
    val done: Boolean,
    val completedAt: Instant?,
)

@Entity(
    tableName = "exams",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val subject: String,
    val date: LocalDate,
    val note: String,
)

@Entity(
    tableName = "craft_projects",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class CraftProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val emoji: String,
    val currentStep: Int,
    val materials: String,
    val cuttingNotes: String,
    val assemblyNotes: String,
    val paintingNotes: String,
    val memoryId: Long?,
    val createdAt: Instant,
    val completedAt: Instant?,
)

@Entity(
    tableName = "earned_achievements",
    primaryKeys = ["childId", "achievementId"],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class EarnedAchievementEntity(
    val childId: Long,
    val achievementId: String,
    val earnedAt: Instant,
)

@Entity(
    tableName = "reward_redemptions",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class RewardRedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val rewardId: String,
    val redeemedAt: Instant,
    val fulfilled: Boolean,
)

@Fts4(notIndexed = ["emoji", "minAge"])
@Entity(tableName = "encyclopedia")
data class EncyclopediaEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val title: String,
    val summary: String,
    val body: String,
    val topic: String,
    val emoji: String,
    val minAge: Int,
)

@Entity(tableName = "forum_topics")
data class ForumTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String?,
    val title: String,
    val body: String,
    val authorName: String,
    val authorRole: AuthorRole,
    val category: String,
    val createdAt: Instant,
)

@Entity(
    tableName = "forum_posts",
    indices = [Index("topicId")],
    foreignKeys = [ForeignKey(entity = ForumTopicEntity::class, parentColumns = ["id"], childColumns = ["topicId"], onDelete = ForeignKey.CASCADE)],
)
data class ForumPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val remoteId: String?,
    val body: String,
    val authorName: String,
    val authorRole: AuthorRole,
    val createdAt: Instant,
    val reported: Boolean,
)

@Entity(
    tableName = "memories",
    indices = [Index("childId")],
    foreignKeys = [ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE)],
)
data class MemoryPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val encryptedPath: String,
    val caption: String,
    val source: MemorySource,
    val createdAt: Instant,
)
