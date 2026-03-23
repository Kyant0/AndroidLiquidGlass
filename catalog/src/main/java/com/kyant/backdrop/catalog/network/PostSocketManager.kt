package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.FullComment
import com.kyant.backdrop.catalog.network.models.PollOption
import com.kyant.backdrop.catalog.network.models.Post
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URI

/**
 * Manages Socket.IO connection for real-time feed/post updates.
 */
object PostSocketManager {
    private const val TAG = "PostSocket"

    private val SOCKET_URL: String
        get() = BuildConfig.SOCKET_BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var isConnecting = false
    private val joinedPosts = mutableSetOf<String>()

    private val _postCreatedFlow = MutableSharedFlow<Post>(replay = 0, extraBufferCapacity = 10)
    val postCreatedFlow = _postCreatedFlow.asSharedFlow()

    private val _postLikedFlow = MutableSharedFlow<PostLikedEvent>(replay = 0, extraBufferCapacity = 20)
    val postLikedFlow = _postLikedFlow.asSharedFlow()

    private val _postSharedFlow = MutableSharedFlow<PostSharedEvent>(replay = 0, extraBufferCapacity = 10)
    val postSharedFlow = _postSharedFlow.asSharedFlow()

    private val _commentCreatedFlow = MutableSharedFlow<CommentCreatedEvent>(replay = 0, extraBufferCapacity = 20)
    val commentCreatedFlow = _commentCreatedFlow.asSharedFlow()

    private val _commentLikedFlow = MutableSharedFlow<CommentLikedEvent>(replay = 0, extraBufferCapacity = 20)
    val commentLikedFlow = _commentLikedFlow.asSharedFlow()

    private val _commentDeletedFlow = MutableSharedFlow<CommentDeletedEvent>(replay = 0, extraBufferCapacity = 10)
    val commentDeletedFlow = _commentDeletedFlow.asSharedFlow()

    private val _pollUpdatedFlow = MutableSharedFlow<PollUpdatedEvent>(replay = 0, extraBufferCapacity = 10)
    val pollUpdatedFlow = _pollUpdatedFlow.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    @Serializable
    private data class PostCreatedPayload(
        val post: Post
    )

    @Serializable
    data class PostLikedEvent(
        val postId: String,
        val userId: String,
        val liked: Boolean,
        val likesCount: Int
    )

    @Serializable
    data class PostSharedEvent(
        val postId: String,
        val userId: String? = null,
        val sharesCount: Int
    )

    @Serializable
    data class CommentCreatedEvent(
        val postId: String,
        val comment: FullComment? = null,
        val commentsCount: Int = 0
    )

    @Serializable
    data class CommentLikedEvent(
        val commentId: String,
        val postId: String,
        val userId: String,
        val liked: Boolean,
        val likesCount: Int
    )

    @Serializable
    data class CommentDeletedEvent(
        val postId: String,
        val commentId: String,
        val commentsCount: Int = 0
    )

    @Serializable
    data class PollUpdatedEvent(
        val postId: String,
        val pollOptions: List<PollOption> = emptyList(),
        val voterId: String? = null,
        val votedOptionId: String? = null
    )

    fun isConnected(): Boolean = socket?.connected() == true

    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) {
            return
        }
        if (isConnecting && currentToken == token) {
            return
        }

        disconnect(clearJoinedPosts = false)
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
                    Log.d(TAG, "Connected: ${id()}")
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    rejoinTrackedPosts()
                }
                on(Socket.EVENT_DISCONNECT) { args ->
                    Log.d(TAG, "Disconnected: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnecting = false
                    Log.e(TAG, "Connect error: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                on("reconnect") {
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    rejoinTrackedPosts()
                }
                on("reconnect_attempt") {
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
                }
                on("reconnect_failed") {
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }

                on("post:created") { args ->
                    decode<PostCreatedPayload>(args)?.post?.let { _postCreatedFlow.tryEmit(it) }
                }
                on("post:liked") { args ->
                    decode<PostLikedEvent>(args)?.let { _postLikedFlow.tryEmit(it) }
                }
                on("post:shared") { args ->
                    decode<PostSharedEvent>(args)?.let { _postSharedFlow.tryEmit(it) }
                }
                on("comment:created") { args ->
                    decode<CommentCreatedEvent>(args)?.let { _commentCreatedFlow.tryEmit(it) }
                }
                on("comment:liked") { args ->
                    decode<CommentLikedEvent>(args)?.let { _commentLikedFlow.tryEmit(it) }
                }
                on("comment:deleted") { args ->
                    decode<CommentDeletedEvent>(args)?.let { _commentDeletedFlow.tryEmit(it) }
                }
                on("poll:updated") { args ->
                    decode<PollUpdatedEvent>(args)?.let { _pollUpdatedFlow.tryEmit(it) }
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, "Socket init error", e)
            _connectionStateFlow.tryEmit(ConnectionState.ERROR)
        }
    }

    fun disconnect(clearJoinedPosts: Boolean = true) {
        socket?.off()
        socket?.disconnect()
        socket = null
        currentToken = null
        isConnecting = false
        if (clearJoinedPosts) {
            joinedPosts.clear()
        }
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }

    fun joinPost(postId: String) {
        if (postId.isBlank()) return
        joinedPosts.add(postId)
        socket?.emit("post:join", JSONObject().put("postId", postId))
    }

    fun leavePost(postId: String) {
        if (postId.isBlank()) return
        joinedPosts.remove(postId)
        socket?.emit("post:leave", JSONObject().put("postId", postId))
    }

    private fun rejoinTrackedPosts() {
        joinedPosts.forEach { postId ->
            socket?.emit("post:join", JSONObject().put("postId", postId))
        }
    }

    private inline fun <reified T> decode(args: Array<Any>): T? {
        if (args.isEmpty()) return null
        return try {
            json.decodeFromString<T>(args[0].toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode ${T::class.java.simpleName}: ${args.getOrNull(0)}", e)
            null
        }
    }
}
