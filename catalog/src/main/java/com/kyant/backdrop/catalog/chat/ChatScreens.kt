package com.kyant.backdrop.catalog.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.ChatMutePreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.SharedPostContent
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import java.text.DateFormat
import java.util.Date

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
    viewModel: ChatViewModel,
    isGlassTheme: Boolean = true,
    openConversationId: String? = null,
    openChatWithUserId: String? = null,
    onConsumedOpenConversation: () -> Unit = {},
    onConsumedOpenChat: () -> Unit = {},
    onInChatThread: (Boolean) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isInThread = uiState.selectedConversation != null
    val isResolvingConversation = uiState.isResolvingConversationOpen && uiState.selectedConversation == null
    
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
    } else if (isResolvingConversation) {
        ChatConversationOpeningState(
            contentColor = contentColor,
            accentColor = accentColor
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
    val isRefreshingConversations = uiState.isLoadingConversations && uiState.conversations.isNotEmpty()
    
    // Filter conversations based on search
    val searchedConversations = if (searchQuery.isNotBlank()) {
        uiState.conversations.filter { conv ->
            val name = conv.otherParticipant.name ?: conv.otherParticipant.username ?: ""
            name.contains(searchQuery, ignoreCase = true)
        }
    } else {
        uiState.conversations
    }
    val requestConversations = searchedConversations.filter { it.isMessageRequest }
    val unreadConversations = searchedConversations.filter { !it.isMessageRequest && it.unreadCount > 0 }
    val recentConversations = searchedConversations.filter { !it.isMessageRequest && it.unreadCount == 0 }
    val orderedConversations = buildList {
        addAll(requestConversations)
        addAll(unreadConversations)
        addAll(recentConversations)
    }
    val hasVisibleConversations = orderedConversations.isNotEmpty()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(4.dp))

        if (isRefreshingConversations || !uiState.socketConnected) {
            ChatSyncStatusPill(
                text = if (uiState.socketConnected) "Refreshing chats..." else "Reconnecting chat...",
                contentColor = contentColor,
                accentColor = accentColor
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(contentColor.copy(alpha = if (isGlassTheme) 0.09f else 0.07f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier.size(16.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(contentColor, 13.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        BasicText(
                            "Search conversations...",
                            style = TextStyle(contentColor.copy(alpha = 0.42f), 13.sp)
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
                        .size(16.dp)
                        .clickable { searchQuery = "" },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoadingConversations && uiState.conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatConversationListLoadingState(contentColor = contentColor)
            }
        } else if (!hasVisibleConversations) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatInboxEmptyState(
                    searchQuery = searchQuery,
                    contentColor = contentColor
                )
            }
        } else if (uiState.error != null && uiState.conversations.isEmpty()) {
            BasicText(uiState.error!!, style = TextStyle(contentColor.copy(alpha = 0.8f), 14.sp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
            ) {
                items(orderedConversations, key = { it.id }) { conv ->
                    SwipeableConversationRow(
                        conversation = conv,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        currentUserId = uiState.currentUserId,
                        onClick = { viewModel.selectConversation(conv) }
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
    currentUserId: String?,
    onClick: () -> Unit
) {
    val other = conversation.otherParticipant
    val lastMsg = conversation.lastMessage
    val previewText = conversationPreviewText(conversation, currentUserId)
    val rowTime = lastMsg?.createdAt ?: conversation.updatedAt
    val showUnreadAccent = conversation.unreadCount > 0
    val avatarInitial = (other.name ?: other.username ?: "?")
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            conversation.isMessageRequest -> accentColor.copy(alpha = 0.16f)
                            showUnreadAccent -> accentColor.copy(alpha = 0.1f)
                            else -> contentColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!other.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = other.profileImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BasicText(
                        avatarInitial,
                        style = TextStyle(
                            color = if (conversation.isMessageRequest) accentColor else contentColor.copy(alpha = 0.82f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        other.name ?: other.username ?: "Unknown",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = if (showUnreadAccent) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        formatTime(rowTime),
                        style = TextStyle(
                            color = if (showUnreadAccent) accentColor else contentColor.copy(alpha = 0.44f),
                            fontSize = 10.sp,
                            fontWeight = if (showUnreadAccent) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                BasicText(
                    previewText,
                    style = TextStyle(
                        color = contentColor.copy(alpha = if (showUnreadAccent) 0.72f else 0.54f),
                        fontSize = 11.sp,
                        fontWeight = if (showUnreadAccent) FontWeight.Medium else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        style = TextStyle(Color.White, 9.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun ChatInboxEmptyState(
    searchQuery: String,
    contentColor: Color
) {
    val title: String
    val subtitle: String

    if (searchQuery.isNotBlank()) {
        title = "No chats match \"$searchQuery\""
        subtitle = "Try a different name or username."
    } else {
        title = "No conversations yet"
        subtitle = "Start a chat from a profile and it will appear here."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(contentColor, 18.sp, fontWeight = FontWeight.SemiBold)
        )
        BasicText(
            subtitle,
            style = TextStyle(contentColor.copy(alpha = 0.58f), 13.sp)
        )
    }
}

private data class LoadMoreAnchor(
    val firstVisibleIndex: Int,
    val firstVisibleOffset: Int,
    val previousMessageCount: Int
)

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
    val reportScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val conv = uiState.selectedConversation ?: return
    var inputText by remember(conv.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastMessageId = uiState.messages.lastOrNull()?.id
    val latestOwnMessageId = uiState.messages.lastOrNull { it.senderId == uiState.currentUserId }?.id
    var pendingLoadMoreAnchor by remember(conv.id) { mutableStateOf<LoadMoreAnchor?>(null) }
    var initialBottomScrollDone by remember(conv.id) { mutableStateOf(false) }
    val connectionStatus = when {
        !uiState.socketConnected -> "reconnecting..."
        uiState.isLoadingMessages -> "syncing messages..."
        else -> "live chat"
    }
    val showLoadingWithoutCache = uiState.threadReadyState == ChatThreadReadyState.LOADING_WITHOUT_CACHE
    val showEmptyThread = uiState.threadReadyState == ChatThreadReadyState.EMPTY_THREAD

    // Chat menu state
    var showChatMenu by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var muteUntilMillis by remember(conv.id) {
        mutableStateOf(ChatMutePreferences.getMuteUntilMillis(context, conv.id))
    }
    var showMuteDurationSheet by remember { mutableStateOf(false) }
    var showReportChatSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val isNotificationsMuted = muteUntilMillis > System.currentTimeMillis()

    LaunchedEffect(conv.id) {
        muteUntilMillis = ChatMutePreferences.getMuteUntilMillis(context, conv.id)
    }

    LaunchedEffect(conv.id, muteUntilMillis) {
        val until = muteUntilMillis
        if (until <= System.currentTimeMillis()) return@LaunchedEffect
        val wait = (until - System.currentTimeMillis()).coerceAtMost(365L * 24 * 60 * 60 * 1000)
        delay(wait)
        muteUntilMillis = ChatMutePreferences.getMuteUntilMillis(context, conv.id)
    }
    
    // Filtered messages for search
    val displayedMessages = if (isSearchMode && searchQuery.isNotBlank()) {
        uiState.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
    } else {
        uiState.messages
    }
    val showingFullThread = !isSearchMode || searchQuery.isBlank()
    
    // Load AI suggestions when messages change (after receiving a new message)
    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        val lastMsg = uiState.messages.lastOrNull()
        // Only load suggestions if the last message is from the other person
        if (lastMsg != null && lastMsg.senderId != uiState.currentUserId) {
            viewModel.loadAiSuggestions()
        }
    }

    DisposableEffect(conv.id) {
        onDispose {
            viewModel.sendTyping(false)
        }
    }

    LaunchedEffect(conv.id) {
        viewModel.markAsRead()
    }

    LaunchedEffect(conv.id, uiState.messages.size, uiState.isLoadingMoreMessages) {
        if (!initialBottomScrollDone && uiState.messages.isNotEmpty() && !uiState.isLoadingMoreMessages) {
            listState.scrollToItem(uiState.messages.lastIndex)
            initialBottomScrollDone = true
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

    LaunchedEffect(conv.id, uiState.messages.size, uiState.isLoadingMoreMessages) {
        val anchor = pendingLoadMoreAnchor ?: return@LaunchedEffect
        if (uiState.isLoadingMoreMessages) return@LaunchedEffect

        val addedCount = uiState.messages.size - anchor.previousMessageCount
        if (addedCount > 0 && uiState.messages.isNotEmpty()) {
            val targetIndex = (anchor.firstVisibleIndex + addedCount)
                .coerceAtMost(uiState.messages.lastIndex)
            listState.scrollToItem(targetIndex, anchor.firstVisibleOffset)
        }

        pendingLoadMoreAnchor = null
    }

    LaunchedEffect(
        listState,
        conv.id,
        showingFullThread,
        uiState.hasMoreMessages,
        uiState.isLoadingMessages,
        uiState.isLoadingMoreMessages,
        uiState.messages.size
    ) {
        if (!showingFullThread) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                val shouldLoadMore =
                    uiState.hasMoreMessages &&
                        !uiState.isLoadingMessages &&
                        !uiState.isLoadingMoreMessages &&
                        uiState.messages.isNotEmpty() &&
                        firstVisibleIndex <= 2

                if (shouldLoadMore && pendingLoadMoreAnchor == null) {
                    pendingLoadMoreAnchor = LoadMoreAnchor(
                        firstVisibleIndex = listState.firstVisibleItemIndex,
                        firstVisibleOffset = listState.firstVisibleItemScrollOffset,
                        previousMessageCount = uiState.messages.size
                    )
                    viewModel.loadMoreMessages()
                }
            }
    }

    LaunchedEffect(listState, conv.id, uiState.messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (
                    lastVisibleIndex != null &&
                    uiState.messages.isNotEmpty() &&
                    lastVisibleIndex >= uiState.messages.lastIndex - 1
                ) {
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Close search",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                isSearchMode = false
                                searchQuery = ""
                            },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = TextStyle(contentColor, 13.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                BasicText(
                                    "Search messages...",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "${displayedMessages.size}",
                            style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
                        )
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.selectConversation(null) },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f))
                            .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                    ) {
                        AsyncImage(
                            model = conv.otherParticipant.profileImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                    ) {
                        BasicText(
                            conv.otherParticipant.name ?: conv.otherParticipant.username ?: "Unknown",
                            style = TextStyle(contentColor, 14.sp, fontWeight = FontWeight.SemiBold)
                        )
                        BasicText(
                            connectionStatus,
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
                        )
                    }
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "Menu",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showChatMenu = true },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                    )
                }
            }

            // Messages
            when {
                showLoadingWithoutCache -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ChatThreadLoadingState(
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }
                isSearchMode && searchQuery.isNotBlank() && displayedMessages.isEmpty() && uiState.messages.isNotEmpty() -> {
            // No search results
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText("🔍", style = TextStyle(fontSize = 26.sp))
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                "No messages found",
                                style = TextStyle(contentColor, 14.sp, fontWeight = FontWeight.Medium)
                            )
                            BasicText(
                                "Try a different search term",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
                showEmptyThread -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ChatThreadEmptyState()
                    }
                }
                else -> {
                    Column(Modifier.weight(1f)) {
                        if (uiState.isLoadingMessages && uiState.messages.isNotEmpty()) {
                            ChatSyncStatusPill(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                text = "syncing messages...",
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 8.dp)
                        ) {
                            if (uiState.isLoadingMoreMessages && showingFullThread) {
                                item(key = "loading-more-history") {
                                    ChatSyncStatusPill(
                                        text = "Loading older messages...",
                                        contentColor = contentColor,
                                        accentColor = accentColor
                                    )
                                }
                            }

                            itemsIndexed(displayedMessages, key = { _, msg -> msg.id }) { index, msg ->
                                val isFromMe = msg.senderId == uiState.currentUserId
                                val nextMsg = displayedMessages.getOrNull(index + 1)
                                val showTimestamp = shouldShowClusterMetaForDm(msg, nextMsg)
                                val deliveryStateText = if (isFromMe && msg.id == latestOwnMessageId) {
                                    if (msg.status.equals("READ", ignoreCase = true) || msg.readAt != null) {
                                        "Read"
                                    } else {
                                        "Sent"
                                    }
                                } else {
                                    null
                                }

                                MessageBubble(
                                    message = msg,
                                    isFromMe = isFromMe,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    backdrop = backdrop,
                                    isGlassTheme = isGlassTheme,
                                    showTimestamp = showTimestamp,
                                    onReply = { viewModel.setReplyTo(msg) },
                                    onReact = { emoji -> viewModel.reactToMessage(msg.id, emoji) },
                                    onDelete = { forEveryone -> viewModel.deleteMessage(msg.id, forEveryone) },
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                    },
                                    currentUserId = uiState.currentUserId,
                                    deliveryStateText = deliveryStateText
                                )
                            }
                        }
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
            if (uiState.aiSuggestions.isNotEmpty() && inputText.isEmpty() && !showEmptyThread) {
                AiSuggestionsRow(
                    suggestions = uiState.aiSuggestions,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    onSuggestionClick = { suggestion ->
                        inputText = suggestion
                        viewModel.clearAiSuggestions()
                        viewModel.sendTyping(true)
                    }
                )
            }

            val sendCurrentMessage = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText.trim(), uiState.replyToMessage?.id)
                    viewModel.sendTyping(false)
                    viewModel.clearAiSuggestions()
                    viewModel.clearReplyTo()
                    inputText = ""
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (uiState.typingUserId != null) {
                    VormexTypingPresence(
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable {
                                // TODO: Open media picker
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "Attach",
                            modifier = Modifier.size(18.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    BasicTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.sendTyping(it.isNotEmpty())
                            if (it.isNotEmpty()) {
                                viewModel.clearAiSuggestions()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(contentColor, 13.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = false,
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                if (inputText.isEmpty()) {
                                    BasicText(
                                        "Message...",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendCurrentMessage() })
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) accentColor else accentColor.copy(alpha = 0.45f))
                            .clickable(enabled = inputText.isNotBlank()) { sendCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                    }
                }
                if (!uiState.socketConnected) {
                    BasicText(
                        "reconnecting...",
                        modifier = Modifier.padding(start = 10.dp, top = 4.dp),
                        style = TextStyle(contentColor.copy(alpha = 0.52f), 11.sp)
                    )
                }
            }
        } // End of Column
    
        // Glass-styled chat options menu (overlay)
        if (showChatMenu) {
            GlassChatMenu(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isMuted = isNotificationsMuted,
                muteStatusLine = formatChatMuteStatusLine(isNotificationsMuted, muteUntilMillis),
                isGlassTheme = isGlassTheme,
                onDismiss = { showChatMenu = false },
                onViewProfile = {
                    onNavigateToProfile(conv.otherParticipant.id)
                    showChatMenu = false
                },
                onSearchMessages = {
                    isSearchMode = true
                    showChatMenu = false
                },
                onMuteNotifications = {
                    showChatMenu = false
                    if (isNotificationsMuted) {
                        ChatMutePreferences.clearMute(context, conv.id)
                        muteUntilMillis = 0L
                        Toast.makeText(context, "Notifications unmuted", Toast.LENGTH_SHORT).show()
                    } else {
                        showMuteDurationSheet = true
                    }
                },
                onClearChat = {
                    showClearConfirm = true
                    showChatMenu = false
                },
                onReport = {
                    showReportChatSheet = true
                    showChatMenu = false
                }
            )
        }

        if (showMuteDurationSheet) {
            GlassMuteDurationSheet(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                peerName = conv.otherParticipant.name ?: conv.otherParticipant.username ?: "this chat",
                onDismiss = { showMuteDurationSheet = false },
                onConfirmDuration = { durationMs ->
                    val until = System.currentTimeMillis() + durationMs
                    ChatMutePreferences.setMuteUntilMillis(context, conv.id, until)
                    muteUntilMillis = until
                    showMuteDurationSheet = false
                    Toast.makeText(context, "Notifications muted", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showReportChatSheet) {
            GlassReportChatDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                reportedLabel = conv.otherParticipant.name ?: conv.otherParticipant.username ?: "this person",
                onDismiss = { showReportChatSheet = false },
                onSubmit = { reason, details ->
                    reportScope.launch {
                        ApiClient.reportChat(
                            context,
                            conv.id,
                            reason,
                            details
                        ).onSuccess {
                            Toast.makeText(
                                context,
                                "Thanks — we received your report and will review it.",
                                Toast.LENGTH_LONG
                            ).show()
                            showReportChatSheet = false
                        }.onFailure { e ->
                            Toast.makeText(
                                context,
                                e.message ?: "Could not submit report",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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
private fun ChatConversationOpeningState(
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ChatThreadLoadingState(
            contentColor = contentColor,
            accentColor = accentColor
        )
    }
}

/**
 * Typing indicator: warm, brand-forward presence — not read receipts or “seen” lists.
 */
@Composable
private fun VormexTypingPresence(
    contentColor: Color,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "vormex_typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "typing_phase"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha = 0.35f + 0.55f * (
                sin(phase * 2.0 * PI + i * 0.8).toFloat() * 0.5f + 0.5f
                ).coerceIn(0f, 1f)
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = alpha))
            )
        }
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = accentColor.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold)) {
                    append("Vormex")
                }
                append(" ")
                withStyle(SpanStyle(color = contentColor.copy(alpha = 0.62f), fontSize = 12.sp)) {
                    append("shaping a reply")
                }
                append("…")
            },
            style = TextStyle(fontSize = 13.sp)
        )
    }
}

@Composable
private fun ChatSyncStatusPill(
    text: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = pulseAlpha))
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text,
            style = TextStyle(contentColor.copy(alpha = 0.78f), 12.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ChatConversationListLoadingState(contentColor: Color) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        repeat(5) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingPlaceholder(
                    modifier = Modifier.size(52.dp),
                    contentColor = contentColor,
                    pulseAlpha = pulseAlpha,
                    shape = CircleShape
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingPlaceholder(
                        modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.58f else 0.7f).height(14.dp),
                        contentColor = contentColor,
                        pulseAlpha = pulseAlpha
                    )
                    LoadingPlaceholder(
                        modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.36f else 0.44f).height(11.dp),
                        contentColor = contentColor,
                        pulseAlpha = pulseAlpha
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadLoadingState(
    contentColor: Color,
    accentColor: Color
) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        ChatSyncStatusPill(
            text = "syncing messages...",
            contentColor = contentColor,
            accentColor = accentColor
        )
        Spacer(Modifier.height(12.dp))
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                LoadingPlaceholder(
                    modifier = Modifier
                        .align(if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .fillMaxWidth(if (index % 2 == 0) 0.52f else 0.46f)
                        .height(if (index == 2) 44.dp else 34.dp),
                    contentColor = contentColor,
                    pulseAlpha = pulseAlpha
                )
            }
        }
    }
}

@Composable
private fun ChatThreadEmptyState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.contact_us))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(220.dp),
                contentScale = ContentScale.Fit,
                clipToCompositionBounds = true
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder(
    modifier: Modifier,
    contentColor: Color,
    pulseAlpha: Float,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(contentColor.copy(alpha = 0.08f + (pulseAlpha * 0.14f)))
    )
}

@Composable
private fun LoadingPlaceholder(
    modifier: Modifier,
    contentColor: Color,
    pulseAlpha: Float,
    shape: androidx.compose.ui.graphics.Shape
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(contentColor.copy(alpha = 0.08f + (pulseAlpha * 0.14f)))
    )
}

@Composable
private fun rememberLoaderPulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_loader_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chat_loader_pulse_alpha"
    )
    return pulseAlpha
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
    showTimestamp: Boolean = true,
    onReply: () -> Unit = {},
    onReact: (String) -> Unit = {},
    onDelete: (Boolean) -> Unit = {},
    onCopy: () -> Unit = {},
    currentUserId: String? = null,
    deliveryStateText: String? = null
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFromMe) accentColor.copy(alpha = 0.18f)
                                else contentColor.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 252.dp)
                    ) {
                        if (message.isDeleted) {
                            BasicText("Message deleted", style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp))
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
                                    style = TextStyle(contentColor, 14.sp)
                                )
                            }
                        }
                    }
                    
                    // Reactions display
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
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

                    val metaParts = buildList {
                        if (showTimestamp) add(formatTime(message.createdAt))
                        if (!deliveryStateText.isNullOrBlank()) add(deliveryStateText)
                    }
                    if (metaParts.isNotEmpty()) {
                        BasicText(
                            metaParts.joinToString(" • "),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                        )
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

private fun formatChatMuteStatusLine(isMuted: Boolean, muteUntilMillis: Long): String {
    if (!isMuted) return "Notifications on"
    return try {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        "Muted until ${fmt.format(Date(muteUntilMillis))}"
    } catch (_: Exception) {
        "Muted"
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

private fun conversationPreviewText(
    conversation: Conversation,
    currentUserId: String?
): String {
    val lastMessage = conversation.lastMessage ?: return when {
        conversation.isMessageRequest -> "Message request"
        else -> "No messages yet"
    }

    val prefix = if (lastMessage.senderId == currentUserId) "You: " else ""
    val body = when (lastMessage.contentType.lowercase()) {
        "image" -> "sent a photo"
        "video" -> "sent a video"
        "audio" -> "sent a voice note"
        "document", "file" -> "shared a file"
        "post" -> "shared a post"
        else -> lastMessage.content
    }

    return "$prefix$body".trim()
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
    muteStatusLine: String,
    isGlassTheme: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onSearchMessages: () -> Unit,
    onMuteNotifications: () -> Unit,
    onClearChat: () -> Unit,
    onReport: () -> Unit
) {
    val menuSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(16f.dp) },
            effects = {
                vibrancy()
                blur(20f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.4f)) }
        )
    } else {
        Modifier.background(
            color = contentColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .then(menuSurface)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 228.dp, max = 280.dp)
                    .padding(8.dp)
            ) {
                GlassMenuItem(
                    iconRes = R.drawable.ic_profile,
                    text = "View profile",
                    contentColor = contentColor,
                    onClick = onViewProfile
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_search,
                    text = "Search messages",
                    contentColor = contentColor,
                    onClick = onSearchMessages
                )
                GlassMenuItem(
                    iconRes = if (isMuted) R.drawable.ic_notifications_off else R.drawable.ic_notifications,
                    text = if (isMuted) "Unmute notifications" else "Mute notifications",
                    subtitle = muteStatusLine,
                    contentColor = contentColor,
                    onClick = onMuteNotifications
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(1.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )

                GlassMenuItem(
                    iconRes = R.drawable.ic_delete,
                    text = "Delete chat",
                    contentColor = Color(0xFFFF6B6B),
                    onClick = onClearChat
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_warning,
                    text = "Report",
                    subtitle = "Safety concerns",
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
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = if (subtitle != null) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun GlassMuteDurationSheet(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    peerName: String,
    onDismiss: () -> Unit,
    onConfirmDuration: (Long) -> Unit
) {
    val options = remember {
        listOf(
            "1 hour" to 60L * 60 * 1000,
            "8 hours" to 8L * 60 * 60 * 1000,
            "1 week" to 7L * 24 * 60 * 60 * 1000,
            "1 year" to 365L * 24 * 60 * 60 * 1000
        )
    }
    var selectedMs by remember { mutableStateOf(options[0].second) }

    val cardSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(20f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
        )
    } else {
        Modifier.background(
            color = contentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        )
    }

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
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .then(cardSurface)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
        ) {
            Column(Modifier.padding(22.dp)) {
                BasicText(
                    "Mute notifications",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "How long should we silence alerts from $peerName? You won’t get message notifications from this chat on your device until then.",
                    style = TextStyle(contentColor.copy(alpha = 0.72f), 13.sp)
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (label, ms) ->
                        val selected = selectedMs == ms
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) accentColor.copy(alpha = 0.18f)
                                    else contentColor.copy(alpha = 0.07f)
                                )
                                .clickable { selectedMs = ms }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                            )
                            if (selected) {
                                BasicText("✓", style = TextStyle(accentColor, 14.sp, FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.85f))
                            .clickable {
                                onConfirmDuration(selectedMs)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Mute",
                            style = TextStyle(Color.White, 15.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassReportChatDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    reportedLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = remember {
        listOf("Harassment or hate", "Spam or scam", "Impersonation", "Other")
    }
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    val cardSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(20f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
        )
    } else {
        Modifier.background(
            color = contentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp)
        )
    }

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
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .then(cardSurface)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
        ) {
            Column(Modifier.padding(22.dp)) {
                BasicText(
                    "Report conversation",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "Tell us what’s wrong about your chat with $reportedLabel.",
                    style = TextStyle(contentColor.copy(alpha = 0.72f), 13.sp)
                )
                Spacer(Modifier.height(14.dp))
                reasons.forEach { reason ->
                    val selected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) accentColor.copy(alpha = 0.16f) else contentColor.copy(alpha = 0.06f)
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            if (selected) "●" else "○",
                            style = TextStyle(accentColor, 12.sp),
                            modifier = Modifier.width(20.dp)
                        )
                        BasicText(
                            reason,
                            style = TextStyle(contentColor, 14.sp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BasicText(
                    "Details (optional)",
                    style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
                )
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 13.sp),
                    cursorBrush = SolidColor(contentColor),
                    maxLines = 4
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.85f))
                            .clickable {
                                onSubmit(selectedReason, details.trim())
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Submit",
                            style = TextStyle(Color.White, 15.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
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
