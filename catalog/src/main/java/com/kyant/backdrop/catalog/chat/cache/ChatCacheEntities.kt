package com.kyant.backdrop.catalog.chat.cache

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_conversations",
    primaryKeys = ["cacheOwnerId", "conversationId"],
    indices = [
        Index(value = ["cacheOwnerId"]),
        Index(value = ["cacheOwnerId", "lastMessageAtEpochMillis"]),
        Index(value = ["cacheOwnerId", "updatedAtEpochMillis"])
    ]
)
data class CachedConversationEntity(
    val cacheOwnerId: String,
    val conversationId: String,
    val participant1Id: String,
    val participant2Id: String,
    val participant1Json: String,
    val participant2Json: String,
    val otherParticipantJson: String,
    val lastMessageJson: String? = null,
    val lastMessageAt: String? = null,
    val lastMessageAtEpochMillis: Long = 0L,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val updatedAtEpochMillis: Long = 0L,
    val isMessageRequest: Boolean = false,
    val messagesCachedAt: Long = 0L,
    val cachedNextCursor: String? = null,
    val hasMoreMessages: Boolean = false
)

@Entity(
    tableName = "cached_messages",
    primaryKeys = ["cacheOwnerId", "conversationId", "messageId"],
    indices = [
        Index(value = ["cacheOwnerId", "conversationId"]),
        Index(value = ["cacheOwnerId", "conversationId", "createdAtEpochMillis"]),
        Index(value = ["cacheOwnerId", "conversationId", "updatedAtEpochMillis"])
    ]
)
data class CachedMessageEntity(
    val cacheOwnerId: String,
    val conversationId: String,
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val contentType: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val status: String = "SENT",
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val isDeleted: Boolean = false,
    val replyToId: String? = null,
    val replyToJson: String? = null,
    val senderJson: String? = null,
    val reactionsJson: String? = null,
    val createdAt: String,
    val createdAtEpochMillis: Long = 0L,
    val updatedAt: String,
    val updatedAtEpochMillis: Long = 0L
)

data class CachedMessagesSnapshot(
    val messages: List<com.kyant.backdrop.catalog.network.models.Message>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val cachedAt: Long
)
