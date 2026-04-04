package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AgentSessionRequest(
    val sessionId: String? = null,
    val mode: String = "text",
    val surface: String = "global",
    val allowAutonomousActions: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class AgentSessionState(
    val sessionId: String = "",
    val status: String = "active",
    val mode: String = "text",
    val currentSurface: String? = null,
    val memorySummary: String? = null,
    val allowAutonomousActions: Boolean = true,
    val lastResponseId: String? = null
)

@Serializable
data class AgentSessionBootstrapResponse(
    val sessionId: String = "",
    val mode: String = "text",
    val sessionState: AgentSessionState = AgentSessionState()
)

@Serializable
data class AgentTurnRequest(
    val inputText: String,
    val surface: String = "global",
    val surfaceContext: Map<String, String> = emptyMap(),
    val allowAutonomousActions: Boolean = true
)

@Serializable
data class AgentUiIntent(
    val type: String = "",
    val tab: String? = null,
    val userId: String? = null,
    val conversationId: String? = null,
    val groupId: String? = null,
    val route: String? = null,
    val label: String? = null,
    val prefillText: String? = null,
    val payload: JsonElement? = null
)

@Serializable
data class AgentAction(
    val type: String = "",
    val toolName: String = "",
    val status: String = "executed",
    val title: String = "",
    val summary: String = "",
    val pendingActionId: String? = null,
    val entityId: String? = null,
    val entityType: String? = null,
    val uiIntents: List<AgentUiIntent> = emptyList(),
    val payload: JsonElement? = null
)

@Serializable
data class AgentPendingAction(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val toolName: String = "",
    val actionType: String = "",
    val title: String = "",
    val summary: String = "",
    val input: JsonElement? = null,
    val status: String = "pending",
    val context: JsonElement? = null,
    val createdAt: String = "",
    val expiresAt: String = "",
    val resolvedAt: String? = null
)

@Serializable
data class AgentGoal(
    val id: String = "",
    val userId: String = "",
    val goal: String = "",
    val category: String? = null,
    val priority: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class AgentTurnResponse(
    val assistantMessage: String = "",
    val executedActions: List<AgentAction> = emptyList(),
    val suggestedActions: List<AgentAction> = emptyList(),
    val uiIntents: List<AgentUiIntent> = emptyList(),
    val pendingActions: List<AgentPendingAction> = emptyList(),
    val goals: List<AgentGoal> = emptyList(),
    val memorySummary: String? = null,
    val sessionState: AgentSessionState = AgentSessionState()
)

@Serializable
data class AgentVoiceTurnResponse(
    val assistantMessage: String = "",
    val executedActions: List<AgentAction> = emptyList(),
    val suggestedActions: List<AgentAction> = emptyList(),
    val uiIntents: List<AgentUiIntent> = emptyList(),
    val pendingActions: List<AgentPendingAction> = emptyList(),
    val goals: List<AgentGoal> = emptyList(),
    val memorySummary: String? = null,
    val sessionState: AgentSessionState = AgentSessionState(),
    val transcript: String = "",
    val audioBase64: String? = null,
    val audioMimeType: String? = null
)

@Serializable
data class AgentPendingActionsResponse(
    val actions: List<AgentPendingAction> = emptyList()
)

@Serializable
data class AgentApproveActionResponse(
    val success: Boolean = false,
    val assistantMessage: String? = null,
    val executedAction: AgentAction? = null,
    val uiIntents: List<AgentUiIntent> = emptyList(),
    val pendingActions: List<AgentPendingAction> = emptyList()
)

@Serializable
data class AgentRejectActionResponse(
    val success: Boolean = false,
    val assistantMessage: String? = null,
    val pendingActions: List<AgentPendingAction> = emptyList()
)

@Serializable
data class AgentGoalsResponse(
    val goals: List<AgentGoal> = emptyList()
)

@Serializable
data class AgentGoalUpsertRequest(
    val goal: String,
    val category: String? = null,
    val priority: Int? = null
)

@Serializable
data class AgentGoalUpsertResponse(
    val goal: AgentGoal? = null,
    val goals: List<AgentGoal> = emptyList()
)

@Serializable
data class AgentGoalDeleteResponse(
    val success: Boolean = false,
    val goals: List<AgentGoal> = emptyList()
)

@Serializable
data class SmartRepliesRequest(
    val lastMessage: String,
    val conversationId: String? = null,
    val context: String? = null
)

@Serializable
data class SmartRepliesResponse(
    val replies: List<String> = emptyList()
)
