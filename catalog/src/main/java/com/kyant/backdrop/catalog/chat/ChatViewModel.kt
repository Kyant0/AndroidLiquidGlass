package com.kyant.backdrop.catalog.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import com.kyant.backdrop.catalog.network.models.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val TAG = "ChatViewModel"

data class ChatUiState(
    val currentUserId: String? = null,
    val conversations: List<Conversation> = emptyList(),
    val messages: List<Message> = emptyList(),
    val selectedConversation: Conversation? = null,
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
    // AI smart reply suggestions (ready for future AI backend integration)
    val aiSuggestions: List<String> = emptyList(),
    val isLoadingAiSuggestions: Boolean = false,
    // Socket connection state for UI feedback
    val socketConnected: Boolean = false,
    // Reply-to message (for swipe reply feature)
    val replyToMessage: Message? = null,
    // Attachment upload in progress
    val isUploadingAttachment: Boolean = false,
    val attachmentUploadError: String? = null,
    // Voice recording
    val isRecordingVoice: Boolean = false
)

class ChatViewModel(private val context: Context) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var loadConversationsJob: Job? = null
    private var loadMessagesJob: Job? = null
    private var hasLoadedConversations: Boolean = false
    private var conversationsLastLoadedAt: Long = 0L
    private val conversationsCacheTtlMs: Long = 90_000L

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
        Log.d(TAG, "ChatViewModel init - connecting socket...")
        viewModelScope.launch {
            val token = ApiClient.getToken(context)
            val userId = ApiClient.getCurrentUserId(context)
            Log.d(TAG, "Got token: ${token?.take(20)}..., userId: $userId")
            if (!token.isNullOrEmpty()) {
                ChatSocketManager.connect(token)
            } else {
                Log.w(TAG, "No token available, socket not connected")
            }
            _uiState.update { it.copy(currentUserId = userId) }
        }
        collectSocketEvents()
        collectConnectionState()
    }
    
    private fun collectConnectionState() {
        viewModelScope.launch {
            ChatSocketManager.connectionStateFlow.collect { state ->
                Log.d(TAG, "Socket connection state: $state")
                _uiState.update { it.copy(socketConnected = state == ChatSocketManager.ConnectionState.CONNECTED) }
            }
        }
    }

    private fun collectSocketEvents() {
        // New messages
        viewModelScope.launch {
            ChatSocketManager.newMessageFlow.collect { (conversationId, messageJson) ->
                Log.d(TAG, "📩 Received message for conversation: $conversationId")
                try {
                    val message = json.decodeFromString(Message.serializer(), messageJson)
                    Log.d(TAG, "📩 Parsed message: id=${message.id}, content=${message.content.take(30)}...")
                    
                    _uiState.update { state ->
                        val updatedMessages = upsertMessage(state.messages, message)
                        val withPendingReplacement = replaceMatchingPendingMessage(updatedMessages, message)

                        if (state.selectedConversation?.id == conversationId) {
                            val shouldMarkRead = message.senderId != state.currentUserId
                            if (shouldMarkRead) {
                                ChatSocketManager.markRead(conversationId)
                            }
                            state.copy(messages = withPendingReplacement)
                        } else {
                            state
                        }
                    }
                    // Keep list/unread counters in sync with server source of truth.
                    refreshConversations()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }
        }
        
        // Typing indicator
        viewModelScope.launch {
            ChatSocketManager.typingFlow.collect { (convId, userId, isTyping) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id != convId) return@update state
                    state.copy(typingUserId = if (isTyping) userId else null)
                }
            }
        }
        
        // Read receipts
        viewModelScope.launch {
            ChatSocketManager.messagesReadFlow.collect { (convId, _) ->
                _uiState.update { state ->
                    val updatedMessages = if (state.selectedConversation?.id == convId) {
                        state.messages.map {
                            if (it.senderId == state.currentUserId) it.copy(status = "READ") else it
                        }
                    } else {
                        state.messages
                    }
                    state.copy(messages = updatedMessages)
                }
                refreshConversations()
            }
        }
        
        // Message deleted
        viewModelScope.launch {
            ChatSocketManager.messageDeletedFlow.collect { (messageId, convId, _) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id != convId) return@update state
                    state.copy(messages = state.messages.filter { it.id != messageId })
                }
                refreshConversations()
            }
        }
        
        // Message edited
        viewModelScope.launch {
            ChatSocketManager.messageEditedFlow.collect { (messageId, convId, content) ->
                _uiState.update { state ->
                    if (state.selectedConversation?.id != convId) return@update state
                    state.copy(messages = state.messages.map { if (it.id == messageId) it.copy(content = content) else it })
                }
                refreshConversations()
            }
        }

        // Message reactions
        viewModelScope.launch {
            ChatSocketManager.reactionFlow.collect { event ->
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
                                    message.reactions + com.kyant.backdrop.catalog.network.models.MessageReaction(
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
                        message.copy(reactions = nextReactions)
                    }
                    state.copy(messages = updatedMessages)
                }
            }
        }
    }
    
    /**
     * Call this when the app resumes to ensure socket is connected.
     */
    fun ensureSocketConnected() {
        viewModelScope.launch {
            val token = ApiClient.getToken(context)
            if (!token.isNullOrEmpty()) {
                ChatSocketManager.reconnectIfNeeded()
            }
        }
    }

    fun ensureConversationsLoaded(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val hasFreshCache =
            hasLoadedConversations &&
            _uiState.value.conversations.isNotEmpty() &&
            (now - conversationsLastLoadedAt) < conversationsCacheTtlMs

        if (!forceRefresh && hasFreshCache) {
            loadUnreadAndRequestsCount()
            return
        }

        loadConversations()
    }

    fun loadConversations() {
        if (_uiState.value.isLoadingConversations) return

        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversations = true, error = null) }
            ApiClient.getConversations(context, 30, null)
                .onSuccess { res ->
                    hasLoadedConversations = true
                    conversationsLastLoadedAt = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            conversations = res.conversations,
                            isLoadingConversations = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingConversations = false, error = e.message)
                    }
                }
            loadUnreadAndRequestsCount()
        }
    }

    private fun loadUnreadAndRequestsCount() {
        viewModelScope.launch {
            ApiClient.getUnreadCount(context).onSuccess { count -> _uiState.update { it.copy(unreadCount = count) } }
            ApiClient.getMessageRequestsCount(context).onSuccess { count -> _uiState.update { it.copy(messageRequestsCount = count) } }
        }
    }

    private fun refreshConversations() {
        viewModelScope.launch {
            ApiClient.getConversations(context, 30, null).onSuccess { res ->
                hasLoadedConversations = true
                conversationsLastLoadedAt = System.currentTimeMillis()
                _uiState.update { it.copy(conversations = res.conversations) }
            }
            loadUnreadAndRequestsCount()
        }
    }

    fun selectConversation(conversation: Conversation?) {
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }

        ChatSocketManager.activeConversationId = conversation?.id
        conversation?.let { MessageNotificationManager.clearConversationNotification(context, it.id) }

        if (conversation == null) {
            _uiState.update {
                it.copy(
                    selectedConversation = null,
                    messages = emptyList(),
                    messagesNextCursor = null,
                    hasMoreMessages = false,
                    typingUserId = null,
                    error = null,
                    isLoadingMessages = false,
                    isLoadingMoreMessages = false
                )
            }
            return
        }

        val cached = messagesCacheByConversation[conversation.id]
        val cacheIsFresh = cached != null && (System.currentTimeMillis() - cached.cachedAt) < messagesCacheTtlMs

        _uiState.update {
            it.copy(
                selectedConversation = conversation,
                messages = if (cacheIsFresh) cached!!.messages else emptyList(),
                messagesNextCursor = if (cacheIsFresh) cached!!.nextCursor else null,
                hasMoreMessages = if (cacheIsFresh) cached!!.hasMore else false,
                typingUserId = null,
                error = null,
                isLoadingMessages = false,
                isLoadingMoreMessages = false
            )
        }

        conversation?.let { conv ->
            ChatSocketManager.joinChat(conv.id)
            ChatSocketManager.markRead(conv.id)
            viewModelScope.launch { ApiClient.markAsRead(context, conv.id) }

            if (!cacheIsFresh) {
                loadMessages(conv.id)
            }
        }
    }

    fun loadMessages(conversationId: String, cursor: String? = null) {
        if (_uiState.value.selectedConversation?.id != conversationId) return
        loadMessagesJob?.cancel()
        loadMessagesJob = viewModelScope.launch {
            if (cursor == null) _uiState.update { it.copy(isLoadingMessages = true, error = null) }
            else _uiState.update { it.copy(isLoadingMoreMessages = true) }
            ApiClient.getMessages(context, conversationId, 50, cursor)
                .onSuccess { res ->
                    _uiState.update { state ->
                        val newList = if (cursor == null) {
                            dedupeAndSortByCreatedAt(res.messages)
                        } else {
                            dedupeAndSortByCreatedAt(res.messages + state.messages)
                        }
                        state.copy(
                            messages = newList,
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            hasMoreMessages = res.hasMore,
                            messagesNextCursor = res.nextCursor,
                            error = null
                        )
                    }

                    messagesCacheByConversation[conversationId] = CachedConversationMessages(
                        messages = _uiState.value.messages,
                        nextCursor = _uiState.value.messagesNextCursor,
                        hasMore = _uiState.value.hasMoreMessages,
                        cachedAt = System.currentTimeMillis()
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            isLoadingMoreMessages = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun loadMoreMessages() {
        val cursor = _uiState.value.messagesNextCursor ?: return
        val convId = _uiState.value.selectedConversation?.id ?: return
        loadMessages(convId, cursor)
    }

    /**
     * Add message immediately (optimistic) and upload in background. User can keep chatting.
     */
    fun uploadAndSendMessage(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String = "",
        replyToId: String? = null
    ) {
        val conv = _uiState.value.selectedConversation ?: return
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
            conversationId = conv.id,
            senderId = currentUserId,
            receiverId = conv.otherParticipant.id,
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
                attachmentUploadError = null
            )
        }
        viewModelScope.launch {
            val resultContentType = contentType
            ApiClient.uploadChatMedia(context, fileBytes, fileName, mimeType)
                .onSuccess { upload ->
                    val content = caption.ifBlank { when (resultContentType) {
                        "image" -> "📷 Photo"
                        "video" -> "🎬 Video"
                        "audio" -> "🎤 Voice message"
                        else -> "📎 ${upload.fileName}"
                    } }
                    ApiClient.sendMessage(
                        context,
                        conv.id,
                        content,
                        resultContentType,
                        mediaUrl = upload.mediaUrl,
                        mediaType = upload.mediaType,
                        fileName = upload.fileName,
                        fileSize = upload.fileSize,
                        replyToId = replyToId
                    ).onSuccess { message ->
                        _uiState.update { state ->
                            val withoutPending = state.messages.filterNot { it.id == pendingId }
                            val merged = dedupeAndSortByCreatedAt(withoutPending + message)
                            state.copy(messages = merged, attachmentUploadError = null)
                        }
                        refreshConversations()
                    }.onFailure { e ->
                        _uiState.update { state ->
                            val withoutPending = state.messages.filterNot { it.id == pendingId }
                            state.copy(messages = withoutPending, attachmentUploadError = e.message)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        val withoutPending = state.messages.filterNot { it.id == pendingId }
                        state.copy(messages = withoutPending, attachmentUploadError = e.message)
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
                } catch (_: Exception) { }
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        val conv = _uiState.value.selectedConversation ?: return
        if (content.isBlank()) return
        val nowIso = isoFormatter.format(Date())
        val tempMessageId = "local-${System.currentTimeMillis()}"
        viewModelScope.launch {
            _uiState.update { state ->
                val optimisticMessage = Message(
                    id = tempMessageId,
                    conversationId = conv.id,
                    senderId = state.currentUserId.orEmpty(),
                    receiverId = conv.otherParticipant.id,
                    content = content,
                    contentType = "text",
                    status = "SENT",
                    createdAt = nowIso,
                    updatedAt = nowIso
                )
                state.copy(
                    messages = dedupeAndSortByCreatedAt(state.messages + optimisticMessage),
                    isSending = true,
                    error = null
                )
            }

            ApiClient.sendMessage(context, conv.id, content, "text", replyToId = replyToId)
                .onSuccess { message ->
                    _uiState.update { state ->
                        val withoutTemp = state.messages.filterNot { it.id == tempMessageId }
                        val merged = dedupeAndSortByCreatedAt(withoutTemp + message)
                        state.copy(messages = merged, isSending = false)
                    }
                    refreshConversations()
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filterNot { it.id == tempMessageId },
                            isSending = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun sendTyping(isTyping: Boolean) {
        _uiState.value.selectedConversation?.let { ChatSocketManager.sendTyping(it.id, isTyping) }
    }

    fun markAsRead() {
        _uiState.value.selectedConversation?.let { conv ->
            ChatSocketManager.markRead(conv.id)
            viewModelScope.launch { ApiClient.markAsRead(context, conv.id) }
        }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean = false) {
        viewModelScope.launch {
            ApiClient.deleteMessage(context, messageId, forEveryone).onSuccess {
                _uiState.update { state -> state.copy(messages = state.messages.filter { it.id != messageId }) }
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
                // Optimistic update - add reaction locally
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == messageId) {
                                val existingReaction = msg.reactions.find { it.userId == state.currentUserId }
                                val newReactions = if (existingReaction != null) {
                                    // Toggle or update reaction
                                    if (existingReaction.emoji == emoji) {
                                        msg.reactions.filter { it.userId != state.currentUserId }
                                    } else {
                                        msg.reactions.map { r ->
                                            if (r.userId == state.currentUserId) r.copy(id = r.id, emoji = emoji) else r
                                        }
                                    }
                                } else {
                                    msg.reactions + com.kyant.backdrop.catalog.network.models.MessageReaction(
                                        id = "local-${System.currentTimeMillis()}",
                                        userId = state.currentUserId.orEmpty(),
                                        emoji = emoji
                                    )
                                }
                                msg.copy(reactions = newReactions)
                            } else msg
                        }
                    )
                }
            }
        }
    }

    fun openChatWithUser(userId: String) {
        viewModelScope.launch {
            ApiClient.getOrCreateConversation(context, userId)
                .onSuccess { conv ->
                    selectConversation(conv)
                }
        }
    }

    fun openConversationById(conversationId: String) {
        _uiState.value.conversations.firstOrNull { it.id == conversationId }?.let { conversation ->
            selectConversation(conversation)
            return
        }

        viewModelScope.launch {
            ApiClient.getConversation(context, conversationId)
                .onSuccess { conversation ->
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(conversation) + state.conversations.filterNot { it.id == conversation.id },
                            error = null
                        )
                    }
                    selectConversation(conversation)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to open conversation $conversationId", error)
                    _uiState.update { it.copy(error = error.message ?: "Failed to open chat") }
                }
        }
    }
    
    fun clearLocalMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
    
    /**
     * Deletes the current conversation and all its messages from the database.
     * This is permanent and cannot be undone.
     */
    fun deleteCurrentConversation(onSuccess: () -> Unit = {}) {
        val conv = _uiState.value.selectedConversation ?: return
        viewModelScope.launch {
            ApiClient.deleteConversation(context, conv.id)
                .onSuccess {
                    Log.d(TAG, "Conversation ${conv.id} deleted successfully")
                    // Clear local state and go back to conversation list
                    selectConversation(null)
                    // Remove from conversations list
                    _uiState.update { state ->
                        state.copy(conversations = state.conversations.filter { it.id != conv.id })
                    }
                    onSuccess()
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to delete conversation", e)
                    _uiState.update { it.copy(error = "Failed to delete chat: ${e.message}") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Load AI-powered smart reply suggestions based on conversation context.
     * Currently uses mock data; ready for integration with AI backend (e.g., /api/ai/suggestions).
     */
    fun loadAiSuggestions() {
        val conv = _uiState.value.selectedConversation ?: return
        val lastMessage = _uiState.value.messages.lastOrNull { it.senderId != _uiState.value.currentUserId }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAiSuggestions = true) }
            
            // Mock suggestions based on context - replace with actual AI API call
            // Example: ApiClient.getAiSuggestions(context, conv.id, lastMessage?.content)
            val suggestions = generateMockSuggestions(lastMessage?.content)
            
            _uiState.update { it.copy(aiSuggestions = suggestions, isLoadingAiSuggestions = false) }
        }
    }
    
    /**
     * Clear AI suggestions (e.g., after user sends a message).
     */
    fun clearAiSuggestions() {
        _uiState.update { it.copy(aiSuggestions = emptyList()) }
    }
    
    /**
     * Use an AI suggestion as the message content.
     */
    fun useAiSuggestion(suggestion: String) {
        sendMessage(suggestion)
        clearAiSuggestions()
    }
    
    private fun generateMockSuggestions(lastMessageContent: String?): List<String> {
        // Context-aware mock suggestions - will be replaced with real AI
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

    private fun upsertMessage(messages: List<Message>, message: Message): List<Message> {
        val withoutExisting = messages.filterNot { it.id == message.id }
        return dedupeAndSortByCreatedAt(withoutExisting + message)
    }

    private fun replaceMatchingPendingMessage(messages: List<Message>, serverMessage: Message): List<Message> {
        val pending = messages.firstOrNull {
            it.id.startsWith("local-") &&
                it.senderId == serverMessage.senderId &&
                it.conversationId == serverMessage.conversationId &&
                it.content == serverMessage.content
        } ?: return messages
        val withoutPending = messages.filterNot { it.id == pending.id }
        return dedupeAndSortByCreatedAt(withoutPending + serverMessage)
    }

    private fun dedupeAndSortByCreatedAt(messages: List<Message>): List<Message> {
        return messages
            .groupBy { it.id }
            .map { (_, grouped) -> grouped.last() }
            .sortedBy { it.createdAt }
    }

    override fun onCleared() {
        _uiState.value.selectedConversation?.let { ChatSocketManager.leaveChat(it.id) }
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context.applicationContext) as T
        }
    }
}
