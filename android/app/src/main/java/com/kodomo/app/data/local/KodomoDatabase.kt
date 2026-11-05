package com.kodomo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kodomo.app.data.local.dao.ActivityDao
import com.kodomo.app.data.local.dao.ChildDao
import com.kodomo.app.data.local.dao.ContentDao
import com.kodomo.app.data.local.dao.DownloadDao
import com.kodomo.app.data.local.dao.EncyclopediaDao
import com.kodomo.app.data.local.dao.FamilyDao
import com.kodomo.app.data.local.dao.FeedbackDao
import com.kodomo.app.data.local.dao.ForumDao
import com.kodomo.app.data.local.dao.MemoryDao
import com.kodomo.app.data.local.dao.ProjectDao
import com.kodomo.app.data.local.dao.RewardDao
import com.kodomo.app.data.local.dao.SchoolDao
import com.kodomo.app.data.local.dao.SettingsDao
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

@Database(
    entities = [
        FamilyEntity::class,
        ChildEntity::class,
        ParentalSettingsEntity::class,
        ContentEntity::class,
        ContentPageEntity::class,
        FeedbackEntity::class,
        OfflineDownloadEntity::class,
        ActivityLogEntity::class,
        ClassSessionEntity::class,
        SchoolTaskEntity::class,
        ExamEntity::class,
        CraftProjectEntity::class,
        EarnedAchievementEntity::class,
        RewardRedemptionEntity::class,
        EncyclopediaEntity::class,
        ForumTopicEntity::class,
        ForumPostEntity::class,
        MemoryPhotoEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KodomoDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao
    abstract fun childDao(): ChildDao
    abstract fun settingsDao(): SettingsDao
    abstract fun contentDao(): ContentDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun downloadDao(): DownloadDao
    abstract fun activityDao(): ActivityDao
    abstract fun schoolDao(): SchoolDao
    abstract fun projectDao(): ProjectDao
    abstract fun rewardDao(): RewardDao
    abstract fun encyclopediaDao(): EncyclopediaDao
    abstract fun forumDao(): ForumDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val NAME = "kodomo.db"

        /** v2: poster, trailer and source columns for catalog items imported from kodoumo.ir. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE content ADD COLUMN trailerUrl TEXT")
                db.execSQL("ALTER TABLE content ADD COLUMN sourceUrl TEXT")
            }
        }

        /** v4: page illustrations for real picture books. */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content_pages ADD COLUMN image TEXT")
            }
        }

        /** v3: series episodes and «approved by parent only» flag. */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content ADD COLUMN episodes TEXT")
                db.execSQL("ALTER TABLE content ADD COLUMN parentOnly INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
