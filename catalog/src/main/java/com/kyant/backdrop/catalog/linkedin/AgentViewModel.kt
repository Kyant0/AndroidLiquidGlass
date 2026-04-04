package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.AgentApiService
import com.kyant.backdrop.catalog.network.AgentSocketManager
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.AgentAction
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentPendingAction
import com.kyant.backdrop.catalog.network.models.AgentSessionState
import com.kyant.backdrop.catalog.network.models.AgentTurnResponse
import com.kyant.backdrop.catalog.network.models.AgentUiIntent
import com.kyant.backdrop.catalog.network.models.AgentVoiceTurnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AgentMessage(
    val role: String,
    val content: String
)

data class AgentUiState(
    val sessionState: AgentSessionState? = null,
    val messages: List<AgentMessage> = emptyList(),
    val isLoadingSession: Boolean = false,
    val isSending: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val isRefreshingMeta: Boolean = false,
    val isSavingGoal: Boolean = false,
    val isResolvingApproval: Boolean = false,
    val autoRunEnabled: Boolean = false,
    val socketConnected: Boolean = false,
    val error: String? = null,
    val pendingUiIntents: List<AgentUiIntent> = emptyList(),
    val lastExecutedActions: List<AgentAction> = emptyList(),
    val lastSuggestedActions: List<AgentAction> = emptyList(),
    val pendingApprovals: List<AgentPendingAction> = emptyList(),
    val goals: List<AgentGoal> = emptyList()
)

class AgentViewModel(
    private val context: Context
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var voiceRecordingFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        observeSocket()
        viewModelScope.launch {
            val storedAutoRun = AgentApiService.getStoredAutoRunEnabled(applicationContext)
            _uiState.update { it.copy(autoRunEnabled = storedAutoRun) }
            connectSocketIfPossible(null)
            refreshPendingActions(silent = true)
            refreshGoals(silent = true)
        }
    }

    fun ensureSession(surface: String) {
        viewModelScope.launch {
            val autoRunEnabled = AgentApiService.getStoredAutoRunEnabled(applicationContext)
            _uiState.update { state ->
                state.copy(
                    autoRunEnabled = autoRunEnabled,
                    sessionState = state.sessionState?.copy(allowAutonomousActions = autoRunEnabled)
                )
            }

            val existingSession = _uiState.value.sessionState
            if (existingSession != null) {
                connectSocketIfPossible(existingSession.sessionId)
                refreshPendingActions(silent = true)
                refreshGoals(silent = true)
                return@launch
            }

            if (_uiState.value.isLoadingSession) {
                return@launch
            }

            _uiState.update { it.copy(isLoadingSession = true, error = null) }
            AgentApiService.bootstrapSession(
                context = applicationContext,
                mode = "text",
                surface = surface,
                allowAutonomousActions = autoRunEnabled
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        sessionState = response.sessionState
                    )
                }
                connectSocketIfPossible(response.sessionState.sessionId)
                refreshPendingActions(silent = true)
                refreshGoals(silent = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        error = error.message ?: "Could not initialize the agent."
                    )
                }
            }
        }
    }

    fun setAutoRunEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AgentApiService.setStoredAutoRunEnabled(applicationContext, enabled)
            _uiState.update { state ->
                state.copy(
                    autoRunEnabled = enabled,
                    sessionState = state.sessionState?.copy(allowAutonomousActions = enabled)
                )
            }
        }
    }

    fun refreshPendingActions(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isRefreshingMeta = true, error = null) }
            }
            AgentApiService.getPendingActions(applicationContext)
                .onSuccess { actions ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            pendingApprovals = actions
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            error = if (silent) it.error else error.message ?: "Could not refresh approvals."
                        )
                    }
                }
        }
    }

    fun refreshGoals(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isRefreshingMeta = true, error = null) }
            }
            AgentApiService.getGoals(applicationContext)
                .onSuccess { goals ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            goals = goals
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            error = if (silent) it.error else error.message ?: "Could not refresh goals."
                        )
                    }
                }
        }
    }

    fun createGoal(goal: String, category: String? = null, priority: Int? = null) {
        val trimmedGoal = goal.trim()
        if (trimmedGoal.isEmpty() || _uiState.value.isSavingGoal) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, error = null) }
            AgentApiService.createGoal(
                context = applicationContext,
                goal = trimmedGoal,
                category = category,
                priority = priority
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isSavingGoal = false,
                        goals = response.goals
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingGoal = false,
                        error = error.message ?: "Could not save goal."
                    )
                }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        if (goalId.isBlank() || _uiState.value.isSavingGoal) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, error = null) }
            AgentApiService.deleteGoal(applicationContext, goalId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isSavingGoal = false,
                            goals = response.goals
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingGoal = false,
                            error = error.message ?: "Could not delete goal."
                        )
                    }
                }
        }
    }

    fun approvePendingAction(actionId: String) {
        if (actionId.isBlank() || _uiState.value.isResolvingApproval) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingApproval = true, error = null) }
            AgentApiService.approvePendingAction(applicationContext, actionId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            pendingApprovals = response.pendingActions,
                            pendingUiIntents = response.uiIntents,
                            lastExecutedActions = response.executedAction?.let(::listOf) ?: it.lastExecutedActions,
                            messages = response.assistantMessage?.takeIf(String::isNotBlank)?.let { assistantMessage ->
                                it.messages + AgentMessage(role = "assistant", content = assistantMessage)
                            } ?: it.messages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            error = error.message ?: "Could not approve action."
                        )
                    }
                }
        }
    }

    fun rejectPendingAction(actionId: String) {
        if (actionId.isBlank() || _uiState.value.isResolvingApproval) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingApproval = true, error = null) }
            AgentApiService.rejectPendingAction(applicationContext, actionId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            pendingApprovals = response.pendingActions,
                            messages = response.assistantMessage?.takeIf(String::isNotBlank)?.let { assistantMessage ->
                                it.messages + AgentMessage(role = "assistant", content = assistantMessage)
                            } ?: it.messages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            error = error.message ?: "Could not reject action."
                        )
                    }
                }
        }
    }

    fun sendMessage(
        message: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap()
    ) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        val autoRunEnabled = _uiState.value.autoRunEnabled

        _uiState.update {
            it.copy(
                messages = it.messages + AgentMessage(role = "user", content = trimmed),
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            AgentApiService.sendTurn(
                context = applicationContext,
                inputText = trimmed,
                surface = surface,
                surfaceContext = surfaceContext,
                allowAutonomousActions = autoRunEnabled
            ).onSuccess { response ->
                applyTurnResponse(response)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = error.message ?: "Agent AI is unavailable right now."
                    )
                }
            }
        }
    }

    fun startVoiceRecording(context: Context) {
        if (_uiState.value.isRecordingVoice || _uiState.value.isSending) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(context.cacheDir, "agent_voice_${System.currentTimeMillis()}.m4a")
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
                        _uiState.update { it.copy(isRecordingVoice = true, error = null) }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(error = e.message ?: "Could not start voice recording.")
                        }
                    }
                }
            }
        }
    }

    fun stopVoiceRecordingAndSend(
        surface: String,
        surfaceContext: Map<String, String> = emptyMap()
    ) {
        if (!_uiState.value.isRecordingVoice) return

        val autoRunEnabled = _uiState.value.autoRunEnabled

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
                        _uiState.update {
                            it.copy(isRecordingVoice = false, isSending = true, error = null)
                        }
                    }

                    val result = AgentApiService.sendVoiceTurn(
                        context = applicationContext,
                        audioBytes = bytes,
                        fileName = "agent-voice.m4a",
                        mimeType = "audio/mp4",
                        surface = surface,
                        surfaceContext = surfaceContext,
                        allowAutonomousActions = autoRunEnabled,
                        synthesizeAudio = true
                    )

                    withContext(Dispatchers.Main) {
                        result.onSuccess { response ->
                            applyVoiceTurnResponse(response)
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isSending = false,
                                    error = error.message ?: "Voice agent is unavailable right now."
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isRecordingVoice = false,
                                isSending = false,
                                error = e.message ?: "Could not send voice request."
                            )
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
                } catch (_: Exception) {
                } finally {
                    mediaRecorder = null
                    voiceRecordingFile?.delete()
                    voiceRecordingFile = null
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                    }
                }
            }
        }
    }

    fun consumeUiIntents() {
        _uiState.update { it.copy(pendingUiIntents = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applyTurnResponse(response: AgentTurnResponse) {
        AgentSocketManager.updateSession(response.sessionState.sessionId)
        _uiState.update {
            it.copy(
                isSending = false,
                sessionState = response.sessionState,
                messages = it.messages + AgentMessage(
                    role = "assistant",
                    content = response.assistantMessage.ifBlank {
                        "I’m here. Tell me what to do next in Vormex."
                    }
                ),
                pendingUiIntents = response.uiIntents,
                lastExecutedActions = response.executedActions,
                lastSuggestedActions = response.suggestedActions,
                pendingApprovals = response.pendingActions,
                goals = response.goals
            )
        }
    }

    private fun applyVoiceTurnResponse(response: AgentVoiceTurnResponse) {
        AgentSocketManager.updateSession(response.sessionState.sessionId)
        val newMessages = buildList {
            if (response.transcript.isNotBlank()) {
                add(AgentMessage(role = "user", content = response.transcript))
            }
            add(
                AgentMessage(
                    role = "assistant",
                    content = response.assistantMessage.ifBlank {
                        "I’m here. Tell me what to do next in Vormex."
                    }
                )
            )
        }

        _uiState.update {
            it.copy(
                isSending = false,
                sessionState = response.sessionState,
                messages = it.messages + newMessages,
                pendingUiIntents = response.uiIntents,
                lastExecutedActions = response.executedActions,
                lastSuggestedActions = response.suggestedActions,
                pendingApprovals = response.pendingActions,
                goals = response.goals
            )
        }

        if (!response.audioBase64.isNullOrBlank()) {
            playSynthesizedAudio(response.audioBase64, response.audioMimeType)
        }
    }

    private fun observeSocket() {
        viewModelScope.launch {
            AgentSocketManager.connectionStateFlow.collectLatest { state ->
                _uiState.update {
                    it.copy(socketConnected = state == AgentSocketManager.ConnectionState.CONNECTED)
                }
            }
        }

        viewModelScope.launch {
            AgentSocketManager.events.collectLatest { event ->
                when (event.type) {
                    "pending_action_created",
                    "pending_action_resolved",
                    "pending_actions_changed",
                    "approval_executed" -> refreshPendingActions(silent = true)
                    "goals_changed" -> refreshGoals(silent = true)
                    "turn_completed" -> {
                        refreshPendingActions(silent = true)
                        refreshGoals(silent = true)
                        if (!event.sessionId.isNullOrBlank()) {
                            AgentSocketManager.updateSession(event.sessionId)
                        }
                    }
                }
            }
        }
    }

    private suspend fun connectSocketIfPossible(sessionId: String?) {
        val token = ApiClient.getToken(applicationContext) ?: return
        AgentSocketManager.connect(token, sessionId)
    }

    private fun playSynthesizedAudio(audioBase64: String, mimeType: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    mediaPlayer?.release()
                    mediaPlayer = null

                    val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
                    val extension = when {
                        mimeType?.contains("mpeg") == true || mimeType?.contains("mp3") == true -> ".mp3"
                        else -> ".audio"
                    }
                    val tempFile = File(applicationContext.cacheDir, "agent_tts_${System.currentTimeMillis()}$extension")
                    tempFile.writeBytes(bytes)

                    val player = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            release()
                            tempFile.delete()
                            mediaPlayer = null
                            _uiState.update { state -> state.copy(isPlayingAudio = false) }
                        }
                        setOnErrorListener { mp, _, _ ->
                            mp.release()
                            tempFile.delete()
                            mediaPlayer = null
                            _uiState.update { state -> state.copy(isPlayingAudio = false) }
                            true
                        }
                        start()
                    }

                    mediaPlayer = player
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isPlayingAudio = true) }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isPlayingAudio = false) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { mediaRecorder?.release() }
        runCatching { mediaPlayer?.release() }
        mediaRecorder = null
        mediaPlayer = null
        voiceRecordingFile?.delete()
        voiceRecordingFile = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgentViewModel(context) as T
        }
    }
}
