package com.kyant.backdrop.catalog.network

import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.AgentUiIntent
import com.kyant.backdrop.catalog.network.models.AgentVoiceTurnResponse
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URI

object AgentSocketManager {
    private const val TAG = "AgentSocket"

    private data class PendingVoiceStartRequest(
        val sessionId: String,
        val surface: String,
        val surfaceContext: Map<String, String>,
        val allowAutonomousActions: Boolean
    )

    data class AgentSocketEvent(
        val type: String,
        val sessionId: String? = null,
        val actionId: String? = null,
        val status: String? = null,
        val pendingCount: Int? = null,
        val goalsCount: Int? = null,
        val surface: String? = null,
        val message: String? = null
    )

    sealed interface AgentVoiceSocketEvent {
        data class Ready(
            val sessionId: String?,
            val model: String?
        ) : AgentVoiceSocketEvent

        data class State(
            val state: String,
            val sessionId: String?,
            val responseId: String?
        ) : AgentVoiceSocketEvent

        data class UserTranscript(
            val text: String,
            val isFinal: Boolean
        ) : AgentVoiceSocketEvent

        data class AssistantTranscript(
            val text: String,
            val isFinal: Boolean
        ) : AgentVoiceSocketEvent

        data class AudioDelta(
            val responseId: String?,
            val audioBase64: String,
            val audioMimeType: String?
        ) : AgentVoiceSocketEvent

        data class AudioDone(
            val sessionId: String?,
            val responseId: String?
        ) : AgentVoiceSocketEvent

        data class TurnFinal(
            val response: AgentVoiceTurnResponse
        ) : AgentVoiceSocketEvent

        data class Error(
            val message: String
        ) : AgentVoiceSocketEvent
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val socketUrl: String
        get() = BuildConfig.SOCKET_BASE_URL

    private var socket: Socket? = null
    private var currentToken: String? = null
    private var currentSessionId: String? = null
    private var isConnecting = false
    private var pendingVoiceStartRequest: PendingVoiceStartRequest? = null

    private val _events = MutableSharedFlow<AgentSocketEvent>(replay = 0, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _voiceEvents = MutableSharedFlow<AgentVoiceSocketEvent>(replay = 0, extraBufferCapacity = 2048)
    val voiceEvents = _voiceEvents.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1, extraBufferCapacity = 1)
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()

    fun connect(token: String, sessionId: String? = null) {
        currentSessionId = sessionId
        if (socket?.connected() == true && currentToken == token) {
            joinCurrentSession()
            flushPendingVoiceStart()
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
                    flushPendingVoiceStart()
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
                    flushPendingVoiceStart()
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
                                surface = payload.optString("surface").takeIf { it.isNotBlank() },
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
                on("agent:navigation_preview") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _events.tryEmit(
                            AgentSocketEvent(
                                type = "navigation_preview",
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                surface = payload.optString("surface").takeIf { it.isNotBlank() },
                                message = payload.optString("message").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                on("agent:voice_ready") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _voiceEvents.tryEmit(
                            AgentVoiceSocketEvent.Ready(
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                model = payload.optString("model").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                on("agent:voice_state") { args ->
                    parseJsonObject(args)?.let { payload ->
                        val state = payload.optString("state")
                        if (state.isNotBlank()) {
                            _voiceEvents.tryEmit(
                                AgentVoiceSocketEvent.State(
                                    state = state,
                                    sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                    responseId = payload.optString("responseId").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                }
                on("agent:voice_user_transcript") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _voiceEvents.tryEmit(
                            AgentVoiceSocketEvent.UserTranscript(
                                text = payload.optString("text"),
                                isFinal = payload.optBoolean("isFinal", false)
                            )
                        )
                    }
                }
                on("agent:voice_assistant_transcript") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _voiceEvents.tryEmit(
                            AgentVoiceSocketEvent.AssistantTranscript(
                                text = payload.optString("text"),
                                isFinal = payload.optBoolean("isFinal", false)
                            )
                        )
                    }
                }
                on("agent:voice_audio_delta") { args ->
                    parseJsonObject(args)?.let { payload ->
                        val audioBase64 = payload.optString("audioBase64")
                        if (audioBase64.isNotBlank()) {
                            _voiceEvents.tryEmit(
                                AgentVoiceSocketEvent.AudioDelta(
                                    responseId = payload.optString("responseId").takeIf { it.isNotBlank() },
                                    audioBase64 = audioBase64,
                                    audioMimeType = payload.optString("audioMimeType").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                }
                on("agent:voice_audio_done") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _voiceEvents.tryEmit(
                            AgentVoiceSocketEvent.AudioDone(
                                sessionId = payload.optString("sessionId").takeIf { it.isNotBlank() },
                                responseId = payload.optString("responseId").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
                on("agent:voice_turn_final") { args ->
                    parseJsonObject(args)?.let { payload ->
                        runCatching {
                            json.decodeFromString<AgentVoiceTurnResponse>(payload.toString())
                        }.onSuccess { response ->
                            _voiceEvents.tryEmit(AgentVoiceSocketEvent.TurnFinal(response))
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to decode final realtime voice turn", error)
                        }
                    }
                }
                on("agent:voice_error") { args ->
                    parseJsonObject(args)?.let { payload ->
                        _voiceEvents.tryEmit(
                            AgentVoiceSocketEvent.Error(
                                message = payload.optString("error").ifBlank {
                                    "Realtime voice is unavailable right now."
                                }
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
        pendingVoiceStartRequest = null
        _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
    }

    private fun joinCurrentSession() {
        val sessionId = currentSessionId
        if (socket?.connected() == true && !sessionId.isNullOrBlank()) {
            socket?.emit("agent:join_session", JSONObject().put("sessionId", sessionId))
        }
    }

    fun startRealtimeVoice(
        sessionId: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean
    ) {
        if (sessionId.isBlank()) {
            return
        }

        val request = PendingVoiceStartRequest(
            sessionId = sessionId,
            surface = surface,
            surfaceContext = surfaceContext,
            allowAutonomousActions = allowAutonomousActions
        )
        updateSession(sessionId)
        if (socket?.connected() == true) {
            pendingVoiceStartRequest = null
            emitVoiceStart(request)
            return
        }

        pendingVoiceStartRequest = request
        if (!isConnecting) {
            socket?.connect()
        }
    }

    fun updateSurface(
        sessionId: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean? = null
    ) {
        if (sessionId.isBlank() || socket?.connected() != true) {
            return
        }

        val contextJson = JSONObject()
        surfaceContext.forEach { (key, value) ->
            contextJson.put(key, value)
        }

        val payload = JSONObject()
            .put("sessionId", sessionId)
            .put("surface", surface)
            .put("surfaceContext", contextJson)

        if (allowAutonomousActions != null) {
            payload.put("allowAutonomousActions", allowAutonomousActions)
        }

        socket?.emit("agent:surface_update", payload)
    }

    fun sendRealtimeAudioChunk(audioBase64: String) {
        if (audioBase64.isBlank() || socket?.connected() != true) {
            return
        }

        socket?.emit(
            "agent:voice_audio_chunk",
            JSONObject().put("audioBase64", audioBase64)
        )
    }

    fun interruptRealtimeVoice() {
        if (socket?.connected() != true) {
            return
        }
        socket?.emit("agent:voice_interrupt")
    }

    fun requestRealtimeVoicePrompt(instructions: String) {
        if (socket?.connected() != true || instructions.isBlank()) {
            return
        }
        socket?.emit(
            "agent:voice_prompt",
            JSONObject().put("instructions", instructions)
        )
    }

    fun stopRealtimeVoice() {
        pendingVoiceStartRequest = null
        if (socket?.connected() != true) {
            return
        }
        socket?.emit("agent:voice_stop")
    }

    private fun flushPendingVoiceStart() {
        val request = pendingVoiceStartRequest ?: return
        if (socket?.connected() != true) {
            return
        }
        pendingVoiceStartRequest = null
        emitVoiceStart(request)
    }

    private fun emitVoiceStart(request: PendingVoiceStartRequest) {
        val contextJson = JSONObject()
        request.surfaceContext.forEach { (key, value) ->
            contextJson.put(key, value)
        }

        socket?.emit(
            "agent:voice_start",
            JSONObject()
                .put("sessionId", request.sessionId)
                .put("surface", request.surface)
                .put("surfaceContext", contextJson)
                .put("allowAutonomousActions", request.allowAutonomousActions)
        )
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
