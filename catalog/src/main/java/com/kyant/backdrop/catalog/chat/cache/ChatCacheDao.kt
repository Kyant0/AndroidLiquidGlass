package com.kyant.backdrop.catalog.chat.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ChatCacheDao {
    @Query(
        """
        SELECT * FROM cached_conversations
        WHERE cacheOwnerId = :cacheOwnerId
        ORDER BY lastMessageAtEpochMillis DESC, updatedAtEpochMillis DESC
        """
    )
    suspend fun getConversations(cacheOwnerId: String): List<CachedConversationEntity>

    @Query(
        """
        SELECT * FROM cached_conversations
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId
        LIMIT 1
        """
    )
    suspend fun getConversation(
        cacheOwnerId: String,
        conversationId: String
    ): CachedConversationEntity?

    @Upsert
    suspend fun upsertConversations(conversations: List<CachedConversationEntity>)

    @Query(
        """
        DELETE FROM cached_conversations
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId NOT IN (:conversationIds)
        """
    )
    suspend fun deleteConversationsNotIn(
        cacheOwnerId: String,
        conversationIds: List<String>
    )

    @Query("DELETE FROM cached_conversations WHERE cacheOwnerId = :cacheOwnerId")
    suspend fun deleteAllConversations(cacheOwnerId: String)

    @Query(
        """
        DELETE FROM cached_conversations
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId
        """
    )
    suspend fun deleteConversation(
        cacheOwnerId: String,
        conversationId: String
    )

    @Query(
        """
        SELECT * FROM cached_messages
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId
        ORDER BY createdAtEpochMillis ASC, createdAt ASC
        """
    )
    suspend fun getMessages(
        cacheOwnerId: String,
        conversationId: String
    ): List<CachedMessageEntity>

    @Upsert
    suspend fun upsertMessages(messages: List<CachedMessageEntity>)

    @Query(
        """
        DELETE FROM cached_messages
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId
        """
    )
    suspend fun deleteMessagesForConversation(
        cacheOwnerId: String,
        conversationId: String
    )

    @Query(
        """
        DELETE FROM cached_messages
        WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId AND messageId = :messageId
        """
    )
    suspend fun deleteMessage(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String
    )

    @Query(
        """
        DELETE FROM cached_messages
        WHERE cacheOwnerId = :cacheOwnerId
        AND conversationId = :conversationId
        AND messageId NOT IN (
            SELECT messageId FROM cached_messages
            WHERE cacheOwnerId = :cacheOwnerId AND conversationId = :conversationId
            ORDER BY createdAtEpochMillis DESC, createdAt DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun trimMessages(
        cacheOwnerId: String,
        conversationId: String,
        keepCount: Int
    )

    @Query("DELETE FROM cached_messages WHERE cacheOwnerId = :cacheOwnerId")
    suspend fun deleteAllMessages(cacheOwnerId: String)
}
