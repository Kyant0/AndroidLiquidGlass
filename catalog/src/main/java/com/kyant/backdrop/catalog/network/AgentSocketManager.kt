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

object AgentSocketManager {
    private const val TAG = "AgentSocket"

    data class AgentSocketEvent(
        val type: String,
        val sessionId: String? = null,
        val actionId: String? = null,
        val status: String? = null,
        val pendingCount: Int? = null,
        val goalsCount: Int? = null
    )

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    private val socketUrl: String
        get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var currentSessionId: String? = null
    private var isConnecting = false

    private val _events = MutableSharedFlow<AgentSocketEvent>(replay = 0, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()

    fun connect(token: String, sessionId: String? = null) {
        currentSessionId = sessionId
        if (socket?.connected() == true && currentToken == token) {
            joinCurrentSession()
            return
        }
        if (isConnecting && currentToken == token) {
            return
        }

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

            socket = IO.socket(URI.create(socketUrl), opts).apply {
                on(Socket.EVENT_CONNECT) {
                    isConnecting = false
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    joinCurrentSession()
                }
                on(Socket.EVENT_DISCONNECT) {
                    _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    isConnecting = false
                    Log.e(TAG, "Connect error: ${args.getOrNull(0)}")
                    _connectionStateFlow.tryEmit(ConnectionState.ERROR)
                }
                on("reconnect") {
                    _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
                    joinCurrentSession()
                }
                on("agent:pending_action_created") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "pending_action_created",
                                sessionId = payload.optJSONObject("action")?.optString("sessionId")?.takeIf { it.isNotBlank() },
                                actionId = payload.optJSONObject("action")?.optString("id")?.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                on("agent:pending_action_resolved") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "pending_action_resolved",
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                actionId = payload.optString("actionId").takeIf { it.isNotBlank() },
                                status = payload.optString("status").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                on("agent:pending_actions_changed") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "pending_actions_changed",
                                pendingCount = payload.optJSONArray("actions")?.length()
                            )
                        )
                    }
                }
                on("agent:goals_changed") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "goals_changed",
                                goalsCount = payload.optJSONArray("goals")?.length()
                            )
                        )
                    }
                }
                on("agent:turn_completed") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "turn_completed",
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                pendingCount = payload.optJSONArray("pendingActions")?.length(),
                                goalsCount = payload.optJSONArray("goals")?.length()
                            )
                        )
                    }
                }
                on("agent:approval_executed") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "approval_executed",
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                actionId = payload.optString("actionId").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            }

            socket?.connect()
        } catch (error: Exception) {
            isConnecting = false
            Log.e(TAG, "Socket init error", error)
            _connectionStateFlow.tryEmit(ConnectionState.ERROR)
        }
    }

    fun updateSession(sessionId: String?) {
        val previousSessionId = currentSessionId
        currentSessionId = sessionId
        if (socket?.connected() != true) {
            return
        }
        if (!previousSessionId.isNullOrBlank() && previousSessionId != sessionId) {
            socket?.emit("agent:leave_session", JSONObject().put("sessionId", previousSessionId))
        }
        joinCurrentSession()
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        isConnecting = false
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }

    private fun joinCurrentSession() {
        val sessionId = currentSessionId
        if (socket?.connected() == true && !sessionId.isNullOrBlank()) {
            socket?.emit("agent:join_session", JSONObject().put("sessionId", sessionId))
        }
    }

    private fun parseJsonObject(args: Array<Any>): JSONObject? {
        if (args.isEmpty()) return null
        return try {
            when (val raw = args[0]) {
                is JSONObject -> raw
                else -> JSONObject(raw.toString())
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to parse agent socket payload", error)
            null
        }
    }
}
