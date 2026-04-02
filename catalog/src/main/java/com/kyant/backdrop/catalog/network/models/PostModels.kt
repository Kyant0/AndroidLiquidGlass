package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ==================== Post Types ====================

enum class PostType {
    TEXT, IMAGE, VIDEO, LINK, POLL, ARTICLE, CELEBRATION, DOCUMENT, MIXED
}

enum class PostVisibility {
    PUBLIC, CONNECTIONS, PRIVATE
}

enum class ReactionType(val icon: String, val label: String, val colorHex: String) {
    LIKE("👍", "Like", "#3b82f6"),
    CELEBRATE("🎉", "Celebrate", "#22c55e"),
    SUPPORT("❤️", "Support", "#a855f7"),
    INSIGHTFUL("💡", "Insightful", "#f59e0b"),
    CURIOUS("❓", "Curious", "#ec4899")
}

enum class CelebrationType(val emoji: String, val label: String) {
    NEW_JOB("🎉", "Started a new position"),
    PROMOTION("🚀", "Got promoted"),
    GRADUATION("🎓", "Graduated"),
    CERTIFICATION("📜", "Earned a certification"),
    WORK_ANNIVERSARY("🎊", "Celebrating work anniversary"),
    BIRTHDAY("🎂", "Birthday")
}

// ==================== Poll Models ====================

@Serializable
data class PollOption(
    val id: String,
    val text: String,
    val votes: Int = 0,
    val hasVoted: Boolean = false,
    val percentage: Double = 0.0
)

enum class PollDuration(val hours: Int, val label: String) {
    ONE_HOUR(1, "1 hour"),
    SIX_HOURS(6, "6 hours"),
    TWELVE_HOURS(12, "12 hours"),
    ONE_DAY(24, "1 day"),
    THREE_DAYS(72, "3 days"),
    ONE_WEEK(168, "1 week")
}

// ==================== Reaction Summary ====================

@Serializable
data class ReactionSummary(
    val type: String,
    val count: Int
)

// ==================== Full Post Model ====================

@Serializable
data class FullPost(
    val id: String,
    val kind: String = "POST",
    val type: String = "TEXT",
    val authorId: String,
    val author: Author,
    val content: String? = null,
    val contentType: String = "text/plain",
    val mentions: List<String> = emptyList(),
    
    // Media
    val mediaUrls: List<String> = emptyList(),
    val mediaCount: Int = 0,
    val videoUrl: String? = null,
    val videoThumbnail: String? = null,
    val videoDuration: Int? = null,
    val videoSize: Long? = null,
    val videoFormat: String? = null,
    
    // Document
    val documentUrl: String? = null,
    val documentName: String? = null,
    val documentType: String? = null,
    val documentSize: Long? = null,
    val documentPages: Int? = null,
    val documentThumbnail: String? = null,
    
    // Link
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkImage: String? = null,
    val linkDomain: String? = null,
    
    // Article
    val articleTitle: String? = null,
    val articleCoverImage: String? = null,
    val articleReadTime: Int? = null,
    val articleTags: List<String> = emptyList(),
    
    // Poll
    val pollDuration: Int? = null,
    val pollEndsAt: String? = null,
    val pollOptions: List<PollOption> = emptyList(),
    val userVotedOptionId: String? = null,
    val showResultsBeforeVote: Boolean = false,
    
    // Celebration
    val celebrationType: String? = null,
    val celebrationMeta: JsonElement? = null,
    val celebrationBadge: String? = null,
    val celebrationGifUrl: String? = null,
    
    // Visibility and engagement
    val visibility: String = "PUBLIC",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val savesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val userReactionType: String? = null,
    val reactionSummary: List<ReactionSummary> = emptyList(),
    
    // Timestamps
    val createdAt: String,
    val updatedAt: String? = null
)

// ==================== Feed Response ====================

@Serializable
data class FullFeedResponse(
    val posts: List<FullPost>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

// ==================== Create Post Request ====================

@Serializable
data class CreatePostRequest(
    val type: String = "TEXT",
    val visibility: String = "PUBLIC",
    val content: String? = null,
    val mentions: List<String> = emptyList(),
    val linkUrl: String? = null,
    val pollOptions: List<String>? = null,
    val pollDuration: Int? = null,
    val showResultsBeforeVote: Boolean = false,
    val articleTitle: String? = null,
    val articleTags: List<String>? = null,
    val celebrationType: String? = null
)

// ==================== Update Post Request ====================

@Serializable
data class UpdatePostRequest(
    val content: String? = null,
    val visibility: String? = null
)

// ==================== Delete Post Response ====================

@Serializable
data class DeletePostResponse(
    val success: Boolean
)

// ==================== Like/Reaction Response ====================

@Serializable
data class ReactionResponse(
    val liked: Boolean,
    val likesCount: Int,
    val reactionType: String? = null,
    val reactionSummary: List<ReactionSummary> = emptyList()
)

// ==================== Save Response ====================

@Serializable
data class SaveResponse(
    val saved: Boolean,
    val savesCount: Int,
    val message: String? = null
)

// ==================== Poll Vote Response ====================

@Serializable
data class PollVoteResponse(
    val success: Boolean,
    val pollOptions: List<PollOption> = emptyList(),
    val userVotedOptionId: String? = null
)

// ==================== Likes List Response ====================

@Serializable
data class LikeUser(
    val id: String,
    val userId: String,
    val username: String? = null,
    val name: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val reactionType: String = "LIKE",
    val createdAt: String? = null
)

@Serializable
data class LikesListResponse(
    val likes: List<LikeUser>
)

// ==================== Report Models ====================

@Serializable
data class ReportReason(
    val id: String,
    val label: String,
    val description: String
)

@Serializable
data class ReportReasonsResponse(
    val reasons: List<ReportReason>
)

@Serializable
data class ReportPostRequest(
    val reason: String,
    val description: String? = null
)

@Serializable
data class ReportResponse(
    val success: Boolean,
    val message: String,
    val reportId: String
)

// ==================== Mention Search ====================

@Serializable
data class MentionUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val profileImage: String? = null,
    val headline: String? = null
)

@Serializable
data class MentionSearchResponse(
    val users: List<MentionUser>
)

// ==================== Full Comment Model ====================

@Serializable
data class FullComment(
    val id: String,
    val postId: String,
    val parentId: String? = null,
    val authorId: String,
    val author: Author,
    val content: String,
    val contentType: String = "text/plain",
    val mentions: List<String> = emptyList(),
    val likesCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<FullComment> = emptyList(),
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class FullCommentsResponse(
    val comments: List<FullComment>,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class CreateCommentFullRequest(
    val content: String,
    val parentId: String? = null,
    val mentions: List<String>? = null
)

// ==================== Color Presets ====================

data class ColorPreset(
    val name: String,
    val hex: String,
    val color: Long // Compose Color value
) {
    companion object {
        val presets = listOf(
            ColorPreset("Red", "#ef4444", 0xFFef4444),
            ColorPreset("Orange", "#f97316", 0xFFf97316),
            ColorPreset("Yellow", "#eab308", 0xFFeab308),
            ColorPreset("Green", "#22c55e", 0xFF22c55e),
            ColorPreset("Teal", "#14b8a6", 0xFF14b8a6),
            ColorPreset("Blue", "#3b82f6", 0xFF3b82f6),
            ColorPreset("Purple", "#a855f7", 0xFFa855f7),
            ColorPreset("Pink", "#ec4899", 0xFFec4899)
        )
    }
}

// ==================== Image Filter Presets ====================

data class ImageFilterPreset(
    val name: String,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
) {
    companion object {
        val presets = listOf(
            ImageFilterPreset("Original"),
            ImageFilterPreset("Vivid", saturation = 1.3f, contrast = 1.1f),
            ImageFilterPreset("Warm", brightness = 0.05f),
            ImageFilterPreset("Cool", brightness = -0.05f),
            ImageFilterPreset("B&W", saturation = 0f),
            ImageFilterPreset("Vintage", saturation = 0.8f, contrast = 1.2f, brightness = 0.05f),
            ImageFilterPreset("Drama", contrast = 1.4f, saturation = 0.9f),
            ImageFilterPreset("Fade", contrast = 0.9f, saturation = 0.8f, brightness = 0.1f)
        )
    }
}

// ==================== Connection Models ====================

@Serializable
data class Connection(
    val id: String,
    val userId: String,
    val name: String? = null,
    val username: String? = null,
    val headline: String? = null,
    val profileImage: String? = null,
    val connectedAt: String? = null
)

@Serializable
data class ConnectionsResponse(
    val connections: List<Connection>,
    val hasMore: Boolean = false,
    val total: Int = 0
)
