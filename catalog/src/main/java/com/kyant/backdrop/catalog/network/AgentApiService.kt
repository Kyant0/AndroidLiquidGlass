package com.kyant.backdrop.catalog.network

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.AgentApproveActionResponse
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentGoalDeleteResponse
import com.kyant.backdrop.catalog.network.models.AgentGoalUpsertRequest
import com.kyant.backdrop.catalog.network.models.AgentGoalUpsertResponse
import com.kyant.backdrop.catalog.network.models.AgentGoalsResponse
import com.kyant.backdrop.catalog.network.models.AgentPendingAction
import com.kyant.backdrop.catalog.network.models.AgentPendingActionsResponse
import com.kyant.backdrop.catalog.network.models.AgentRejectActionResponse
import com.kyant.backdrop.catalog.network.models.AgentSessionBootstrapResponse
import com.kyant.backdrop.catalog.network.models.AgentSessionRequest
import com.kyant.backdrop.catalog.network.models.AgentTurnRequest
import com.kyant.backdrop.catalog.network.models.AgentTurnResponse
import com.kyant.backdrop.catalog.network.models.AgentVoiceTurnResponse
import com.kyant.backdrop.catalog.network.models.ApiError
import com.kyant.backdrop.catalog.network.models.SmartRepliesRequest
import com.kyant.backdrop.catalog.network.models.SmartRepliesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(name = "vormex_agent_prefs")

object AgentApiService {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val agentSessionKey = stringPreferencesKey("agent_session_id")
    private val agentAutoRunKey = booleanPreferencesKey("agent_auto_run_enabled")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("AgentApiService", message)
                    }
                }
                level = LogLevel.BODY
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
    }

    private suspend fun authToken(context: Context): String? = ApiClient.getToken(context)

    private suspend fun parseError(response: HttpResponse): String {
        return try {
            val error: ApiError = response.body()
            error.getErrorMessage()
        } catch (_: Exception) {
            response.bodyAsText().ifBlank { "Request failed with status ${response.status.value}" }
        }
    }

    suspend fun getStoredSessionId(context: Context): String? {
        return context.agentDataStore.data.first()[agentSessionKey]
    }

    suspend fun getStoredAutoRunEnabled(context: Context): Boolean {
        return context.agentDataStore.data.first()[agentAutoRunKey] ?: false
    }

    suspend fun setStoredAutoRunEnabled(context: Context, enabled: Boolean) {
        context.agentDataStore.edit { prefs ->
            prefs[agentAutoRunKey] = enabled
        }
    }

    suspend fun clearSession(context: Context) {
        context.agentDataStore.edit { prefs ->
            prefs.remove(agentSessionKey)
        }
    }

    private suspend fun saveSessionId(context: Context, sessionId: String) {
        context.agentDataStore.edit { prefs ->
            prefs[agentSessionKey] = sessionId
        }
    }

    suspend fun bootstrapSession(
        context: Context,
        mode: String = "text",
        surface: String = "global",
        allowAutonomousActions: Boolean = true
    ): Result<AgentSessionBootstrapResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val storedSessionId = getStoredSessionId(context)
            val response = client.post("$baseUrl/agent/sessions") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentSessionRequest(
                        sessionId = storedSessionId,
                        mode = mode,
                        surface = surface,
                        allowAutonomousActions = allowAutonomousActions
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: AgentSessionBootstrapResponse = response.body()
                if (body.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionId)
                }
                Result.success(body)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureSessionId(
        context: Context,
        mode: String,
        surface: String,
        allowAutonomousActions: Boolean
    ): Result<String> {
        val stored = getStoredSessionId(context)
        if (!stored.isNullOrBlank()) return Result.success(stored)

        return bootstrapSession(
            context = context,
            mode = mode,
            surface = surface,
            allowAutonomousActions = allowAutonomousActions
        ).map { it.sessionId }
    }

    suspend fun sendTurn(
        context: Context,
        inputText: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean = true
    ): Result<AgentTurnResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val sessionId = ensureSessionId(
                context = context,
                mode = "text",
                surface = surface,
                allowAutonomousActions = allowAutonomousActions
            ).getOrElse { return Result.failure(it) }

            val response = client.post("$baseUrl/agent/sessions/$sessionId/turns") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentTurnRequest(
                        inputText = inputText,
                        surface = surface,
                        surfaceContext = surfaceContext,
                        allowAutonomousActions = allowAutonomousActions
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: AgentTurnResponse = response.body()
                if (body.sessionState.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionState.sessionId)
                }
                Result.success(body)
            } else {
                if (response.status.value == 404) {
                    clearSession(context)
                }
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVoiceTurn(
        context: Context,
        audioBytes: ByteArray,
        fileName: String,
        mimeType: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        allowAutonomousActions: Boolean = true,
        synthesizeAudio: Boolean = true
    ): Result<AgentVoiceTurnResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val sessionId = ensureSessionId(
                context = context,
                mode = "voice",
                surface = surface,
                allowAutonomousActions = allowAutonomousActions
            ).getOrElse { return Result.failure(it) }

            val response = client.post("$baseUrl/agent/sessions/$sessionId/voice") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "audio",
                        audioBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${fileName.ifBlank { "agent-voice.m4a" }}")
                        }
                    )
                    append("surface", surface)
                    append("allowAutonomousActions", allowAutonomousActions.toString())
                    append("synthesizeAudio", synthesizeAudio.toString())
                    append("surfaceContext", json.encodeToString(surfaceContext))
                }))
            }

            if (response.status.value in 200..299) {
                val body: AgentVoiceTurnResponse = response.body()
                if (body.sessionState.sessionId.isNotBlank()) {
                    saveSessionId(context, body.sessionState.sessionId)
                }
                Result.success(body)
            } else {
                if (response.status.value == 404) {
                    clearSession(context)
                }
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingActions(context: Context): Result<List<AgentPendingAction>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/agent/pending-actions") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                val body: AgentPendingActionsResponse = response.body()
                Result.success(body.actions)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePendingAction(
        context: Context,
        actionId: String
    ): Result<AgentApproveActionResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/agent/approve/$actionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPendingAction(
        context: Context,
        actionId: String
    ): Result<AgentRejectActionResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/agent/reject/$actionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoals(context: Context): Result<List<AgentGoal>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/agent/goals") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                val body: AgentGoalsResponse = response.body()
                Result.success(body.goals)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGoal(
        context: Context,
        goal: String,
        category: String? = null,
        priority: Int? = null
    ): Result<AgentGoalUpsertResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/agent/goals") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    AgentGoalUpsertRequest(
                        goal = goal,
                        category = category,
                        priority = priority
                    )
                )
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(
        context: Context,
        goalId: String
    ): Result<AgentGoalDeleteResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$baseUrl/agent/goals/$goalId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmartReplies(
        context: Context,
        lastMessage: String,
        conversationId: String? = null,
        contextText: String? = null
    ): Result<List<String>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/ai/chat/smart-replies") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    SmartRepliesRequest(
                        lastMessage = lastMessage,
                        conversationId = conversationId,
                        context = contextText
                    )
                )
            }
            if (response.status.value in 200..299) {
                val body: SmartRepliesResponse = response.body()
                Result.success(body.replies)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
