package com.kodomo.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** REST contract implemented by the Kodomo backend in `/backend` (NestJS). */
interface KodomoApi {
    @POST("auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequestDto): OtpRequestResponseDto

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyDto): AuthTokenDto

    @PUT("families/me")
    suspend fun syncFamily(@Body body: FamilySyncDto): FamilySyncResponseDto

    @GET("content/catalog")
    suspend fun catalog(@Query("since") since: String?): CatalogResponseDto

    @POST("content/feedback")
    suspend fun sendFeedback(@Body body: FeedbackDto)

    @GET("forum/topics")
    suspend fun forumTopics(): List<ForumTopicDto>

    @POST("forum/topics")
    suspend fun createTopic(@Body body: CreateTopicDto): ForumTopicDto

    @POST("forum/topics/{id}/posts")
    suspend fun createPost(@Path("id") topicId: String, @Body body: CreatePostDto): ForumPostDto

    @POST("forum/posts/{id}/report")
    suspend fun reportPost(@Path("id") postId: String, @Body body: ReportDto)
}
