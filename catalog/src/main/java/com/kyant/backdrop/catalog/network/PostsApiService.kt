package com.kyant.backdrop.catalog.network

import android.content.Context
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Posts API Service - Handles all post-related API calls
 */
object PostsApiService {
    
    private val BASE_URL = BuildConfig.API_BASE_URL
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }
    
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.BODY
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300000 // 5 minutes for large video uploads
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 180000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    
    // Helper to safely parse error response
    private suspend fun parseErrorMessage(response: HttpResponse): String {
        return try {
            val error: ApiError = response.body()
            error.getErrorMessage()
        } catch (e: Exception) {
            try {
                response.bodyAsText()
            } catch (_: Exception) {
                "Request failed with status ${response.status.value}"
            }
        }
    }
    
    // ==================== Feed APIs ====================
    
    /**
     * Get feed with cursor-based pagination
     */
    suspend fun getFeed(
        context: Context,
        cursor: String? = null,
        limit: Int = 20
    ): Result<FullFeedResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/feed") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a single post by ID
     */
    suspend fun getPost(context: Context, postId: String): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/$postId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Create Post APIs ====================
    
    /**
     * Create a text post
     */
    suspend fun createTextPost(
        context: Context,
        content: String,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return createPost(context, "TEXT", content, visibility, mentions)
    }
    
    /**
     * Create an image post with one or more images
     */
    suspend fun createImagePost(
        context: Context,
        content: String? = null,
        visibility: String = "PUBLIC",
        images: List<Pair<ByteArray, String>>, // (bytes, filename)
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "IMAGE")
                    append("visibility", visibility)
                    content?.let { append("content", it) }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                    images.forEachIndexed { index, (bytes, filename) ->
                        append(
                            "media",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=${filename.ifEmpty { "image$index.jpg" }}")
                            }
                        )
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a video post
     */
    suspend fun createVideoPost(
        context: Context,
        content: String? = null,
        visibility: String = "PUBLIC",
        videoBytes: ByteArray,
        videoFilename: String = "video.mp4",
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "VIDEO")
                    append("visibility", visibility)
                    content?.let { append("content", it) }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                    append(
                        "video",
                        videoBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "video/mp4")
                            append(HttpHeaders.ContentDisposition, "filename=$videoFilename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a link post
     */
    suspend fun createLinkPost(
        context: Context,
        linkUrl: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "LINK")
                    append("visibility", visibility)
                    append("linkUrl", linkUrl)
                    content?.let { append("content", it) }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a poll post
     */
    suspend fun createPollPost(
        context: Context,
        pollOptions: List<String>,
        pollDurationHours: Int = 24,
        content: String? = null,
        visibility: String = "PUBLIC",
        showResultsBeforeVote: Boolean = false,
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "POLL")
                    append("visibility", visibility)
                    append("pollDuration", pollDurationHours.toString())
                    append("showResultsBeforeVote", showResultsBeforeVote.toString())
                    content?.let { append("content", it) }
                    pollOptions.forEach { option ->
                        append("pollOptions", option)
                    }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create an article post
     */
    suspend fun createArticlePost(
        context: Context,
        articleTitle: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        coverImage: Pair<ByteArray, String>? = null,
        articleTags: List<String> = emptyList(),
        mentions: List<String> = emptyList()
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "ARTICLE")
                    append("visibility", visibility)
                    append("articleTitle", articleTitle)
                    content?.let { append("content", it) }
                    articleTags.forEach { tag ->
                        append("articleTags", tag)
                    }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                    coverImage?.let { (bytes, filename) ->
                        append(
                            "articleCoverImage",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=$filename")
                            }
                        )
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a celebration post
     */
    suspend fun createCelebrationPost(
        context: Context,
        celebrationType: String,
        content: String? = null,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        celebrationGif: Pair<ByteArray, String>? = null
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", "CELEBRATION")
                    append("visibility", visibility)
                    append("celebrationType", celebrationType)
                    content?.let { append("content", it) }
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                    celebrationGif?.let { (bytes, filename) ->
                        val mime = when {
                            filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
                            filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                            filename.endsWith(".png", ignoreCase = true) -> "image/png"
                            else -> "image/jpeg"
                        }
                        append(
                            "image",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mime)
                                append(HttpHeaders.ContentDisposition, "filename=$filename")
                            }
                        )
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generic create post for simple text posts
     */
    private suspend fun createPost(
        context: Context,
        type: String,
        content: String,
        visibility: String,
        mentions: List<String>
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", type)
                    append("visibility", visibility)
                    append("content", content)
                    if (mentions.isNotEmpty()) {
                        append("mentions", json.encodeToString(ListSerializer(String.serializer()), mentions))
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Update/Delete Post APIs ====================
    
    /**
     * Update a post (only content and visibility are editable)
     */
    suspend fun updatePost(
        context: Context,
        postId: String,
        content: String? = null,
        visibility: String? = null
    ): Result<FullPost> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/posts/$postId") {
                header("Authorization", "Bearer $token")
                setBody(UpdatePostRequest(content, visibility))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a post
     */
    suspend fun deletePost(context: Context, postId: String): Result<DeletePostResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/posts/$postId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Engagement APIs ====================
    
    /**
     * Toggle like on a post
     */
    suspend fun toggleLike(context: Context, postId: String): Result<ReactionResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of users who liked a post
     */
    suspend fun getLikes(context: Context, postId: String): Result<LikesListResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/$postId/likes") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get likes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Vote on a poll
     */
    suspend fun votePoll(context: Context, postId: String, optionId: String): Result<PollVoteResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/poll/vote") {
                header("Authorization", "Bearer $token")
                setBody(mapOf("optionId" to optionId))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to vote on poll"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Share a post
     */
    suspend fun sharePost(context: Context, postId: String, userId: String? = null): Result<ShareResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/share") {
                header("Authorization", "Bearer $token")
                userId?.let { setBody(mapOf("userId" to it)) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to share post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Save APIs ====================
    
    /**
     * Toggle save on a post
     */
    suspend fun toggleSave(context: Context, postId: String): Result<SaveResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/saved/$postId/toggle") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle save"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get saved posts
     */
    suspend fun getSavedPosts(
        context: Context,
        cursor: String? = null,
        limit: Int = 20
    ): Result<FullFeedResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/saved") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Comments APIs ====================
    
    /**
     * Get comments for a post
     */
    suspend fun getComments(
        context: Context,
        postId: String,
        parentId: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<FullCommentsResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
                parentId?.let { parameter("parentId", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a comment
     */
    suspend fun createComment(
        context: Context,
        postId: String,
        content: String,
        parentId: String? = null,
        mentions: List<String>? = null
    ): Result<FullComment> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                setBody(CreateCommentFullRequest(content, parentId, mentions))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle like on a comment
     */
    suspend fun toggleCommentLike(
        context: Context,
        postId: String,
        commentId: String
    ): Result<CommentLikeResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments/$commentId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle comment like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a comment
     */
    suspend fun deleteComment(
        context: Context,
        postId: String,
        commentId: String
    ): Result<DeletePostResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/posts/$postId/comments/$commentId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to delete comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Mention APIs ====================
    
    /**
     * Search users for @mention autocomplete
     */
    suspend fun searchMentions(context: Context, query: String, limit: Int = 10): Result<MentionSearchResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/mentions/search") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to search mentions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Report APIs ====================
    
    /**
     * Get report reasons
     */
    suspend fun getReportReasons(): Result<ReportReasonsResponse> {
        return try {
            val response = client.get("$BASE_URL/reports/reasons")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get report reasons"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Report a post
     */
    suspend fun reportPost(
        context: Context,
        postId: String,
        reason: String,
        description: String? = null
    ): Result<ReportResponse> {
        return try {
            val token = ApiClient.getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reports/post/$postId") {
                header("Authorization", "Bearer $token")
                setBody(ReportPostRequest(reason, description))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to report post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
