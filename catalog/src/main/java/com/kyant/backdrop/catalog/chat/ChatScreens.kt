package com.kyant.backdrop.catalog.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.SharedPostContent
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Main entry point for the Chat tab.
 * @param onInChatThread Callback invoked when entering/leaving a chat thread.
 *        Use this to hide/show bottom navigation for an immersive chat experience.
 */
@Composable
fun ChatTabContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean = true,
    openConversationId: String? = null,
    openChatWithUserId: String? = null,
    onConsumedOpenConversation: () -> Unit = {},
    onConsumedOpenChat: () -> Unit = {},
    onInChatThread: (Boolean) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isInThread = uiState.selectedConversation != null
    
    // Ensure socket stays connected when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.ensureSocketConnected()
                    viewModel.ensureConversationsLoaded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Notify parent about chat thread state changes for bottom nav visibility
    LaunchedEffect(isInThread) {
        onInChatThread(isInThread)
    }
    
    // Handle back button: return to chat list instead of closing app
    BackHandler(enabled = isInThread) {
        viewModel.selectConversation(null)
    }

    LaunchedEffect(Unit) {
        viewModel.ensureConversationsLoaded()
    }

    LaunchedEffect(openChatWithUserId) {
        val userId = openChatWithUserId ?: return@LaunchedEffect
        viewModel.openChatWithUser(userId)
        onConsumedOpenChat()
    }

    LaunchedEffect(openConversationId) {
        val conversationId = openConversationId ?: return@LaunchedEffect
        viewModel.openConversationById(conversationId)
        onConsumedOpenConversation()
    }

    if (isInThread) {
        ChatThreadScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            viewModel = viewModel,
            onNavigateToProfile = onNavigateToProfile
        )
    } else {
        ChatListScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ChatListScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter conversations based on search
    val filteredConversations = if (searchQuery.isNotBlank()) {
        uiState.conversations.filter { conv ->
            val name = conv.otherParticipant.name ?: conv.otherParticipant.username ?: ""
            name.contains(searchQuery, ignoreCase = true)
        }
    } else {
        uiState.conversations
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(4.dp))
        
        // Search Bar
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (isGlassTheme) {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(24f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.25f))
                                }
                            )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.6f))
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        BasicText(
                            "Search conversations...",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { searchQuery = "" },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.6f))
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))

        if (uiState.isLoadingConversations) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppIconLoadingIndicator(contentColor = contentColor)
            }
        } else if (uiState.error != null) {
            BasicText(uiState.error!!, style = TextStyle(contentColor.copy(alpha = 0.8f), 14.sp))
        } else if (uiState.conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText(
                    "No conversations yet.\nStart a chat from someone's profile.",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
            ) {
                // General section header
                item {
                    BasicText(
                        "General",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp)
                    )
                }
                
                items(filteredConversations, key = { it.id }) { conv ->
                    SwipeableConversationRow(
                        conversation = conv,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        backdrop = backdrop,
                        isGlassTheme = isGlassTheme,
                        onClick = { viewModel.selectConversation(conv) },
                        onClearChat = { /* TODO: Implement clear chat */ },
                        onDelete = { /* TODO: Implement delete */ },
                        onArchive = { /* TODO: Implement archive */ },
                        onMarkUnread = { /* TODO: Implement mark unread */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableConversationRow(
    conversation: Conversation,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    onClick: () -> Unit,
    onClearChat: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onMarkUnread: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var showActionMenu by remember { mutableStateOf(false) }
    
    val other = conversation.otherParticipant
    val lastMsg = conversation.lastMessage

    Box(Modifier.fillMaxWidth()) {
        // Background action buttons (revealed on swipe)
        Row(
            Modifier
                .matchParentSize()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Archive button
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6C5CE7))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f, tween(200)) }
                        onArchive()
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_archive),
                    contentDescription = "Archive",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
            Spacer(Modifier.width(8.dp))
            // Delete button
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6B6B))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f, tween(200)) }
                        showActionMenu = true
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
        }
        
        // Main conversation row (swipeable)
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -swipeThreshold) {
                                    offsetX.animateTo(-swipeThreshold * 1.3f, tween(200))
                                } else {
                                    offsetX.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newValue = offsetX.value + dragAmount
                                offsetX.snapTo(newValue.coerceIn(-swipeThreshold * 1.5f, 0f))
                            }
                        }
                    )
                }
                .then(
                    if (isGlassTheme) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(12f.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.3f))
                            }
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f))
            ) {
                AsyncImage(
                    model = other.profileImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicText(
                        other.name ?: other.username ?: "Unknown",
                        style = TextStyle(contentColor, 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    lastMsg?.createdAt?.let { time ->
                        BasicText(
                            formatTime(time),
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                BasicText(
                    lastMsg?.content?.take(50)?.let { if (it.length >= 50) "$it..." else it } ?: "No messages yet",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        style = TextStyle(Color.White, 10.sp)
                    )
                }
            }
        }
        
        // Action menu popup
        if (showActionMenu) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showActionMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2a2a3e))
                        .padding(8.dp)
                ) {
                    Column {
                        // Clear Chat option
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showActionMenu = false
                                    onClearChat()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_clear_chat),
                                contentDescription = "Clear Chat",
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText("Clear Chat", style = TextStyle(Color.White, 14.sp))
                        }
                        
                        // Delete User option
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showActionMenu = false
                                    onDelete()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFFF6B6B))
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText("Delete Chat", style = TextStyle(Color(0xFFFF6B6B), 14.sp))
                        }
                        
                        // Archive option
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showActionMenu = false
                                    onArchive()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_archive),
                                contentDescription = "Archive",
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText("Archive", style = TextStyle(Color.White, 14.sp))
                        }
                        
                        // Mark Unread option
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showActionMenu = false
                                    onMarkUnread()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_mark_unread),
                                contentDescription = "Mark Unread",
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText("Mark as Unread", style = TextStyle(Color.White, 14.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    viewModel: ChatViewModel,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val conv = uiState.selectedConversation ?: return
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastMessageId = uiState.messages.lastOrNull()?.id
    
    // Chat menu state
    var showChatMenu by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    
    // Filtered messages for search
    val displayedMessages = if (isSearchMode && searchQuery.isNotBlank()) {
        uiState.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
    } else {
        uiState.messages
    }
    
    // Load AI suggestions when messages change (after receiving a new message)
    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        val lastMsg = uiState.messages.lastOrNull()
        // Only load suggestions if the last message is from the other person
        if (lastMsg != null && lastMsg.senderId != uiState.currentUserId) {
            viewModel.loadAiSuggestions()
        }
    }

    LaunchedEffect(conv.id) {
        viewModel.markAsRead()
    }

    LaunchedEffect(conv.id, uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && listState.layoutInfo.totalItemsCount == 0) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(lastMessageId) {
        val lastMessage = uiState.messages.lastOrNull() ?: return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val isNearBottom = lastVisibleIndex >= (uiState.messages.lastIndex - 2)
        val isMyMessage = lastMessage.senderId == uiState.currentUserId
        if (isNearBottom || isMyMessage) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(listState, conv.id) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= uiState.messages.lastIndex - 1) {
                    viewModel.markAsRead()
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            // Header (normal or search mode)
            if (isSearchMode) {
            // Search mode header
            Row(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16f.dp) },
                        effects = {
                            vibrancy()
                            blur(12f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Close search",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { 
                            isSearchMode = false 
                            searchQuery = ""
                        },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    textStyle = TextStyle(contentColor, 15.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            BasicText(
                                "Search messages...",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 15.sp)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "${displayedMessages.size} found",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                }
            }
        } else {
            // Normal header - clean design without glass
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { viewModel.selectConversation(null) },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                ) {
                    AsyncImage(model = conv.otherParticipant.profileImage, contentDescription = null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                ) {
                    BasicText(
                        conv.otherParticipant.name ?: conv.otherParticipant.username ?: "Unknown",
                        style = TextStyle(contentColor, 15.sp, fontWeight = FontWeight.Medium)
                    )
                    if (uiState.typingUserId != null) {
                        BasicText("typing...", style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
                    }
                }
                // Menu button (3-dot)
                Spacer(Modifier.width(8.dp))
                Box {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "Menu",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showChatMenu = true },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                    )
                }
            }
        }

        // Messages
        if (uiState.isLoadingMessages) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AppIconLoadingIndicator(contentColor = contentColor)
            }
        } else if (isSearchMode && searchQuery.isNotBlank() && displayedMessages.isEmpty()) {
            // No search results
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText("🔍", style = TextStyle(fontSize = 48.sp))
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        "No messages found",
                        style = TextStyle(contentColor, 16.sp)
                    )
                    BasicText(
                        "Try a different search term",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 10.dp)
            ) {
                items(displayedMessages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        isFromMe = msg.senderId == uiState.currentUserId,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        backdrop = backdrop,
                        isGlassTheme = isGlassTheme,
                        onReply = { viewModel.setReplyTo(msg) },
                        onReact = { emoji -> viewModel.reactToMessage(msg.id, emoji) },
                        onDelete = { forEveryone -> viewModel.deleteMessage(msg.id, forEveryone) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                        currentUserId = uiState.currentUserId
                    )
                }
            }
        }
        
        // Reply preview (shown when replying to a message)
        uiState.replyToMessage?.let { replyMsg ->
            ReplyPreview(
                message = replyMsg,
                contentColor = contentColor,
                accentColor = accentColor,
                backdrop = backdrop,
                onDismiss = { viewModel.clearReplyTo() }
            )
        }
        
        // AI Smart Reply Suggestions (shown above input when available)
        if (uiState.aiSuggestions.isNotEmpty() && inputText.isEmpty()) {
            AiSuggestionsRow(
                suggestions = uiState.aiSuggestions,
                contentColor = contentColor,
                accentColor = accentColor,
                backdrop = backdrop,
                onSuggestionClick = { suggestion ->
                    viewModel.useAiSuggestion(suggestion)
                }
            )
        }

        // Input area
        Row(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus button for attachments
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f))
                    .clickable {
                        // TODO: Open media picker
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "Attach",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                )
            }
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.sendTyping(it.isNotEmpty())
                    // Clear suggestions when user starts typing
                    if (it.isNotEmpty()) {
                        viewModel.clearAiSuggestions()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = false,
                maxLines = 3,
                decorationBox = { innerTextField ->
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (inputText.isEmpty()) {
                            BasicText(
                                "Message...",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                            )
                        }
                        innerTextField()
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim(), uiState.replyToMessage?.id)
                            viewModel.clearAiSuggestions()
                            viewModel.clearReplyTo()
                            inputText = ""
                        }
                    }
                )
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) accentColor else accentColor.copy(alpha = 0.5f))
                    .clickable(enabled = inputText.isNotBlank()) {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim(), uiState.replyToMessage?.id)
                            viewModel.clearAiSuggestions()
                            viewModel.clearReplyTo()
                            inputText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
        } // End of Input Row
        } // End of Column
    
        // Glass-styled chat options menu (overlay)
        if (showChatMenu) {
            GlassChatMenu(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isMuted = isMuted,
                onDismiss = { showChatMenu = false },
                onViewProfile = {
                    onNavigateToProfile(conv.otherParticipant.id)
                    showChatMenu = false
                },
                onSearchMessages = {
                    isSearchMode = true
                    showChatMenu = false
                },
                onToggleMute = {
                    isMuted = !isMuted
                    showChatMenu = false
                },
                onClearChat = {
                    showClearConfirm = true
                    showChatMenu = false
                },
                onReport = {
                    // TODO: Open report dialog
                    showChatMenu = false
                }
            )
        }
        
        // Clear chat confirmation dialog (glass-styled overlay)
        if (showClearConfirm) {
            GlassConfirmDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                title = "Delete Chat",
                message = "Are you sure you want to delete this conversation? All messages will be permanently removed from the database. This action cannot be undone.",
                confirmText = "Delete",
                confirmColor = Color(0xFFFF6B6B),
                onConfirm = {
                    viewModel.deleteCurrentConversation()
                    showClearConfirm = false
                },
                onDismiss = { showClearConfirm = false }
            )
        }
        

    } // End of Box
}

@Composable
private fun AppIconLoadingIndicator(contentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chat_loader_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chat_loader_pulse"
    )

    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Loading",
            modifier = Modifier
                .size(34.dp * pulse)
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}

// Common emoji reactions
private val quickReactions = listOf("❤️", "👍", "😂", "😮", "😢", "🔥")

@Composable
private fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean = true,
    onReply: () -> Unit = {},
    onReact: (String) -> Unit = {},
    onDelete: (Boolean) -> Unit = {},
    onCopy: () -> Unit = {},
    currentUserId: String? = null
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Reply-to preview (if this message is a reply)
        message.replyTo?.let { reply ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(accentColor, RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    reply.content.take(40) + if (reply.content.length > 40) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                    maxLines = 1
                )
            }
        }
        
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (kotlin.math.abs(offsetX.value) > swipeThreshold) {
                                    onReply()
                                }
                                offsetX.animateTo(0f, tween(200))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newValue = offsetX.value + dragAmount
                                // Only allow swipe in one direction based on message owner
                                if (isFromMe) {
                                    offsetX.snapTo(newValue.coerceIn(-swipeThreshold * 1.2f, 0f))
                                } else {
                                    offsetX.snapTo(newValue.coerceIn(0f, swipeThreshold * 1.2f))
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onDoubleTap = { onReact("❤️") }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                // Reply indicator (shown during swipe)
                if (!isFromMe && offsetX.value > 20f) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("↩", style = TextStyle(contentColor, 14.sp))
                    }
                    Spacer(Modifier.width(4.dp))
                }
                
                Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (isGlassTheme) {
                                    Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(18f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(8f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(
                                                if (isFromMe) accentColor.copy(alpha = 0.35f)
                                                else Color.White.copy(alpha = 0.2f)
                                            )
                                        }
                                    )
                                } else {
                                    Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isFromMe) accentColor.copy(alpha = 0.15f)
                                            else contentColor.copy(alpha = 0.08f)
                                        )
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        if (message.isDeleted) {
                            BasicText("Message deleted", style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp))
                        } else {
                            // Try to parse as shared post
                            val sharedPost = SharedPostContent.tryParse(message.content)
                            if (sharedPost != null && sharedPost.type == "shared_post") {
                                SharedPostCard(
                                    sharedPost = sharedPost,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            } else {
                                BasicText(
                                    message.content,
                                    style = TextStyle(contentColor, 15.sp)
                                )
                            }
                        }
                    }
                    
                    // Reactions display
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Group reactions by emoji and show count
                            message.reactions.groupBy { it.emoji }.forEach { (emoji, reactions) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText(emoji, style = TextStyle(fontSize = 12.sp))
                                    if (reactions.size > 1) {
                                        BasicText(
                                            "${reactions.size}",
                                            style = TextStyle(contentColor.copy(alpha = 0.7f), 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Reply indicator (shown during swipe for own messages)
                if (isFromMe && offsetX.value < -20f) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("↩", style = TextStyle(contentColor, 14.sp))
                    }
                }
            }
        }
        
        // Glass-styled context menu for message
        if (showMenu) {
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(12f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                    )
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(6.dp)
                ) {
                    GlassMessageMenuItem(
                        icon = "↩️",
                        text = "Reply",
                        contentColor = contentColor,
                        onClick = { onReply(); showMenu = false }
                    )
                    GlassMessageMenuItem(
                        icon = "😊",
                        text = "React",
                        contentColor = contentColor,
                        onClick = { showReactionPicker = true; showMenu = false }
                    )
                    GlassMessageMenuItem(
                        icon = "📋",
                        text = "Copy",
                        contentColor = contentColor,
                        onClick = { onCopy(); showMenu = false }
                    )
                    if (isFromMe) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(1.dp)
                                .background(contentColor.copy(alpha = 0.1f))
                        )
                        GlassMessageMenuItem(
                            icon = "🗑️",
                            text = "Delete for me",
                            contentColor = Color(0xFFFF8866),
                            onClick = { onDelete(false); showMenu = false }
                        )
                        GlassMessageMenuItem(
                            icon = "🗑️",
                            text = "Delete for all",
                            contentColor = Color(0xFFFF6B6B),
                            onClick = { onDelete(true); showMenu = false }
                        )
                    }
                }
            }
        }
        
        // Quick reaction picker (glass-styled)
        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(20f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickReactions.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onReact(emoji)
                                showReactionPicker = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            emoji,
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassMessageMenuItem(
    icon: String,
    text: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            icon,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp
            )
        )
    }
}

/**
 * Reply preview bar shown above input when replying to a message.
 */
@Composable
private fun ReplyPreview(
    message: Message,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12f.dp) },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                },
                onDrawSurface = { drawRect(accentColor.copy(alpha = 0.15f)) }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(32.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                "Replying to message",
                style = TextStyle(accentColor, 11.sp, fontWeight = FontWeight.Medium)
            )
            BasicText(
                message.content.take(50) + if (message.content.length > 50) "..." else "",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.1f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            BasicText("✕", style = TextStyle(contentColor, 14.sp))
        }
    }
}

private fun formatTime(iso: String): String {
    return try {
        val t = iso.indexOf('T')
        if (t > 0) iso.substring(t + 1, minOf(t + 6, iso.length)) else iso.take(5)
    } catch (_: Exception) {
        iso.take(5)
    }
}

/**
 * AI-powered smart reply suggestion chips.
 * Displays contextual reply suggestions that users can tap to send instantly.
 */
@Composable
private fun AiSuggestionsRow(
    suggestions: List<String>,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // AI label with sparkle icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            BasicText(
                "✨ Smart replies",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        // Horizontally scrollable suggestion chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(16f.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(accentColor.copy(alpha = 0.15f))
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        suggestion,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Glass-styled chat options menu with blur effects matching app theme.
 */
@Composable
private fun GlassChatMenu(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isMuted: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onSearchMessages: () -> Unit,
    onToggleMute: () -> Unit,
    onClearChat: () -> Unit,
    onReport: () -> Unit
) {
    // Full screen dismissible overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        // Menu positioned at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.4f)) }
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = false) { } // Prevent click-through
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .padding(8.dp)
            ) {
                GlassMenuItem(
                    iconRes = R.drawable.ic_profile,
                    text = "View Profile",
                    contentColor = contentColor,
                    onClick = onViewProfile
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_search,
                    text = "Search Messages",
                    contentColor = contentColor,
                    onClick = onSearchMessages
                )
                GlassMenuItem(
                    iconRes = if (isMuted) R.drawable.ic_notifications else R.drawable.ic_notifications_off,
                    text = if (isMuted) "Unmute Notifications" else "Mute Notifications",
                    contentColor = contentColor,
                    onClick = onToggleMute
                )
                
                // Divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(1.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )
                
                GlassMenuItem(
                    iconRes = R.drawable.ic_delete,
                    text = "Delete Chat",
                    contentColor = Color(0xFFFF6B6B),
                    onClick = onClearChat
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_warning,
                    text = "Report",
                    contentColor = Color(0xFFFFAA33),
                    onClick = onReport
                )
            }
        }
    }
}

@Composable
private fun GlassMenuItem(
    iconRes: Int,
    text: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            text,
            style = TextStyle(
                color = contentColor,
                fontSize = 15.sp
            )
        )
    }
}

/**
 * Glass-styled confirmation dialog matching app theme.
 */
@Composable
private fun GlassConfirmDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color = accentColor,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Full screen dismissible overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Dialog box
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) { } // Prevent click-through
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                BasicText(
                    title,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Message
                BasicText(
                    message,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Cancel",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    // Confirm button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(confirmColor.copy(alpha = 0.8f))
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            confirmText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders shared post content as a formatted card instead of raw JSON.
 */
@Composable
private fun SharedPostCard(
    sharedPost: SharedPostContent,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .widthIn(min = 220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .clickable {
                // Open post URL when clicked
                if (sharedPost.postUrl.isNotEmpty()) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(sharedPost.postUrl)
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open post", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(12.dp)
    ) {
        // Shared post label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            BasicText(
                "📤 Shared Post",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        // Post preview image FIRST (if available) - more prominent
        if (!sharedPost.mediaUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(contentColor.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = sharedPost.mediaUrl,
                    contentDescription = "Post media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        
        // Post preview text
        if (sharedPost.preview.isNotEmpty()) {
            // Clean up preview text - remove color/markdown formatting
            val cleanPreview = sharedPost.preview
                .replace(Regex("\\[color:#[0-9a-fA-F]+\\]"), "")
                .replace("[/color]", "")
                .replace("\\n", "\n")
                .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove bold markers
                .trim()
                .take(120)
            
            BasicText(
                cleanPreview + if (sharedPost.preview.length > 120) "..." else "",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
        }
        
        // Author info row at bottom
        sharedPost.author?.let { author ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f))
                ) {
                    if (!author.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = author.profileImage,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Author name
                    if (!author.name.isNullOrEmpty()) {
                        BasicText(
                            author.name,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Author username
                    if (!author.username.isNullOrEmpty()) {
                        BasicText(
                            "@${author.username}",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
                // Arrow icon
                BasicText(
                    "→",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
