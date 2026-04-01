package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.net.URI

/**
 * Manages Socket.IO connection for real-time chat with vormex-backend.
 * 
 * Backend expects: handshake.auth.token (JWT), and events:
 * - chat:join, chat:leave (room management)
 * - chat:send_message, chat:new_message, chat:notification  
 * - chat:typing, chat:user_typing
 * - chat:mark_read, chat:messages_read
 * - chat:delete_message, chat:message_deleted
 * - chat:edit_message, chat:message_edited
 * - chat:message_reaction
 * 
 * Configuration:
 * - For local dev with `adb reverse tcp:5000 tcp:5000`: use "http://localhost:5000"
 * - For Android emulator: use "http://10.0.2.2:5000"
 * - For production: use your production WebSocket URL (e.g., "https://api.vormex.com")
 */
object ChatSocketManager {
    private const val TAG = "ChatSocket"
    private const val RECENT_MESSAGE_DEDUPE_LIMIT = 200

    private val SOCKET_URL: String
        get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var isConnecting = false
    var currentUserId: String? = null
    
    // Track joined rooms to re-join on reconnect
    private val joinedRooms = mutableSetOf<String>()
    private val recentlyProcessedMessageIds = ArrayDeque<String>()
    private val recentMessageIdSet = mutableSetOf<String>()
    
    // Notification callback for showing local notifications
    private var notificationCallback: ((title: String, body: String, data: Map<String, String>) -> Unit)? = null
    
    // Track current active conversation to avoid notifying for messages the user is viewing
    var activeConversationId: String? = null

    // Use MutableSharedFlow with replay to ensure events aren't lost
    // Replay=1 ensures late collectors get the last event
    
    /** Emits (conversationId, messageJsonString) for new messages */
    private val _newMessageFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 10)
    val newMessageFlow = _newMessageFlow.asSharedFlow()

    private val _typingFlow = MutableSharedFlow<Triple<String, String, Boolean>>(replay = 0, extraBufferCapacity = 5)
    val typingFlow = _typingFlow.asSharedFlow()

    private val _messagesReadFlow = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 5)
    val messagesReadFlow = _messagesReadFlow.asSharedFlow()
    
    /**
     * Set callback for showing local notifications
     */
    fun setNotificationCallback(callback: (title: String, body: String, data: Map<String, String>) -> Unit) {
        notificationCallback = callback
    }

    private val _messageDeletedFlow = MutableSharedFlow<Triple<String, String, Boolean>>(replay = 0, extraBufferCapacity = 5)
    val messageDeletedFlow = _messageDeletedFlow.asSharedFlow()

    private val _messageEditedFlow = MutableSharedFlow<Triple<String, String, String>>(replay = 0, extraBufferCapacity = 5)
    val messageEditedFlow = _messageEditedFlow.asSharedFlow()

    private val _reactionFlow = MutableSharedFlow<ReactionEvent>(replay = 0, extraBufferCapacity = 5)
    val reactionFlow = _reactionFlow.asSharedFlow()
    
    /** Connection state flow for UI feedback */
    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    data class ReactionEvent(
        val messageId: String,
        val conversationId: String,
        val userId: String,
        val emoji: String,
        val action: String
    )

    fun isConnected(): Boolean = socket?.connected() == true
    
    fun getConnectionState(): ConnectionState = when {
        socket?.connected() == true -> ConnectionState.CONNECTED
        isConnecting -> ConnectionState.CONNECTING
        else -> ConnectionState.DISCONNECTED
    }

    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) {
            Log.d(TAG, "Already connected with same token, skipping")
            return
        }
        if (isConnecting && currentToken == token) {
            Log.d(TAG, "Already connecting with same token, skipping")
            return
        }
        
        Log.d(TAG, "Connecting to socket at $SOCKET_URL")
        disconnect()
        currentToken = token
        isConnecting = true
        _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
        
        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                transports = arrayOf(WebSocket.NAME, "polling")
                auth = mapOf("token" to token)
            }
            socket = IO.socket(URI.create(SOCKET_URL), opts).apply {
                on(Socket.EVENT_CONNECT) {
                    isConnecting = false
                    Log.d(TAG, "✅ Socket connected! ID: ${id()}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    // Re-join all tracked rooms on connect
                    rejoinAllRooms()
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    Log.d(TAG, "❌ Socket disconnected: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnecting = false
                    val error = args.getOrNull(0)?.toString() ?: "Unknown error"
                    Log.e(TAG, "🔴 Socket connect error: $error")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                on("reconnect") { args ->
                    Log.d(TAG, "🔄 Socket reconnected: attempt ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    // Re-join all tracked rooms on reconnect
                    rejoinAllRooms()
                }
                on("reconnect_attempt") { args ->
                    Log.d(TAG, "🔄 Socket reconnecting... attempt ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                on("reconnect_failed") {
                    Log.e(TAG, "🔴 Socket reconnection failed after all attempts")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                
                // Chat events
                on("chat:new_message") { args ->
                    Log.d(TAG, "📩 Received chat:new_message")
                    handleNewMessage(args)
                }
                on("chat:notification") { args ->
                    Log.d(TAG, "🔔 Received chat:notification")
                    handleNotification(args)
                }
                on("chat:user_typing") { args ->
                    handleTyping(args)
                }
                on("chat:messages_read") { args ->
                    handleMessagesRead(args)
                }
                on("chat:message_deleted") { args ->
                    handleMessageDeleted(args)
                }
                on("chat:message_edited") { args ->
                    handleMessageEdited(args)
                }
                on("chat:message_reaction") { args ->
                    handleReaction(args)
                }
                
                // Debug: catch all events to see what's being received
                onAnyIncoming { args ->
                    Log.d(TAG, "🔵 ANY EVENT: ${args.contentToString()}")
                }
            }
            socket?.connect()
            Log.d(TAG, "Socket.connect() called")
        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, "Socket init error", e)
            _connectionStateFlow.tryEmit(ConnectionState.ERROR)
        }
    }
    
    private fun handleNewMessage(args: Array<Any>) {
        Log.d(TAG, "📩 handleNewMessage called with ${args.size} args")
        if (args.isEmpty()) {
            Log.w(TAG, "📩 handleNewMessage: args is empty!")
            return
        }
        try {
            val rawArg = args[0]
            Log.d(TAG, "📩 Raw arg type: ${rawArg::class.java.simpleName}, value: $rawArg")
            val obj = rawArg as? JSONObject
            if (obj == null) {
                Log.w(TAG, "📩 Could not cast to JSONObject, trying toString...")
                // Try parsing as string
                val jsonStr = rawArg.toString()
                val parsed = JSONObject(jsonStr)
                val conversationId = parsed.optString("conversationId")
                val message = parsed.optJSONObject("message")
                if (message != null && conversationId.isNotEmpty()) {
                    emitIncomingMessage(conversationId, message, "string")
                }
                return
            }
            val conversationId = obj.optString("conversationId")
            val message = obj.optJSONObject("message")
            if (message == null) {
                Log.w(TAG, "📩 message object is null in: $obj")
                return
            }
            Log.d(TAG, "📩 New message in conversation $conversationId: ${message.optString("content").take(50)}")
            if (conversationId.isNotEmpty()) {
                emitIncomingMessage(conversationId, message, "event")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new message", e)
        }
    }

    private fun emitIncomingMessage(conversationId: String, message: JSONObject, source: String) {
        val messageId = message.optString("id")
        if (messageId.isNotBlank() && !rememberIncomingMessageId(messageId)) {
            Log.d(TAG, "📩 Ignoring duplicate incoming message $messageId from $source")
            return
        }

        val emitted = _newMessageFlow.tryEmit(conversationId to message.toString())
        Log.d(TAG, "📩 Flow emit result ($source): $emitted")
        showMessageNotificationIfNeeded(conversationId, message)
    }

    private fun rememberIncomingMessageId(messageId: String): Boolean {
        synchronized(recentMessageIdSet) {
            if (!recentMessageIdSet.add(messageId)) {
                return false
            }

            recentlyProcessedMessageIds.addLast(messageId)
            while (recentlyProcessedMessageIds.size > RECENT_MESSAGE_DEDUPE_LIMIT) {
                val removed = recentlyProcessedMessageIds.removeFirst()
                recentMessageIdSet.remove(removed)
            }

            return true
        }
    }
    
    private fun showMessageNotificationIfNeeded(conversationId: String, message: JSONObject) {
        if (activeConversationId == conversationId) {
            Log.d(TAG, "🔕 Skipping notification - user is viewing conversation $conversationId")
            return
        }

        val sender = message.optJSONObject("sender")
        val senderName = sender?.optString("name")?.takeIf { it.isNotBlank() }
            ?: sender?.optString("username")?.takeIf { it.isNotBlank() }
            ?: "Someone"
        val senderId = sender?.optString("id")?.takeIf { it.isNotBlank() }
            ?: message.optString("senderId")
        if (!currentUserId.isNullOrBlank() && senderId == currentUserId) {
            Log.d(TAG, "🔕 Skipping notification for self-sent message $conversationId")
            return
        }
        val senderImage = sender?.optString("profileImage")?.takeIf { it.isNotBlank() } ?: ""

        val contentType = message.optString("contentType", "text")
        val displayContent = when (contentType) {
            "image" -> "\uD83D\uDCF7 Sent a photo"
            "video" -> "\uD83C\uDFA5 Sent a video"
            "file" -> "\uD83D\uDCCE Sent a file"
            "audio" -> "\uD83C\uDFB5 Sent a voice message"
            else -> message.optString("content", "Sent you a message")
        }

        Log.d(TAG, "🔔 Showing notification for message from $senderName (image: ${senderImage.take(30)})")

        notificationCallback?.invoke(
            senderName,
            displayContent,
            mapOf(
                "type" to "message",
                "conversationId" to conversationId,
                "user_id" to senderId,
                "senderImage" to senderImage
            )
        )
    }
    
    private fun handleNotification(args: Array<Any>) {
        Log.d(TAG, "🔔 handleNotification called with ${args.size} args")
        if (args.isEmpty()) {
            Log.w(TAG, "🔔 handleNotification: args is empty!")
            return
        }
        try {
            val rawArg = args[0]
            Log.d(TAG, "🔔 Raw arg type: ${rawArg::class.java.simpleName}")
            val obj = rawArg as? JSONObject ?: JSONObject(rawArg.toString())

            Log.d(TAG, "🔔 Notification type: ${obj.optString("type")}")
            if (obj.optString("type") == "new_message") {
                val conversationId = obj.optString("conversationId")
                val message = obj.optJSONObject("message")
                if (message == null) {
                    Log.w(TAG, "🔔 message is null in notification")
                    return
                }
                Log.d(TAG, "💬 Message notification for conversation $conversationId")
                if (conversationId.isNotEmpty()) {
                    // Merge top-level sender info into message if message.sender is missing
                    if (!message.has("sender") || message.isNull("sender")) {
                        obj.optJSONObject("sender")?.let { message.put("sender", it) }
                    }
                    emitIncomingMessage(conversationId, message, "notification")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    private fun handleTyping(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: return
            _typingFlow.tryEmit(
                Triple(
                    obj.optString("conversationId"),
                    obj.optString("userId"),
                    obj.optBoolean("isTyping")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling typing", e)
        }
    }
    
    private fun handleMessagesRead(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: return
            _messagesReadFlow.tryEmit(
                obj.optString("conversationId") to obj.optString("readBy")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling messages read", e)
        }
    }
    
    private fun handleMessageDeleted(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: return
            _messageDeletedFlow.tryEmit(
                Triple(
                    obj.optString("messageId"),
                    obj.optString("conversationId"),
                    obj.optBoolean("forEveryone")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message deleted", e)
        }
    }
    
    private fun handleMessageEdited(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: return
            _messageEditedFlow.tryEmit(
                Triple(
                    obj.optString("messageId"),
                    obj.optString("conversationId"),
                    obj.optString("content")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message edited", e)
        }
    }
    
    private fun handleReaction(args: Array<Any>) {
        if (args.isEmpty()) return
        try {
            val obj = args[0] as? JSONObject ?: return
            _reactionFlow.tryEmit(
                ReactionEvent(
                    messageId = obj.optString("messageId"),
                    conversationId = obj.optString("conversationId"),
                    userId = obj.optString("userId"),
                    emoji = obj.optString("emoji"),
                    action = obj.optString("action")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling reaction", e)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting socket")
        socket?.off()
        socket?.disconnect()
        socket = null
        currentToken = null
        isConnecting = false
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }
    
    /** Reconnect with the stored token (call on app resume) */
    fun reconnectIfNeeded() {
        val token = currentToken
        if (token != null && socket?.connected() != true && !isConnecting) {
            Log.d(TAG, "Reconnecting socket...")
            connect(token)
        }
    }

    fun joinChat(conversationId: String) {
        Log.d(TAG, "➡️ Joining chat room: $conversationId, socket connected: ${socket?.connected()}")
        joinedRooms.add(conversationId)
        if (socket?.connected() == true) {
            socket?.emit("chat:join", JSONObject().put("conversationId", conversationId))
            Log.d(TAG, "➡️ Emitted chat:join for room: $conversationId")
        } else {
            Log.d(TAG, "➡️ Socket not connected, room $conversationId will be joined on connect")
        }
    }

    fun leaveChat(conversationId: String) {
        Log.d(TAG, "⬅️ Leaving chat room: $conversationId")
        joinedRooms.remove(conversationId)
        if (socket?.connected() == true) {
            socket?.emit("chat:leave", JSONObject().put("conversationId", conversationId))
        }
    }
    
    private fun rejoinAllRooms() {
        if (joinedRooms.isNotEmpty()) {
            Log.d(TAG, "🔄 Re-joining ${joinedRooms.size} rooms: $joinedRooms")
            joinedRooms.forEach { conversationId ->
                socket?.emit("chat:join", JSONObject().put("conversationId", conversationId))
                Log.d(TAG, "🔄 Re-joined room: $conversationId")
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        contentType: String = "text",
        replyToId: String? = null
    ) {
        val obj = JSONObject()
            .put("conversationId", conversationId)
            .put("content", content)
            .put("contentType", contentType)
        replyToId?.let { obj.put("replyToId", it) }
        Log.d(TAG, "📤 Sending message via socket to $conversationId")
        socket?.emit("chat:send_message", obj)
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        socket?.emit("chat:typing", JSONObject().put("conversationId", conversationId).put("isTyping", isTyping))
    }

    fun markRead(conversationId: String) {
        socket?.emit("chat:mark_read", JSONObject().put("conversationId", conversationId))
    }
}
