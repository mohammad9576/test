package com.kodomo.app.data.local

import com.kodomo.app.data.local.dao.TopicWithStats
import com.kodomo.app.data.local.entity.ActivityLogEntity
import com.kodomo.app.data.local.entity.ChildEntity
import com.kodomo.app.data.local.entity.ClassSessionEntity
import com.kodomo.app.data.local.entity.ContentEntity
import com.kodomo.app.data.local.entity.ContentPageEntity
import com.kodomo.app.data.local.entity.CraftProjectEntity
import com.kodomo.app.data.local.entity.EncyclopediaEntity
import com.kodomo.app.data.local.entity.ExamEntity
import com.kodomo.app.data.local.entity.FamilyEntity
import com.kodomo.app.data.local.entity.ForumPostEntity
import com.kodomo.app.data.local.entity.MemoryPhotoEntity
import com.kodomo.app.data.local.entity.OfflineDownloadEntity
import com.kodomo.app.data.local.entity.ParentalSettingsEntity
import com.kodomo.app.data.local.entity.SchoolTaskEntity
import com.kodomo.app.domain.model.ActivityLog
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ClassSession
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.CraftProject
import com.kodomo.app.domain.model.EncyclopediaEntry
import com.kodomo.app.domain.model.Exam
import com.kodomo.app.domain.model.Family
import com.kodomo.app.domain.model.ForumPost
import com.kodomo.app.domain.model.ForumTopic
import com.kodomo.app.domain.model.KodomoRating
import com.kodomo.app.domain.model.MemoryPhoto
import com.kodomo.app.domain.model.OfflineDownload
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.model.SchoolTask

fun FamilyEntity.toDomain() = Family(familyName, fatherName, motherName, phone, remoteId)

fun ChildEntity.toDomain() = Child(id, name, birthDate, gender, avatar, interests, talents, abilities, stars, darkMode)

fun ParentalSettingsEntity.toDomain() = ParentalSettings(
    childId, dailyLimitMinutes, bedtimeEnabled, bedtimeStartMinutes, bedtimeEndMinutes, maxViolence, maxFear, maxAbnormality,
)

fun ParentalSettings.toEntity() = ParentalSettingsEntity(
    childId, dailyLimitMinutes, bedtimeEnabled, bedtimeStartMinutes, bedtimeEndMinutes, maxViolence, maxFear, maxAbnormality,
)

fun ContentEntity.toDomain() = ContentItem(
    id = id, type = type, title = title, description = description, body = body, category = category, emoji = emoji,
    colorStart = colorStart, colorEnd = colorEnd, mediaUrl = mediaUrl, durationMinutes = durationMinutes,
    rating = KodomoRating(minAge, maxAge, violence, fear, educational, abnormality, creativity, familyValues),
    positives = positives, negatives = negatives, expertReview = expertReview, expertName = expertName,
    peerLikes = peerLikes, peerDislikes = peerDislikes, interests = interests, author = author,
    imageUrl = imageUrl, trailerUrl = trailerUrl, sourceUrl = sourceUrl,
)

fun ContentItem.toEntity() = ContentEntity(
    id = id, type = type, title = title, description = description, body = body, category = category, emoji = emoji,
    colorStart = colorStart, colorEnd = colorEnd, mediaUrl = mediaUrl, durationMinutes = durationMinutes,
    minAge = rating.minAge, maxAge = rating.maxAge, violence = rating.violence, fear = rating.fear,
    educational = rating.educational, abnormality = rating.abnormality, creativity = rating.creativity,
    familyValues = rating.familyValues, positives = positives, negatives = negatives, expertReview = expertReview,
    expertName = expertName, peerLikes = peerLikes, peerDislikes = peerDislikes, interests = interests, author = author,
    imageUrl = imageUrl, trailerUrl = trailerUrl, sourceUrl = sourceUrl,
)

fun ContentPageEntity.toDomain() = ContentPage(contentId, pageIndex, text, emoji)
fun ContentPage.toEntity() = ContentPageEntity(contentId, index, text, emoji)

fun ActivityLogEntity.toDomain() = ActivityLog(id, childId, category, title, contentId, startedAt, durationSeconds)

fun ClassSessionEntity.toDomain() = ClassSession(id, childId, day, period, subject, startTime)

fun SchoolTaskEntity.toDomain() = SchoolTask(id, childId, kind, subject, title, dueDate, done)

fun ExamEntity.toDomain() = Exam(id, childId, subject, date, note)

fun CraftProjectEntity.toDomain() = CraftProject(
    id, childId, title, emoji, currentStep, materials, cuttingNotes, assemblyNotes, paintingNotes, memoryId, completedAt,
)

fun EncyclopediaEntity.toDomain() = EncyclopediaEntry(rowId, title, summary, body, topic, emoji, minAge)

fun TopicWithStats.toDomain() = ForumTopic(id, title, body, authorName, authorRole, category, createdAt, replyCount, expertReplies > 0)

fun ForumPostEntity.toDomain() = ForumPost(id, topicId, body, authorName, authorRole, createdAt, reported)

fun MemoryPhotoEntity.toDomain() = MemoryPhoto(id, childId, encryptedPath, caption, source, createdAt)

fun OfflineDownloadEntity.toDomain() = OfflineDownload(contentId, localPath, sizeBytes)
