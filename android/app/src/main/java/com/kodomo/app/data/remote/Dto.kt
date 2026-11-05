package com.kodomo.app.data.remote

import kotlinx.serialization.Serializable

@Serializable data class OtpRequestDto(val phone: String)

@Serializable data class OtpRequestResponseDto(val requestId: String, val expiresInSeconds: Int)

@Serializable data class OtpVerifyDto(val requestId: String, val phone: String, val code: String)

@Serializable data class AuthTokenDto(val accessToken: String, val refreshToken: String, val familyId: String)

@Serializable
data class ChildProfileDto(
    val localId: Long,
    val name: String,
    val birthDate: String,
    val gender: String,
    val avatar: String,
    val interests: List<String>,
    val talents: String,
    val abilities: String,
    val stars: Int,
    val dailyLimitMinutes: Int,
    val bedtimeEnabled: Boolean,
    val bedtimeStartMinutes: Int,
    val bedtimeEndMinutes: Int,
    val maxViolence: Int,
    val maxFear: Int,
    val maxAbnormality: Int,
)

@Serializable
data class FamilySyncDto(
    val familyName: String,
    val fatherName: String,
    val motherName: String,
    val children: List<ChildProfileDto>,
)

@Serializable data class FamilySyncResponseDto(val familyId: String, val syncedAt: String)

@Serializable
data class RatingDto(
    val minAge: Int,
    val maxAge: Int,
    val violence: Int,
    val fear: Int,
    val educational: Int,
    val abnormality: Int,
    val creativity: Int = 3,
    val familyValues: Int = 3,
)

@Serializable data class ContentPageDto(val index: Int, val text: String, val emoji: String = "")

@Serializable
data class ContentDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val body: String = "",
    val category: String,
    val emoji: String = "✨",
    val colorStart: Long = 0xFFB9E4CBL,
    val colorEnd: Long = 0xFF7CC9A0L,
    val mediaUrl: String? = null,
    val durationMinutes: Int = 0,
    val rating: RatingDto,
    val positives: List<String> = emptyList(),
    val negatives: List<String> = emptyList(),
    val expertReview: String = "",
    val expertName: String = "",
    val peerLikes: Int = 0,
    val peerDislikes: Int = 0,
    val interests: List<String> = emptyList(),
    val author: String = "",
    val pages: List<ContentPageDto> = emptyList(),
)

@Serializable data class CatalogResponseDto(val items: List<ContentDto>, val updatedAt: String)

@Serializable
data class ReviewRequestDto(val title: String, val kind: String, val url: String, val note: String, val childAge: Int, val deviceId: String)

@Serializable
data class ReviewRequestResponseDto(val id: String = "", val requests: Int = 1)

@Serializable
data class FeedbackDto(val contentId: String, val liked: Boolean, val comment: String, val childAge: Int)

@Serializable
data class ForumPostDto(
    val id: String,
    val body: String,
    val authorName: String,
    val authorRole: String,
    val createdAt: String,
)

@Serializable
data class ForumTopicDto(
    val id: String,
    val title: String,
    val body: String,
    val authorName: String,
    val authorRole: String,
    val category: String,
    val createdAt: String,
    val posts: List<ForumPostDto> = emptyList(),
)

@Serializable data class CreateTopicDto(val title: String, val body: String, val category: String)

@Serializable data class CreatePostDto(val body: String)

@Serializable data class ReportDto(val reason: String)

@Serializable
data class UsageEventDto(
    val type: String,
    val childLocalId: Long,
    val childName: String,
    val category: String,
    val title: String,
    val usedSecondsToday: Long,
    val limitSeconds: Long,
    val timestamp: String,
)
