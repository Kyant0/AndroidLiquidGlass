package com.kyant.backdrop.catalog.chat.cache

import android.content.Context
import androidx.room.withTransaction
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.ChatUser
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.ConversationLastMessage
import com.kyant.backdrop.catalog.network.models.ConversationsResponse
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.MessageReaction
import com.kyant.backdrop.catalog.network.models.MessagesResponse
import com.kyant.backdrop.catalog.network.models.ReplyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val RECENT_MESSAGE_LIMIT = 50

class ChatCacheRepository(
    context: Context,
    private val database: ChatCacheDatabase = ChatCacheDatabase.getInstance(context)
) {
    private val appContext = context.applicationContext
    private val dao = database.chatCacheDao()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun getCachedConversations(cacheOwnerId: String): List<Conversation> = withContext(Dispatchers.IO) {
        dao.getConversations(cacheOwnerId).map(::entityToConversation)
    }

    suspend fun getCachedConversation(
        cacheOwnerId: String,
        conversationId: String
    ): Conversation? = withContext(Dispatchers.IO) {
        dao.getConversation(cacheOwnerId, conversationId)?.let(::entityToConversation)
    }

    suspend fun getCachedMessagesSnapshot(
        cacheOwnerId: String,
        conversationId: String
    ): CachedMessagesSnapshot? = withContext(Dispatchers.IO) {
        val conversation = dao.getConversation(cacheOwnerId, conversationId) ?: return@withContext null
        val messages = dao.getMessages(cacheOwnerId, conversationId).map(::entityToMessage)
        if (messages.isEmpty() && conversation.messagesCachedAt <= 0L) {
            return@withContext null
        }
        CachedMessagesSnapshot(
            messages = messages,
            nextCursor = conversation.cachedNextCursor,
            hasMore = conversation.hasMoreMessages,
            cachedAt = conversation.messagesCachedAt
        )
    }

    suspend fun upsertConversation(
        cacheOwnerId: String,
        conversation: Conversation
    ) {
        withContext(Dispatchers.IO) {
            val existing = dao.getConversation(cacheOwnerId, conversation.id)
            dao.upsertConversations(listOf(conversationToEntity(cacheOwnerId, conversation, existing)))
        }
    }

    suspend fun refreshConversations(
        cacheOwnerId: String,
        limit: Int = 30,
        cursor: String? = null
    ): Result<ConversationsResponse> = withContext(Dispatchers.IO) {
        ApiClient.getConversations(appContext, limit, cursor).onSuccess { response ->
            cacheConversations(
                cacheOwnerId = cacheOwnerId,
                conversations = response.conversations,
                replaceAll = cursor == null
            )
        }
    }

    suspend fun refreshMessages(
        cacheOwnerId: String,
        conversationId: String,
        limit: Int = RECENT_MESSAGE_LIMIT,
        cursor: String? = null
    ): Result<MessagesResponse> = withContext(Dispatchers.IO) {
        ApiClient.getMessages(appContext, conversationId, limit, cursor).onSuccess { response ->
            cacheMessagesResponse(cacheOwnerId, conversationId, response, cursor)
        }
    }

    suspend fun prefetchRecentMessages(
        cacheOwnerId: String,
        conversations: List<Conversation>,
        topCount: Int = 5,
        limit: Int = RECENT_MESSAGE_LIMIT,
        freshnessWindowMs: Long = 2 * 60_000L
    ) {
        withContext(Dispatchers.IO) {
            val topConversations = conversations
                .sortedByDescending(::conversationSortEpochMillis)
                .take(topCount)

            topConversations.forEach { conversation ->
                val cachedConversation = dao.getConversation(cacheOwnerId, conversation.id)
                val isFresh = cachedConversation != null &&
                    cachedConversation.messagesCachedAt > 0L &&
                    (System.currentTimeMillis() - cachedConversation.messagesCachedAt) < freshnessWindowMs
                if (!isFresh) {
                    refreshMessages(
                        cacheOwnerId = cacheOwnerId,
                        conversationId = conversation.id,
                        limit = limit,
                        cursor = null
                    )
                }
            }
        }
    }

    suspend fun cacheConversationMessagesSnapshot(
        cacheOwnerId: String,
        conversation: Conversation?,
        messages: List<Message>,
        nextCursor: String?,
        hasMore: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val conversationId = conversation?.id ?: return@withContext
            val existingConversation = conversation?.let {
                dao.getConversation(cacheOwnerId, it.id)
            }
            val persistedMessages = dedupeAndSort(messages).takeLast(RECENT_MESSAGE_LIMIT)
            val effectiveNextCursor = if (messages.size > RECENT_MESSAGE_LIMIT) {
                existingConversation?.cachedNextCursor
            } else {
                nextCursor
            }
            val effectiveHasMore = if (messages.size > RECENT_MESSAGE_LIMIT) {
                existingConversation?.hasMoreMessages ?: hasMore
            } else {
                hasMore
            }

            database.withTransaction {
                conversation?.let {
                    val conversationWithFreshSummary = it.copy(
                        lastMessage = persistedMessages.lastOrNull()?.toConversationLastMessage() ?: it.lastMessage,
                        lastMessageAt = persistedMessages.lastOrNull()?.createdAt ?: it.lastMessageAt,
                        updatedAt = persistedMessages.lastOrNull()?.updatedAt ?: it.updatedAt
                    )
                    val entity = conversationToEntity(
                        cacheOwnerId = cacheOwnerId,
                        conversation = conversationWithFreshSummary,
                        existing = existingConversation,
                        messagesCachedAt = System.currentTimeMillis(),
                        cachedNextCursor = effectiveNextCursor,
                        hasMoreMessages = effectiveHasMore
                    )
                    dao.upsertConversations(listOf(entity))
                }

                dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
                if (persistedMessages.isNotEmpty()) {
                    dao.upsertMessages(
                        persistedMessages.map { message ->
                            messageToEntity(cacheOwnerId, message)
                        }
                    )
                    dao.trimMessages(cacheOwnerId, conversationId, RECENT_MESSAGE_LIMIT)
                }
            }
        }
    }

    suspend fun upsertIncomingMessage(
        cacheOwnerId: String,
        conversationId: String,
        message: Message,
        currentUserId: String?,
        incrementUnread: Boolean
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.upsertMessages(listOf(messageToEntity(cacheOwnerId, message)))
                dao.trimMessages(cacheOwnerId, conversationId, RECENT_MESSAGE_LIMIT)

                val existingConversation = dao.getConversation(cacheOwnerId, conversationId)
                if (existingConversation != null) {
                    val updatedConversation = existingConversation.copy(
                        lastMessageJson = json.encodeToString(message.toConversationLastMessage()),
                        lastMessageAt = message.createdAt,
                        lastMessageAtEpochMillis = parseEpochMillis(message.createdAt),
                        updatedAt = message.updatedAt,
                        updatedAtEpochMillis = parseEpochMillis(message.updatedAt),
                        unreadCount = if (incrementUnread && message.senderId != currentUserId) {
                            existingConversation.unreadCount + 1
                        } else {
                            existingConversation.unreadCount
                        }
                    )
                    dao.upsertConversations(listOf(updatedConversation))
                }
            }
        }
    }

    suspend fun markConversationRead(
        cacheOwnerId: String,
        conversationId: String
    ) {
        withContext(Dispatchers.IO) {
            val conversation = dao.getConversation(cacheOwnerId, conversationId) ?: return@withContext
            dao.upsertConversations(listOf(conversation.copy(unreadCount = 0)))
        }
    }

    suspend fun markOwnMessagesAsReadByPeer(
        cacheOwnerId: String,
        conversationId: String,
        currentUserId: String
    ) {
        withContext(Dispatchers.IO) {
            val messages = dao.getMessages(cacheOwnerId, conversationId)
            val updatedMessages = messages.map { entity ->
                if (entity.senderId == currentUserId && entity.status != "READ") {
                    entity.copy(status = "READ")
                } else {
                    entity
                }
            }
            if (updatedMessages != messages) {
                dao.upsertMessages(updatedMessages)
            }
        }
    }

    suspend fun updateCachedMessageContent(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String,
        content: String
    ) {
        withContext(Dispatchers.IO) {
            val messages = dao.getMessages(cacheOwnerId, conversationId)
            val updatedMessages = messages.map { entity ->
                if (entity.messageId == messageId) {
                    entity.copy(content = content, updatedAt = formatNowIso(), updatedAtEpochMillis = System.currentTimeMillis())
                } else {
                    entity
                }
            }
            if (updatedMessages == messages) return@withContext
            database.withTransaction {
                dao.upsertMessages(updatedMessages)
                val conversation = dao.getConversation(cacheOwnerId, conversationId)
                val latest = updatedMessages.maxByOrNull { it.createdAtEpochMillis }
                if (conversation != null && latest?.messageId == messageId) {
                    dao.upsertConversations(
                        listOf(
                            conversation.copy(
                                lastMessageJson = json.encodeToString(
                                    ConversationLastMessage(
                                        id = latest.messageId,
                                        content = content,
                                        contentType = latest.contentType,
                                        senderId = latest.senderId,
                                        status = latest.status,
                                        createdAt = latest.createdAt
                                    )
                                )
                            )
                        )
                    )
                }
            }
        }
    }

    suspend fun updateCachedMessageReactions(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String,
        reactions: List<MessageReaction>
    ) {
        withContext(Dispatchers.IO) {
            val messages = dao.getMessages(cacheOwnerId, conversationId)
            val updatedMessages = messages.map { entity ->
                if (entity.messageId == messageId) {
                    entity.copy(
                        reactionsJson = json.encodeToString(reactions),
                        updatedAt = formatNowIso(),
                        updatedAtEpochMillis = System.currentTimeMillis()
                    )
                } else {
                    entity
                }
            }
            if (updatedMessages != messages) {
                dao.upsertMessages(updatedMessages)
            }
        }
    }

    suspend fun deleteCachedMessage(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteMessage(cacheOwnerId, conversationId, messageId)
                val conversation = dao.getConversation(cacheOwnerId, conversationId) ?: return@withTransaction
                val latestMessage = dao.getMessages(cacheOwnerId, conversationId).lastOrNull()
                dao.upsertConversations(
                    listOf(
                        conversation.copy(
                            lastMessageJson = latestMessage?.let {
                                json.encodeToString(
                                    ConversationLastMessage(
                                        id = it.messageId,
                                        content = it.content,
                                        contentType = it.contentType,
                                        senderId = it.senderId,
                                        status = it.status,
                                        createdAt = it.createdAt
                                    )
                                )
                            },
                            lastMessageAt = latestMessage?.createdAt,
                            lastMessageAtEpochMillis = latestMessage?.createdAtEpochMillis ?: 0L
                        )
                    )
                )
            }
        }
    }

    suspend fun deleteConversation(
        cacheOwnerId: String,
        conversationId: String
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
                dao.deleteConversation(cacheOwnerId, conversationId)
            }
        }
    }

    private suspend fun cacheConversations(
        cacheOwnerId: String,
        conversations: List<Conversation>,
        replaceAll: Boolean
    ) {
        val existingById = dao.getConversations(cacheOwnerId).associateBy { it.conversationId }
        val entities = conversations.map { conversation ->
            conversationToEntity(
                cacheOwnerId = cacheOwnerId,
                conversation = conversation,
                existing = existingById[conversation.id]
            )
        }
        database.withTransaction {
            dao.upsertConversations(entities)
            if (replaceAll) {
                if (conversations.isEmpty()) {
                    dao.deleteAllConversations(cacheOwnerId)
                } else {
                    dao.deleteConversationsNotIn(cacheOwnerId, conversations.map { it.id })
                }
            }
        }
    }

    private suspend fun cacheMessagesResponse(
        cacheOwnerId: String,
        conversationId: String,
        response: MessagesResponse,
        cursor: String?
    ) {
        val existingConversation = dao.getConversation(cacheOwnerId, conversationId)
        val existingMessages = dao.getMessages(cacheOwnerId, conversationId).map(::entityToMessage)
        val mergedMessages = if (cursor == null) {
            dedupeAndSort(response.messages).takeLast(RECENT_MESSAGE_LIMIT)
        } else {
            dedupeAndSort(existingMessages + response.messages).takeLast(RECENT_MESSAGE_LIMIT)
        }
        val effectiveNextCursor = if (cursor == null) {
            response.nextCursor
        } else {
            existingConversation?.cachedNextCursor
        }
        val effectiveHasMore = if (cursor == null) {
            response.hasMore
        } else {
            existingConversation?.hasMoreMessages ?: response.hasMore
        }

        database.withTransaction {
            existingConversation?.let {
                dao.upsertConversations(
                    listOf(
                        it.copy(
                            messagesCachedAt = System.currentTimeMillis(),
                            cachedNextCursor = effectiveNextCursor,
                            hasMoreMessages = effectiveHasMore
                        )
                    )
                )
            }
            dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
            if (mergedMessages.isNotEmpty()) {
                dao.upsertMessages(mergedMessages.map { message -> messageToEntity(cacheOwnerId, message) })
                dao.trimMessages(cacheOwnerId, conversationId, RECENT_MESSAGE_LIMIT)
            }
        }
    }

    private fun conversationToEntity(
        cacheOwnerId: String,
        conversation: Conversation,
        existing: CachedConversationEntity? = null,
        messagesCachedAt: Long = existing?.messagesCachedAt ?: 0L,
        cachedNextCursor: String? = existing?.cachedNextCursor,
        hasMoreMessages: Boolean = existing?.hasMoreMessages ?: false
    ): CachedConversationEntity {
        return CachedConversationEntity(
            cacheOwnerId = cacheOwnerId,
            conversationId = conversation.id,
            participant1Id = conversation.participant1Id,
            participant2Id = conversation.participant2Id,
            participant1Json = json.encodeToString(conversation.participant1),
            participant2Json = json.encodeToString(conversation.participant2),
            otherParticipantJson = json.encodeToString(conversation.otherParticipant),
            lastMessageJson = conversation.lastMessage?.let(json::encodeToString),
            lastMessageAt = conversation.lastMessageAt,
            lastMessageAtEpochMillis = parseEpochMillis(conversation.lastMessageAt ?: conversation.lastMessage?.createdAt),
            unreadCount = conversation.unreadCount,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            updatedAtEpochMillis = parseEpochMillis(conversation.updatedAt),
            isMessageRequest = conversation.isMessageRequest,
            messagesCachedAt = messagesCachedAt,
            cachedNextCursor = cachedNextCursor,
            hasMoreMessages = hasMoreMessages
        )
    }

    private fun messageToEntity(
        cacheOwnerId: String,
        message: Message
    ): CachedMessageEntity {
        return CachedMessageEntity(
            cacheOwnerId = cacheOwnerId,
            conversationId = message.conversationId,
            messageId = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = message.content,
            contentType = message.contentType,
            mediaUrl = message.mediaUrl,
            mediaType = message.mediaType,
            fileName = message.fileName,
            fileSize = message.fileSize,
            status = message.status,
            deliveredAt = message.deliveredAt,
            readAt = message.readAt,
            isDeleted = message.isDeleted,
            replyToId = message.replyToId,
            replyToJson = message.replyTo?.let(json::encodeToString),
            senderJson = message.sender?.let(json::encodeToString),
            reactionsJson = if (message.reactions.isEmpty()) null else json.encodeToString(message.reactions),
            createdAt = message.createdAt,
            createdAtEpochMillis = parseEpochMillis(message.createdAt),
            updatedAt = message.updatedAt,
            updatedAtEpochMillis = parseEpochMillis(message.updatedAt)
        )
    }

    private fun entityToConversation(entity: CachedConversationEntity): Conversation {
        val participant1 = json.decodeFromString<ChatUser>(entity.participant1Json)
        val participant2 = json.decodeFromString<ChatUser>(entity.participant2Json)
        return Conversation(
            id = entity.conversationId,
            participant1Id = entity.participant1Id,
            participant2Id = entity.participant2Id,
            participant1 = participant1,
            participant2 = participant2,
            otherParticipant = json.decodeFromString<ChatUser>(entity.otherParticipantJson),
            lastMessage = entity.lastMessageJson?.let { json.decodeFromString<ConversationLastMessage>(it) },
            lastMessageAt = entity.lastMessageAt,
            unreadCount = entity.unreadCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isMessageRequest = entity.isMessageRequest
        )
    }

    private fun entityToMessage(entity: CachedMessageEntity): Message {
        return Message(
            id = entity.messageId,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            receiverId = entity.receiverId,
            content = entity.content,
            contentType = entity.contentType,
            mediaUrl = entity.mediaUrl,
            mediaType = entity.mediaType,
            fileName = entity.fileName,
            fileSize = entity.fileSize,
            status = entity.status,
            deliveredAt = entity.deliveredAt,
            readAt = entity.readAt,
            isDeleted = entity.isDeleted,
            replyToId = entity.replyToId,
            replyTo = entity.replyToJson?.let { json.decodeFromString<ReplyTo>(it) },
            sender = entity.senderJson?.let { json.decodeFromString<ChatUser>(it) },
            reactions = entity.reactionsJson?.let { json.decodeFromString<List<MessageReaction>>(it) }.orEmpty(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun dedupeAndSort(messages: List<Message>): List<Message> {
        return messages
            .groupBy { it.id }
            .map { (_, group) -> group.last() }
            .sortedWith(compareBy<Message> { parseEpochMillis(it.createdAt) }.thenBy { it.createdAt })
    }

    private fun conversationSortEpochMillis(conversation: Conversation): Long {
        return parseEpochMillis(conversation.lastMessageAt ?: conversation.updatedAt.ifBlank { conversation.createdAt })
    }

    private fun parseEpochMillis(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        patterns.forEach { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()?.let { return it }
        }
        return 0L
    }

    private fun formatNowIso(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun Message.toConversationLastMessage(): ConversationLastMessage {
        return ConversationLastMessage(
            id = id,
            content = content,
            contentType = contentType,
            senderId = senderId,
            status = status,
            createdAt = createdAt
        )
    }
}
