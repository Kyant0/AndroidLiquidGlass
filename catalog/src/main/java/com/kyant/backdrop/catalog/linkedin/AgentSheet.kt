package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentPendingAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSheetContent(
    viewModel: AgentViewModel,
    surface: String,
    surfaceContext: Map<String, String>,
    userDisplayName: String? = null,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false,
    isDarkTheme: Boolean = contentColor == Color.White,
    enableInlineNavigationActions: Boolean = true,
    onOpenInlineProfile: (String) -> Unit = {},
    onOpenInlineChat: (String) -> Unit = {},
    onSeeMoreInlineResults: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    isFullScreen: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val displaySurface = uiState.sessionState?.currentSurface?.takeIf { it.isNotBlank() } ?: surface
    var draft by rememberSaveable { mutableStateOf("") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedApproval by remember { mutableStateOf<AgentPendingAction?>(null) }
    val openingVoiceGreeting = remember(userDisplayName) {
        buildOpeningVoiceGreetingPrompt(userDisplayName)
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRealtimeVoice(
                surface = surface,
                surfaceContext = surfaceContext,
                openingGreeting = openingVoiceGreeting
            )
        }
    }

    LaunchedEffect(surface) {
        viewModel.ensureSession(surface = surface)
    }

    LaunchedEffect(surface, surfaceContext) {
        viewModel.syncSurface(surface = surface, surfaceContext = surfaceContext)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.heightIn(max = 720.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText(
                        text = "Vormex Agent",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AgentDisplayFontFamily
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        text = if (isFullScreen) "Back" else "Close",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaChip(
                    text = humanizeSurface(displaySurface),
                    contentColor = contentColor,
                    background = contentColor.copy(alpha = 0.08f)
                )
                MetaChip(
                    text = if (uiState.socketConnected) "Live sync on" else "Live sync off",
                    contentColor = contentColor,
                    background = accentColor.copy(alpha = if (uiState.socketConnected) 0.14f else 0.08f)
                )
                if (uiState.pendingApprovals.isNotEmpty()) {
                    MetaChip(
                        text = "${uiState.pendingApprovals.size} approvals",
                        contentColor = contentColor,
                        background = Color(0xFFFFE3E3)
                    )
                }
                if (uiState.goals.isNotEmpty()) {
                    MetaChip(
                        text = "${uiState.goals.size} goals",
                        contentColor = contentColor,
                        background = contentColor.copy(alpha = 0.08f)
                    )
                }
                ActionChip(
                    text = if (uiState.isSavingGoal) "Saving…" else "Add Goal",
                    contentColor = contentColor,
                    background = accentColor.copy(alpha = 0.14f),
                    onClick = { showGoalDialog = true }
                )
            }

            if (!uiState.liveStatus.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        text = uiState.liveStatus ?: "",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            BasicText(
                                text = if (uiState.autoRunEnabled) "Auto-run safe actions" else "Approval mode",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = AgentBodyFontFamily
                                )
                            )
                            BasicText(
                                text = if (uiState.autoRunEnabled) {
                                    "Safe actions run immediately."
                                } else {
                                    "Safe actions wait for your approval."
                                },
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.62f),
                                    fontSize = 12.sp,
                                    fontFamily = AgentBodyFontFamily
                                )
                            )
                        }
                        LiquidToggle(
                            selected = { uiState.autoRunEnabled },
                            onSelect = viewModel::setAutoRunEnabled,
                            backdrop = backdrop
                        )
                    }
                }
            }

            if (uiState.goals.isNotEmpty()) {
                BasicText(
                    text = "Goals",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    )
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.goals.forEach { goal ->
                        GoalChip(
                            goal = goal,
                            contentColor = contentColor,
                            onDelete = { viewModel.deleteGoal(goal.id) }
                        )
                    }
                }
            }

            if (uiState.pendingApprovals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = "Pending Approvals",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                    uiState.pendingApprovals.take(3).forEach { action ->
                        PendingApprovalCard(
                            action = action,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onOpen = { selectedApproval = action }
                        )
                    }
                }
            }

            if (uiState.lastExecutedActions.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.lastExecutedActions.takeLast(3).forEach { action ->
                        MetaChip(
                            text = action.title,
                            contentColor = contentColor,
                            background = accentColor.copy(alpha = 0.14f)
                        )
                    }
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFE3E3))
                        .padding(12.dp)
                ) {
                    BasicText(
                        text = uiState.error ?: "",
                        style = TextStyle(
                            color = Color(0xFF8A1C1C),
                            fontSize = 13.sp,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            uiState.activeInlineResults?.let { inlineResults ->
                AgentInlineResultsSurface(
                    panel = inlineResults,
                    dismissedIds = uiState.dismissedInlineResultIds,
                    actionInProgress = uiState.inlineResultActionInProgress,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isDarkTheme = isDarkTheme,
                    enableNavigationActions = enableInlineNavigationActions,
                    onViewProfile = {
                        viewModel.dismissInlineResults()
                        onOpenInlineProfile(it)
                    },
                    onMessage = {
                        viewModel.dismissInlineResults()
                        onOpenInlineChat(it)
                    },
                    onConnect = viewModel::sendInlineConnectionRequest,
                    onCloseItem = viewModel::dismissInlineResultItem,
                    onClosePanel = viewModel::dismissInlineResults,
                    onSeeMore = onSeeMoreInlineResults
                )
            }

            if (uiState.messages.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = uiState.messages) { message ->
                        AgentMessageBubble(
                            message = message,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f, fill = true))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentColor.copy(alpha = 0.07f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontFamily = AgentBodyFontFamily
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draft.isBlank()) {
                                BasicText(
                                    text = "Tell the agent what you want done in the app…",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.45f),
                                        fontSize = 14.sp,
                                        fontFamily = AgentBodyFontFamily
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting) Color(0xFFFF6B6B)
                                else accentColor.copy(alpha = 0.16f)
                            )
                            .clickable(enabled = !uiState.isSending) {
                                if (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting) {
                                    viewModel.stopRealtimeVoice()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (granted) {
                                        viewModel.startRealtimeVoice(
                                            surface = surface,
                                            surfaceContext = surfaceContext,
                                            openingGreeting = openingVoiceGreeting
                                        )
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isVoiceSessionConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = com.kyant.backdrop.catalog.R.drawable.ic_mic),
                                contentDescription = "Mic",
                                tint = if (uiState.isRecordingVoice) Color.White else contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(2.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(accentColor)
                            .clickable(enabled = !uiState.isSending && !uiState.isVoiceSessionConnecting) {
                                val message = draft
                                draft = ""
                                viewModel.sendMessage(
                                    message = message,
                                    surface = surface,
                                    surfaceContext = surfaceContext
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = when {
                                uiState.isSending -> "Thinking…"
                                uiState.isVoiceSessionConnecting -> "Opening live voice…"
                                else -> "Send to Agent"
                            },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AgentBodyFontFamily
                            )
                        )
                    }
                }

                BasicText(
                    text = if (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting) {
                        "Live voice uses realtime Vormex data and can interrupt replies when you start speaking again."
                    } else if (uiState.autoRunEnabled) {
                        "Safe in-app actions can run automatically. Destructive or billing actions stay blocked."
                    } else {
                        "Safe actions land in approvals first. Destructive or billing actions stay blocked."
                    },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
    }

    if (showGoalDialog) {
            GoalSettingDialog(
                contentColor = contentColor,
                accentColor = accentColor,
                onDismiss = { showGoalDialog = false },
                onSave = { goal, category ->
                    showGoalDialog = false
                    viewModel.createGoal(goal = goal, category = category)
                }
        )
    }

    selectedApproval?.let { action ->
        ModalBottomSheet(
            onDismissRequest = { selectedApproval = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (contentColor == Color.White) Color(0xFF1A1A1A) else Color(0xFFF9F7F2)
        ) {
            ApprovalBottomSheetContent(
                action = action,
                contentColor = contentColor,
                accentColor = accentColor,
                isSubmitting = uiState.isResolvingApproval,
                onApprove = {
                    selectedApproval = null
                    viewModel.approvePendingAction(action.id)
                },
                onReject = {
                    selectedApproval = null
                    viewModel.rejectPendingAction(action.id)
                }
            )
        }
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    contentColor: Color,
    accentColor: Color
) {
    val isUser = message.role.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 22.dp
                    )
                )
                .background(
                    if (isUser) accentColor.copy(alpha = 0.18f)
                    else contentColor.copy(alpha = 0.08f)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicText(
                text = message.content,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}

private fun humanizeSurface(surface: String): String {
    return when (surface.trim().lowercase()) {
        "feed" -> "Home"
        "find_people" -> "Find People"
        "chat" -> "Chat"
        "groups" -> "Groups"
        "profile" -> "Profile"
        "notifications" -> "Notifications"
        "growth_hub" -> "Growth Hub"
        else -> surface.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun MetaChip(
    text: String,
    contentColor: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun ActionChip(
    text: String,
    contentColor: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

private fun buildOpeningVoiceGreetingPrompt(userDisplayName: String?): String {
    val firstName = userDisplayName
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }

    val greetingLine = if (firstName != null) {
        "Hey, $firstName, what's the plan for today?"
    } else {
        "Hey, what's the plan for today?"
    }

    return "Open the conversation first. Say exactly one short warm line: \"$greetingLine\" Then stop speaking and wait for the user's reply."
}

@Composable
private fun GoalChip(
    goal: AgentGoal,
    contentColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = goal.goal,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.12f))
                .clickable(onClick = onDelete)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = "X",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}

@Composable
private fun PendingApprovalCard(
    action: AgentPendingAction,
    contentColor: Color,
    accentColor: Color,
    onOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .clickable(onClick = onOpen)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                text = action.title.ifBlank { "Pending approval" },
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentBodyFontFamily
                )
            )
            BasicText(
                text = action.summary,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}
