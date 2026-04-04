package com.kyant.backdrop.catalog.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.chat.cache.CachedMessagesSnapshot
import com.kyant.backdrop.catalog.chat.cache.ChatCacheRepository
import com.kyant.backdrop.catalog.network.AgentApiService
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.MessageReaction
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "ChatViewModel"

enum class ChatThreadReadyState {
    CACHED_READY,
    LOADING_WITHOUT_CACHE,
    EMPTY_THREAD,
    MESSAGES_READY
}

data class ChatUiState(
    val currentUserId: String? = null,
    val conversations: List<Conversation> = emptyList(),
    val messages: List<Message> = emptyList(),
    val selectedConversation: Conversation? = null,
    val isResolvingConversationOpen: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = false,
    val messagesNextCursor: String? = null,
    val isSending: Boolean = false,
    val error: String? = null,
    val typingUserId: String? = null,
    val unreadCount: Int = 0,
    val messageRequestsCount: Int = 0,
    val aiSuggestions: List<String> = emptyList(),
    val isLoadingAiSuggestions: Boolean = false,
    val socketConnected: Boolean = false,
    val replyToMessage: Message? = null,
    val isUploadingAttachment: Boolean = false,
    val attachmentUploadError: String? = null,
    val isRecordingVoice: Boolean = false,
    val threadReadyState: ChatThreadReadyState = ChatThreadReadyState.EMPTY_THREAD
)

class ChatViewModel(private val context: Context) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val chatCacheRepository = ChatCacheRepository(context)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var loadConversationsJob: Job? = null
    private var loadMessagesJob: Job? = null
    private var preloadJob: Job? = null
    private var typingIndicatorTimeoutJob: Job? = null
    private var outgoingTypingStopJob: Job? = null
    private var hasLoadedConversations: Boolean = false
    private var conversationsLastLoadedAt: Long = 0L
    private var lastPreloadedUserId: String? = null
    private val conversationsCacheTtlMs: Long = 90_000L
    private var isOutgoingTyping = false
    private var outgoingTypingConversationId: String? = null

    private data class CachedConversationMessages(
        val messages: List<Message>,
        val nextCursor: String?,
        val hasMore: Boolean,
        val cachedAt: Long
    )

    private val messagesCacheByConversation = mutableMapOf<String, CachedConversationMessages>()
    private val messagesCacheTtlMs: Long = 2 * 60_000L
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private var mediaRecorder: MediaRecorder? = null
    private var voiceRecordingFile: File? = null

    init {
        Log.d(TAG, "ChatViewModel init")
        viewModelScope.launch {
            val token = ApiClient.getToken(context)
            val userId = ApiClient.getCurrentUserId(context)
            if (!token.isNullOrEmpty()) {
                ChatSocketManager.connect(token)
            } else {
                Log.w(TAG, "No token available, socket not connected")
            }
            ChatSocketManager.currentUserId = userId
            _uiState.update { it.copy(currentUserId = userId) }
        }
        collectSocketEvents()
        collectConnectionState()
    }

    private fun collectConnectionState() {
        viewModelScope.launch {
            ChatSocketManager.connectionStateFlow.collect { state ->
                _uiState.update {
                    it.copy(socketConnected = state == ChatSocketManager.ConnectionState.CONNECTED)
                }
            }
        }
    }

    private fun collectSocketEvents() {
        viewModelScope.launch {
            ChatSocketManager.newMessageFlow.collect { (conversationId, messageJson) ->
                try {
                    val message = json.decodeFromString(Message.serializer(), messageJson)
                    val currentUserId = ensureCurrentUserId()
                    val isSelectedConversation = _uiState.value.selectedConversation?.id == conversationId

                    if (isSelectedConversation) {
                        if (message.senderId != _uiState.value.currentUserId) {
                            ChatSocketManager.markRead(conversationId)
                        }
                        _uiState.update { state ->
                            val updatedMessages = replaceMatchingPendingMessage(
                                upsertMessage(state.messages, message),
                                message
                            )
                            state.copy(
                                messages = updatedMessages,
                                threadReadyState = resolveThreadReadyState(
                                    messages = updatedMessages,
                                    fromCache = false
                                )
                            )
                        }
                        persistSelectedConversationSnapshot()
                        currentUserId?.let {
                            chatCacheRepository.markConversationRead(it, conversationId)
                        }
                    } else {
                        currentUserId?.let {
                            chatCacheRepository.upsertIncomingMessage(
                                cacheOwnerId = it,
                                conversationId = conversationId,
                                message = message,
                                currentUserId = _uiState.value.currentUserId,
                                incrementUnread = true
                            )
                        }
                    }

                    refreshConversations()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing socket message", e)
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.typingFlow.collect { (conversationId, userId, isTyping) ->
                val state = _uiState.value
                if (state.selectedConversation?.id != conversationId || userId == state.currentUserId) {
                    return@collect
                }
                if (isTyping) {
                    scheduleTypingIndicatorClear(conversationId, userId)
                } else {
                    clearTypingIndicator()
                }
                _uiState.update { currentState ->
                    if (currentState.selectedConversation?.id != conversationId) return@update currentState
                    currentState.copy(typingUserId = if (isTyping) userId else null)
                }
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messagesReadFlow.collect { (conversationId, _) ->
                _uiState.update { state ->
                    val updatedMessages = if (state.selectedConversation?.id == conversationId) {
                        state.messages.map { message ->
                            if (message.senderId == state.currentUserId) {
                                message.copy(status = "READ")
                            } else {
                                message
                            }
                        }
                    } else {
                        state.messages
                    }
                    state.copy(messages = updatedMessages)
                }
                val currentUserId = ensureCurrentUserId()
                if (!currentUserId.isNullOrBlank()) {
                    chatCacheRepository.markOwnMessagesAsReadByPeer(
                        cacheOwnerId = currentUserId,
                        conversationId = conversationId,
                        currentUserId = currentUserId
                    )
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messageDeletedFlow.collect { (messageId, conversationId, _) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id == conversationId) {
                        val updatedMessages = state.messages.filter { it.id != messageId }
                        state.copy(
                            messages = updatedMessages,
                            threadReadyState = resolveThreadReadyState(
                                messages = updatedMessages,
                                fromCache = false
                            )
                        )
                    } else {
                        state
                    }
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.deleteCachedMessage(userId, conversationId, messageId)
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.messageEditedFlow.collect { (messageId, conversationId, content) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id == conversationId) {
                        state.copy(
                            messages = state.messages.map { message ->
                                if (message.id == messageId) message.copy(content = content) else message
                            }
                        )
                    } else {
                        state
                    }
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.updateCachedMessageContent(
                        cacheOwnerId = userId,
                        conversationId = conversationId,
                        messageId = messageId,
                        content = content
                    )
                    if (_uiState.value.selectedConversation?.id == conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
                refreshConversations()
            }
        }

        viewModelScope.launch {
            ChatSocketManager.reactionFlow.collect { event ->
                var updatedReactions: List<MessageReaction>? = null
                _uiState.update { state ->
                    if (state.selectedConversation?.id != event.conversationId) return@update state
                    val updatedMessages = state.messages.map { message ->
                        if (message.id != event.messageId) return@map message
                        val existing = message.reactions.firstOrNull { it.userId == event.userId }
                        val nextReactions = when (event.action) {
                            "removed" -> message.reactions.filterNot { it.userId == event.userId }
                            "updated" -> message.reactions.map {
                                if (it.userId == event.userId) it.copy(emoji = event.emoji) else it
                            }
                            else -> {
                                if (existing == null) {
                                    message.reactions + MessageReaction(
                                        id = "local-${event.messageId}-${event.userId}",
                                        userId = event.userId,
                                        emoji = event.emoji
                                    )
                                } else {
                                    message.reactions.map {
                                        if (it.userId == event.userId) it.copy(emoji = event.emoji) else it
                                    }
                                }
                            }
                        }
                        updatedReactions = nextReactions
                        message.copy(reactions = nextReactions)
                    }
                    state.copy(messages = updatedMessages)
                }
                ensureCurrentUserId()?.let { userId ->
                    updatedReactions?.let { reactions ->
                        chatCacheRepository.updateCachedMessageReactions(
                            cacheOwnerId = userId,
                            conversationId = event.conversationId,
                            messageId = event.messageId,
                            reactions = reactions
                        )
                    }
                    if (_uiState.value.selectedConversation?.id == event.conversationId) {
                        persistSelectedConversationSnapshot()
                    }
                }
            }
        }
    }

    fun ensureSocketConnected() {
        viewModelScope.launch {
            val token = ApiClient.getToken(context)
            if (!token.isNullOrEmpty()) {
                ChatSocketManager.reconnectIfNeeded()
            }
        }
    }

    fun preloadChats(forceRefresh: Boolean = false) {
        if (preloadJob?.isActive == true && !forceRefresh) return

        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            val token = ApiClient.getToken(context) ?: return@launch
            ChatSocketManager.connect(token)

            val currentUserId = ensureCurrentUserId() ?: return@launch
            if (_uiState.value.conversations.isEmpty()) {
                hydrateCachedConversations(currentUserId)
            }

            val now = System.currentTimeMillis()
            val shouldRefresh =
                forceRefresh ||
                currentUserId != lastPreloadedUserId ||
                _uiState.value.conversations.isEmpty() ||
                (now - conversationsLastLoadedAt) >= conversationsCacheTtlMs

            lastPreloadedUserId = currentUserId
            if (shouldRefresh) {
                loadConversationsInternal(
                    cacheOwnerId = currentUserId,
                    prefetchRecentMessages = true
                )
            } else {
                loadUnreadAndRequestsCount()
                runCatching {
                    chatCacheRepository.prefetchRecentMessages(
                        cacheOwnerId = currentUserId,
                        conversations = _uiState.value.conversations
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Failed to prefetch cached chats", error)
                }
            }
        }
    }

    fun ensureConversationsLoaded(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId() ?: return@launch
            if (_uiState.value.conversations.isEmpty()) {
                hydrateCachedConversations(currentUserId)
            }

            val now = System.currentTimeMillis()
            val hasFreshCache =
                hasLoadedConversations &&
                _uiState.value.conversations.isNotEmpty() &&
                (now - conversationsLastLoadedAt) < conversationsCacheTtlMs

            if (!forceRefresh && hasFreshCache) {
                loadUnreadAndRequestsCount()
                return@launch
            }

            loadConversationsInternal(cacheOwnerId = currentUserId)
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            ensureCurrentUserId()?.let { currentUserId ->
                loadConversationsInternal(cacheOwnerId = currentUserId)
            }
        }
    }

    fun selectConversation(conversation: Conversation?) {
        stopOutgoingTyping()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        clearTypingIndicator()

        ChatSocketManager.activeConversationId = conversation?.id
        conversation?.let { MessageNotificationManager.clearConversationNotification(context, it.id) }

        if (conversation == null) {
            _uiState.update {
                it.copy(
                    selectedConversation = null,
                    isResolvingConversationOpen = false,
                    messages = emptyList(),
                    messagesNextCursor = null,
                    hasMoreMessages = false,
                    typingUserId = null,
                    error = null,
                    isLoadingMessages = false,
                    isLoadingMoreMessages = false,
                    threadReadyState = ChatThreadReadyState.EMPTY_THREAD
                )
            }
            return
        }

        val inMemoryCache = messagesCacheByConversation[conversation.id]
        val hasFreshInMemoryCache = inMemoryCache != null &&
            (System.currentTimeMillis() - inMemoryCache.cachedAt) < messagesCacheTtlMs
        val freshInMemoryCache = inMemoryCache?.takeIf { hasFreshInMemoryCache }

        _uiState.update {
            it.copy(
                selectedConversation = conversation,
                isResolvingConversationOpen = false,
                messages = freshInMemoryCache?.messages.orEmpty(),
                messagesNextCursor = freshInMemoryCache?.nextCursor,
                hasMoreMessages = freshInMemoryCache?.hasMore ?: false,
                typingUserId = null,
                error = null,
                isLoadingMessages = !hasFreshInMemoryCache,
                isLoadingMoreMessages = false,
                threadReadyState = when {
                    freshInMemoryCache?.messages?.isNotEmpty() == true -> ChatThreadReadyState.CACHED_READY
                    hasFreshInMemoryCache -> ChatThreadReadyState.EMPTY_THREAD
                    else -> ChatThreadReadyState.LOADING_WITHOUT_CACHE
                }
            )
        }

        ChatSocketManager.joinChat(conversation.id)
        ChatSocketManager.markRead(conversation.id)

        viewModelScope.launch {
            ApiClient.markAsRead(context, conversation.id)
            val currentUserId = ensureCurrentUserId()
            currentUserId?.let {
                chatCacheRepository.markConversationRead(it, conversation.id)
            }

            val cachedSnapshot = if (!hasFreshInMemoryCache && !currentUserId.isNullOrBlank()) {
                hydrateCachedMessages(currentUserId, conversation.id)
            } else {
                null
            }
            val hasFreshLocalSnapshot = cachedSnapshot != null &&
                (System.currentTimeMillis() - cachedSnapshot.cachedAt) < messagesCacheTtlMs

            if (cachedSnapshot != null && _uiState.value.selectedConversation?.id == conversation.id) {
                _uiState.update { state ->
                    state.copy(
                        messages = cachedSnapshot.messages,
                        messagesNextCursor = cachedSnapshot.nextCursor,
                        hasMoreMessages = cachedSnapshot.hasMore,
                        isLoadingMessages = false,
                        error = null,
                        threadReadyState = when {
                            cachedSnapshot.messages.isNotEmpty() -> ChatThreadReadyState.CACHED_READY
                            hasFreshLocalSnapshot -> ChatThreadReadyState.EMPTY_THREAD
                            else -> ChatThreadReadyState.LOADING_WITHOUT_CACHE
                        }
                    )
                }
            }

            val shouldLoadFromNetwork =
                (!hasFreshInMemoryCache && !hasFreshLocalSnapshot) ||
                    (_uiState.value.messages.isEmpty() && conversation.lastMessage != null)

            if (shouldLoadFromNetwork) {
                loadMessages(conversation.id)
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMessages = false,
                        threadReadyState = resolveThreadReadyState(
                            messages = it.messages,
                            fromCache = true
                        )
                    )
                }
            }
        }
    }

    fun loadMessages(conversationId: String, cursor: String? = null) {
        if (_uiState.value.selectedConversation?.id != conversationId) return

        loadMessagesJob?.cancel()
        loadMessagesJob = viewModelScope.launch {
            val currentUserId = ensureCurrentUserId() ?: return@launch
            if (cursor == null) {
                _uiState.update {
                    it.copy(
                        isLoadingMessages = true,
                        error = null,
                        threadReadyState = if (it.messages.isEmpty()) {
                            ChatThreadReadyState.LOADING_WITHOUT_CACHE
                        } else {
                            it.threadReadyState
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMoreMessages = true, error = null) }
            }

            chatCacheRepository.refreshMessages(
                cacheOwnerId = currentUserId,
                conversationId = conversationId,
                limit = 50,
                cursor = cursor
            )
                .onSuccess { response ->
                    _uiState.update { state ->
                        val updatedMessages = if (cursor == null) {
                            dedupeAndSortByCreatedAt(response.messages)
                        } else {
                            dedupeAndSortByCreatedAt(state.messages + response.messages)
                        }
                        state.copy(
                            messages = updatedMessages,
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            hasMoreMessages = response.hasMore,
                            messagesNextCursor = response.nextCursor,
                            error = null,
                            threadReadyState = resolveThreadReadyState(
                                messages = updatedMessages,
                                fromCache = false
                            )
                        )
                    }
                    cacheCurrentMessages(conversationId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            error = error.message,
                            threadReadyState = if (it.messages.isEmpty()) {
                                ChatThreadReadyState.EMPTY_THREAD
                            } else {
                                it.threadReadyState
                            }
                        )
                    }
                }
        }
    }

    fun loadMoreMessages() {
        val state = _uiState.value
        if (state.isLoadingMessages || state.isLoadingMoreMessages || !state.hasMoreMessages) return

        val conversationId = state.selectedConversation?.id ?: return
        val cursor = state.messagesNextCursor ?: return
        loadMessages(conversationId, cursor)
    }

    fun uploadAndSendMessage(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String = "",
        replyToId: String? = null
    ) {
        val conversation = _uiState.value.selectedConversation ?: return
        val currentUserId = _uiState.value.currentUserId.orEmpty()
        val contentType = when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "document"
        }
        val pendingLabel = when (contentType) {
            "image" -> "Photo"
            "video" -> "Video"
            "audio" -> "Voice"
            else -> "Document"
        }
        val pendingId = "pending-${System.currentTimeMillis()}"
        val nowIso = isoFormatter.format(Date())
        val optimisticMessage = Message(
            id = pendingId,
            conversationId = conversation.id,
            senderId = currentUserId,
            receiverId = conversation.otherParticipant.id,
            content = "Sending $pendingLabel...",
            contentType = "pending",
            status = "SENDING",
            createdAt = nowIso,
            updatedAt = nowIso
        )

        _uiState.update { state ->
            state.copy(
                messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                isUploadingAttachment = false,
                attachmentUploadError = null,
                threadReadyState = ChatThreadReadyState.MESSAGES_READY
            )
        }

        viewModelScope.launch {
            ApiClient.uploadChatMedia(context, fileBytes, fileName, mimeType)
                .onSuccess { upload ->
                    val content = caption.ifBlank {
                        when (contentType) {
                            "image" -> "📷 Photo"
                            "video" -> "🎬 Video"
                            "audio" -> "🎤 Voice message"
                            else -> "📎 ${upload.fileName}"
                        }
                    }

                    ApiClient.sendMessage(
                        context,
                        conversation.id,
                        content,
                        contentType,
                        mediaUrl = upload.mediaUrl,
                        mediaType = upload.mediaType,
                        fileName = upload.fileName,
                        fileSize = upload.fileSize,
                        replyToId = replyToId
                    ).onSuccess { message ->
                        _uiState.update { state ->
                            val withoutPending = state.messages.filterNot { it.id == pendingId }
                            state.copy(
                                messages = dedupeAndSortByCreatedAt(withoutPending + message),
                                attachmentUploadError = null,
                                threadReadyState = ChatThreadReadyState.MESSAGES_READY
                            )
                        }
                        persistSelectedConversationSnapshot()
                        refreshConversations()
                    }.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.filterNot { it.id == pendingId },
                                attachmentUploadError = error.message
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filterNot { it.id == pendingId },
                            attachmentUploadError = error.message
                        )
                    }
                }
        }
    }

    fun clearAttachmentError() {
        _uiState.update { it.copy(attachmentUploadError = null) }
    }

    fun startVoiceRecording(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(context.cacheDir, "chat_voice_${System.currentTimeMillis()}.m4a")
                    voiceRecordingFile = file
                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    mediaRecorder = recorder
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = true) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start voice recording", e)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(attachmentUploadError = "Could not start recording") }
                    }
                }
            }
        }
    }

    fun stopVoiceRecordingAndSend(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    val file = voiceRecordingFile ?: return@withContext
                    voiceRecordingFile = null
                    val bytes = file.readBytes()
                    file.delete()
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                        uploadAndSendMessage(
                            fileBytes = bytes,
                            fileName = "voice.m4a",
                            mimeType = "audio/mp4",
                            caption = "",
                            replyToId = _uiState.value.replyToMessage?.id
                        )
                        clearReplyTo()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop/send voice", e)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(isRecordingVoice = false, attachmentUploadError = e.message)
                        }
                    }
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                    voiceRecordingFile?.delete()
                    voiceRecordingFile = null
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        val conversation = _uiState.value.selectedConversation ?: return
        if (content.isBlank()) return

        val nowIso = isoFormatter.format(Date())
        val tempMessageId = "local-${System.currentTimeMillis()}"

        viewModelScope.launch {
            _uiState.update { state ->
                val optimisticMessage = Message(
                    id = tempMessageId,
                    conversationId = conversation.id,
                    senderId = state.currentUserId.orEmpty(),
                    receiverId = conversation.otherParticipant.id,
                    content = content,
                    contentType = "text",
                    status = "SENT",
                    createdAt = nowIso,
                    updatedAt = nowIso
                )
                state.copy(
                    messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                    isSending = true,
                    error = null,
                    threadReadyState = ChatThreadReadyState.MESSAGES_READY
                )
            }

            ApiClient.sendMessage(context, conversation.id, content, "text", replyToId = replyToId)
                .onSuccess { message ->
                    _uiState.update { state ->
                        val withoutTemp = state.messages.filterNot { it.id == tempMessageId }
                        state.copy(
                            messages = dedupeAndSortByCreatedAt(withoutTemp + message),
                            isSending = false,
                            threadReadyState = ChatThreadReadyState.MESSAGES_READY
                        )
                    }
                    persistSelectedConversationSnapshot()
                    refreshConversations()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filterNot { it.id == tempMessageId },
                            isSending = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        val conversationId = _uiState.value.selectedConversation?.id ?: return

        if (!isTyping) {
            stopOutgoingTyping()
            return
        }

        if (!isOutgoingTyping || outgoingTypingConversationId != conversationId) {
            ChatSocketManager.sendTyping(conversationId, true)
            isOutgoingTyping = true
            outgoingTypingConversationId = conversationId
        }

        outgoingTypingStopJob?.cancel()
        outgoingTypingStopJob = viewModelScope.launch {
            delay(2_000)
            if (_uiState.value.selectedConversation?.id == conversationId) {
                stopOutgoingTyping(conversationId)
            }
        }
    }

    fun markAsRead() {
        _uiState.value.selectedConversation?.let { conversation ->
            ChatSocketManager.markRead(conversation.id)
            viewModelScope.launch {
                ApiClient.markAsRead(context, conversation.id)
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.markConversationRead(userId, conversation.id)
                }
            }
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean = false) {
        val conversationId = _uiState.value.selectedConversation?.id ?: return
        viewModelScope.launch {
            ApiClient.deleteMessage(context, messageId, forEveryone).onSuccess {
                _uiState.update { state ->
                    val updatedMessages = state.messages.filter { it.id != messageId }
                    state.copy(
                        messages = updatedMessages,
                        threadReadyState = resolveThreadReadyState(
                            messages = updatedMessages,
                            fromCache = false
                        )
                    )
                }
                ensureCurrentUserId()?.let { userId ->
                    chatCacheRepository.deleteCachedMessage(userId, conversationId, messageId)
                }
                persistSelectedConversationSnapshot()
                refreshConversations()
            }
        }
    }

    fun setReplyTo(message: Message?) {
        _uiState.update { it.copy(replyToMessage = message) }
    }

    fun clearReplyTo() {
        _uiState.update { it.copy(replyToMessage = null) }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            ApiClient.addReaction(context, messageId, emoji).onSuccess {
                var updatedReactions: List<MessageReaction>? = null
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { message ->
                            if (message.id == messageId) {
                                val existingReaction = message.reactions.find { it.userId == state.currentUserId }
                                val newReactions = if (existingReaction != null) {
                                    if (existingReaction.emoji == emoji) {
                                        message.reactions.filter { it.userId != state.currentUserId }
                                    } else {
                                        message.reactions.map { reaction ->
                                            if (reaction.userId == state.currentUserId) {
                                                reaction.copy(emoji = emoji)
                                            } else {
                                                reaction
                                            }
                                        }
                                    }
                                } else {
                                    message.reactions + MessageReaction(
                                        id = "local-${System.currentTimeMillis()}",
                                        userId = state.currentUserId.orEmpty(),
                                        emoji = emoji
                                    )
                                }
                                updatedReactions = newReactions
                                message.copy(reactions = newReactions)
                            } else {
                                message
                            }
                        }
                    )
                }
                val conversationId = _uiState.value.selectedConversation?.id
                val currentUserId = ensureCurrentUserId()
                if (!conversationId.isNullOrBlank() && !currentUserId.isNullOrBlank() && updatedReactions != null) {
                    chatCacheRepository.updateCachedMessageReactions(
                        cacheOwnerId = currentUserId,
                        conversationId = conversationId,
                        messageId = messageId,
                        reactions = updatedReactions.orEmpty()
                    )
                }
                persistSelectedConversationSnapshot()
            }
        }
    }

    fun openChatWithUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingConversationOpen = true, error = null) }
            val currentUserId = ensureCurrentUserId()
            val existingConversation = _uiState.value.conversations.firstOrNull { it.otherParticipant.id == userId }
                ?: currentUserId?.let { cacheOwnerId ->
                    runCatching {
                        chatCacheRepository.getCachedConversations(cacheOwnerId)
                    }.getOrNull()?.firstOrNull { it.otherParticipant.id == userId }
                }

            if (existingConversation != null) {
                currentUserId?.let { cacheOwnerId ->
                    chatCacheRepository.upsertConversation(cacheOwnerId, existingConversation)
                }
                _uiState.update { state ->
                    state.copy(
                        conversations = listOf(existingConversation) + state.conversations.filterNot { it.id == existingConversation.id },
                        error = null
                    )
                }
                if (_uiState.value.selectedConversation?.id != existingConversation.id) {
                    selectConversation(existingConversation)
                } else {
                    _uiState.update {
                        it.copy(
                            selectedConversation = existingConversation,
                            isResolvingConversationOpen = false,
                            error = null
                        )
                    }
                }
            }

            ApiClient.getOrCreateConversation(context, userId)
                .onSuccess { conversation ->
                    currentUserId?.let { cacheOwnerId ->
                        chatCacheRepository.upsertConversation(cacheOwnerId, conversation)
                    }
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(conversation) + state.conversations.filterNot { it.id == conversation.id }
                        )
                    }
                    if (_uiState.value.selectedConversation?.id != conversation.id) {
                        selectConversation(conversation)
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                selectedConversation = conversation,
                                isResolvingConversationOpen = false,
                                error = null
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to open chat with user $userId", error)
                    _uiState.update {
                        it.copy(
                            isResolvingConversationOpen = false,
                            error = error.message ?: "Failed to open chat"
                        )
                    }
                }
        }
    }

    fun openConversationById(conversationId: String) {
        _uiState.update { it.copy(isResolvingConversationOpen = true, error = null) }

        _uiState.value.conversations.firstOrNull { it.id == conversationId }?.let { conversation ->
            selectConversation(conversation)
            return
        }

        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId()
            val cachedConversation = currentUserId?.let {
                runCatching {
                    chatCacheRepository.getCachedConversation(it, conversationId)
                }.getOrNull()
            }
            if (cachedConversation != null) {
                _uiState.update { state ->
                    state.copy(
                        conversations = listOf(cachedConversation) + state.conversations.filterNot { it.id == cachedConversation.id },
                        isResolvingConversationOpen = false,
                        error = null
                    )
                }
                selectConversation(cachedConversation)
            }

            ApiClient.getConversation(context, conversationId)
                .onSuccess { conversation ->
                    currentUserId?.let { userId ->
                        chatCacheRepository.upsertConversation(userId, conversation)
                    }
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(conversation) + state.conversations.filterNot { it.id == conversation.id },
                            selectedConversation = if (state.selectedConversation?.id == conversation.id) {
                                conversation
                            } else {
                                state.selectedConversation
                            },
                            isResolvingConversationOpen = false,
                            error = null
                        )
                    }
                    if (_uiState.value.selectedConversation?.id != conversation.id) {
                        selectConversation(conversation)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to open conversation $conversationId", error)
                    _uiState.update {
                        it.copy(
                            isResolvingConversationOpen = false,
                            error = error.message ?: "Failed to open chat"
                        )
                    }
                }
        }
    }

    fun clearLocalMessages() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                threadReadyState = ChatThreadReadyState.EMPTY_THREAD
            )
        }
    }

    fun deleteCurrentConversation(onSuccess: () -> Unit = {}) {
        val conversation = _uiState.value.selectedConversation ?: return
        viewModelScope.launch {
            ApiClient.deleteConversation(context, conversation.id)
                .onSuccess {
                    ensureCurrentUserId()?.let { userId ->
                        chatCacheRepository.deleteConversation(userId, conversation.id)
                    }
                    selectConversation(null)
                    _uiState.update { state ->
                        state.copy(conversations = state.conversations.filter { it.id != conversation.id })
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete conversation", error)
                    _uiState.update { it.copy(error = "Failed to delete chat: ${error.message}") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun loadAiSuggestions() {
        val lastMessage = _uiState.value.messages.lastOrNull { it.senderId != _uiState.value.currentUserId }
        val conversationId = _uiState.value.selectedConversation?.id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAiSuggestions = true) }
            val messageText = lastMessage?.content
            if (messageText.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        aiSuggestions = generateMockSuggestions(lastMessage?.content),
                        isLoadingAiSuggestions = false
                    )
                }
                return@launch
            }

            AgentApiService.getSmartReplies(
                context = context,
                lastMessage = messageText,
                conversationId = conversationId
            ).onSuccess { replies ->
                _uiState.update {
                    it.copy(
                        aiSuggestions = replies.ifEmpty { generateMockSuggestions(messageText) },
                        isLoadingAiSuggestions = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        aiSuggestions = generateMockSuggestions(messageText),
                        isLoadingAiSuggestions = false
                    )
                }
            }
        }
    }

    fun clearAiSuggestions() {
        _uiState.update { it.copy(aiSuggestions = emptyList()) }
    }

    fun useAiSuggestion(suggestion: String) {
        sendMessage(suggestion)
        clearAiSuggestions()
    }

    private fun generateMockSuggestions(lastMessageContent: String?): List<String> {
        return when {
            lastMessageContent == null -> listOf("Hey! 👋", "How's it going?", "What's up?")
            lastMessageContent.contains("?", ignoreCase = true) -> listOf("Yes, sounds good!", "Let me check", "I'll get back to you")
            lastMessageContent.contains("thanks", ignoreCase = true) ||
                lastMessageContent.contains("thank", ignoreCase = true) -> listOf("You're welcome! 😊", "Anytime!", "Happy to help")
            lastMessageContent.contains("meet", ignoreCase = true) ||
                lastMessageContent.contains("call", ignoreCase = true) -> listOf("Sure, I'm free!", "What time works?", "Let's schedule it")
            lastMessageContent.contains("project", ignoreCase = true) ||
                lastMessageContent.contains("work", ignoreCase = true) -> listOf("I'll look into it", "Sounds interesting!", "Let's discuss more")
            else -> listOf("Got it! 👍", "Sounds good", "Let me know")
        }
    }

    private suspend fun ensureCurrentUserId(): String? {
        val existing = _uiState.value.currentUserId
        if (!existing.isNullOrBlank()) return existing

        val userId = ApiClient.getCurrentUserId(context)
        if (!userId.isNullOrBlank()) {
            ChatSocketManager.currentUserId = userId
            _uiState.update { it.copy(currentUserId = userId) }
        }
        return userId
    }

    private fun loadConversationsInternal(
        cacheOwnerId: String,
        prefetchRecentMessages: Boolean = false
    ) {
        if (loadConversationsJob?.isActive == true) return

        loadConversationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversations = true, error = null) }
            chatCacheRepository.refreshConversations(cacheOwnerId = cacheOwnerId)
                .onSuccess { response ->
                    hasLoadedConversations = true
                    conversationsLastLoadedAt = System.currentTimeMillis()
                    _uiState.update { state ->
                        val refreshedSelectedConversation = state.selectedConversation?.let { selected ->
                            response.conversations.firstOrNull { it.id == selected.id } ?: selected
                        }
                        state.copy(
                            conversations = response.conversations,
                            selectedConversation = refreshedSelectedConversation,
                            isLoadingConversations = false,
                            error = null
                        )
                    }
                    if (prefetchRecentMessages) {
                        runCatching {
                            chatCacheRepository.prefetchRecentMessages(
                                cacheOwnerId = cacheOwnerId,
                                conversations = response.conversations
                            )
                        }.onFailure { error ->
                            Log.w(TAG, "Failed to prefetch recent chat messages", error)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingConversations = false, error = error.message)
                    }
                }
            loadUnreadAndRequestsCount()
        }
    }

    private fun refreshConversations() {
        viewModelScope.launch {
            val currentUserId = ensureCurrentUserId() ?: return@launch
            chatCacheRepository.refreshConversations(cacheOwnerId = currentUserId)
                .onSuccess { response ->
                    hasLoadedConversations = true
                    conversationsLastLoadedAt = System.currentTimeMillis()
                    _uiState.update { state ->
                        val refreshedSelectedConversation = state.selectedConversation?.let { selected ->
                            response.conversations.firstOrNull { it.id == selected.id } ?: selected
                        }
                        state.copy(
                            conversations = response.conversations,
                            selectedConversation = refreshedSelectedConversation
                        )
                    }
                }
            loadUnreadAndRequestsCount()
        }
    }

    private suspend fun hydrateCachedConversations(cacheOwnerId: String) {
        runCatching {
            chatCacheRepository.getCachedConversations(cacheOwnerId)
        }.onSuccess { conversations ->
            if (conversations.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(conversations = conversations, error = null)
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to hydrate cached conversations", error)
        }
    }

    private suspend fun hydrateCachedMessages(
        cacheOwnerId: String,
        conversationId: String
    ): CachedMessagesSnapshot? {
        return runCatching {
            chatCacheRepository.getCachedMessagesSnapshot(cacheOwnerId, conversationId)
        }.onSuccess { snapshot ->
            if (snapshot != null) {
                messagesCacheByConversation[conversationId] = CachedConversationMessages(
                    messages = snapshot.messages,
                    nextCursor = snapshot.nextCursor,
                    hasMore = snapshot.hasMore,
                    cachedAt = snapshot.cachedAt
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "Failed to hydrate cached messages for $conversationId", error)
        }.getOrNull()
    }

    private fun cacheCurrentMessages(conversationId: String) {
        messagesCacheByConversation[conversationId] = CachedConversationMessages(
            messages = _uiState.value.messages,
            nextCursor = _uiState.value.messagesNextCursor,
            hasMore = _uiState.value.hasMoreMessages,
            cachedAt = System.currentTimeMillis()
        )
    }

    private suspend fun persistSelectedConversationSnapshot() {
        val currentUserId = ensureCurrentUserId() ?: return
        val conversation = _uiState.value.selectedConversation ?: return
        chatCacheRepository.cacheConversationMessagesSnapshot(
            cacheOwnerId = currentUserId,
            conversation = conversation,
            messages = _uiState.value.messages,
            nextCursor = _uiState.value.messagesNextCursor,
            hasMore = _uiState.value.hasMoreMessages
        )
        cacheCurrentMessages(conversation.id)
    }

    private fun loadUnreadAndRequestsCount() {
        viewModelScope.launch {
            ApiClient.getUnreadCount(context).onSuccess { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
            ApiClient.getMessageRequestsCount(context).onSuccess { count ->
                _uiState.update { it.copy(messageRequestsCount = count) }
            }
        }
    }

    private fun upsertMessage(messages: List<Message>, message: Message): List<Message> {
        return dedupeAndSortByCreatedAt(messages.filterNot { it.id == message.id } + message)
    }

    private fun scheduleTypingIndicatorClear(conversationId: String, userId: String) {
        typingIndicatorTimeoutJob?.cancel()
        typingIndicatorTimeoutJob = viewModelScope.launch {
            delay(4_000)
            _uiState.update { state ->
                if (state.selectedConversation?.id != conversationId || state.typingUserId != userId) {
                    state
                } else {
                    state.copy(typingUserId = null)
                }
            }
        }
    }

    private fun clearTypingIndicator() {
        typingIndicatorTimeoutJob?.cancel()
        typingIndicatorTimeoutJob = null
    }

    private fun stopOutgoingTyping(conversationId: String? = _uiState.value.selectedConversation?.id) {
        outgoingTypingStopJob?.cancel()
        outgoingTypingStopJob = null

        if (isOutgoingTyping && !conversationId.isNullOrBlank()) {
            ChatSocketManager.sendTyping(conversationId, false)
        }

        isOutgoingTyping = false
        outgoingTypingConversationId = null
    }

    private fun resolveThreadReadyState(
        messages: List<Message>,
        fromCache: Boolean
    ): ChatThreadReadyState {
        if (messages.isEmpty()) return ChatThreadReadyState.EMPTY_THREAD
        return if (fromCache) {
            ChatThreadReadyState.CACHED_READY
        } else {
            ChatThreadReadyState.MESSAGES_READY
        }
    }

    private fun replaceMatchingPendingMessage(messages: List<Message>, serverMessage: Message): List<Message> {
        val pending = messages.firstOrNull {
            it.id.startsWith("local-") &&
                it.senderId == serverMessage.senderId &&
                it.conversationId == serverMessage.conversationId &&
                it.content == serverMessage.content
        } ?: return messages
        return dedupeAndSortByCreatedAt(messages.filterNot { it.id == pending.id } + serverMessage)
    }

    private fun dedupeAndSortByCreatedAt(messages: List<Message>): List<Message> {
        return messages
            .groupBy { it.id }
            .map { (_, grouped) -> grouped.last() }
            .sortedBy { it.createdAt }
    }

    override fun onCleared() {
        stopOutgoingTyping()
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        clearTypingIndicator()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context.applicationContext) as T
        }
    }
}
