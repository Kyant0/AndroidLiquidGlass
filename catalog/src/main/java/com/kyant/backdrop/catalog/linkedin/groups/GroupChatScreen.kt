package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.chat.shouldShowClusterMetaForGroup
import com.kyant.backdrop.catalog.network.GroupSocketManager
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== Group Chat Screen ====================

@Composable
fun GroupChatScreen(
    groupId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentUserId: String?,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val chatState by viewModel.chatState.collectAsState()
    
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Load chat when screen opens
    LaunchedEffect(groupId) {
        viewModel.loadGroupChat(groupId)
    }
    
    // Cleanup when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveGroupChat()
        }
    }
    
    // Auto-scroll to latest message
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header
        ChatHeader(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            group = chatState.group,
            onlineCount = chatState.onlineCount,
            connectionState = chatState.connectionState,
            onBackClick = onNavigateBack
        )
        
        // Messages
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatState.isLoading && chatState.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (chatState.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            "No messages yet",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        )
                        BasicText(
                            "Start the conversation!",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Load more indicator
                    if (chatState.isLoadingMore) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    
                    // Messages grouped by date
                    val groupedMessages = chatState.messages.groupBy { msg ->
                        formatDateHeader(msg.createdAt)
                    }
                    
                    groupedMessages.forEach { (date, messages) ->
                        // Date header
                        item(key = "date_$date") {
                            DateDivider(contentColor = contentColor, date = date)
                        }
                        
                        itemsIndexed(messages, key = { _, m -> m.id }) { index, message ->
                            val isOwnMessage = message.senderId == currentUserId
                            val nextInDay = messages.getOrNull(index + 1)
                            val showTimestamp = shouldShowClusterMetaForGroup(message, nextInDay)
                            
                            MessageBubble(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                message = message,
                                isOwnMessage = isOwnMessage,
                                showTimestamp = showTimestamp,
                                onLongPress = {
                                    viewModel.setReplyingTo(message)
                                },
                                onAuthorClick = { onNavigateToProfile(message.senderId) }
                            )
                        }
                    }
                }
            }
            
            // Typing indicators
            Box(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = chatState.typingUsers.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    TypingIndicator(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        typingUsers = chatState.typingUsers
                    )
                }
            }
        }
        
        // Reply preview
        AnimatedVisibility(
            visible = chatState.replyingTo != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            chatState.replyingTo?.let { replyMessage ->
                ReplyPreview(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    message = replyMessage,
                    onDismiss = { viewModel.setReplyingTo(null) }
                )
            }
        }
        
        // Input bar
        ChatInputBar(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            value = messageInput,
            onValueChange = { 
                messageInput = it
                viewModel.sendTyping(it.isNotEmpty())
            },
            isSending = chatState.isSending,
            onSend = {
                if (messageInput.isNotBlank()) {
                    viewModel.sendMessage(messageInput)
                    messageInput = ""
                }
            }
        )
    }
}

// ==================== Header ====================

@Composable
private fun ChatHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    group: Group?,
    onlineCount: Int,
    connectionState: GroupSocketManager.ConnectionState,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(contentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Group icon
        Box(
            Modifier
                .size(44.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (group?.iconImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(group.iconImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            BasicText(
                group?.name ?: "Loading...",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection state indicator
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            when (connectionState) {
                                GroupSocketManager.ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                GroupSocketManager.ConnectionState.CONNECTING -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    if (onlineCount > 0) "$onlineCount online" else 
                        when (connectionState) {
                            GroupSocketManager.ConnectionState.CONNECTED -> "Connected"
                            GroupSocketManager.ConnectionState.CONNECTING -> "Connecting..."
                            else -> "Offline"
                        },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// ==================== Message Bubble ====================

@Composable
private fun MessageBubble(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    message: GroupMessage,
    isOwnMessage: Boolean,
    showTimestamp: Boolean = true,
    onLongPress: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val context = LocalContext.current
    val bubbleColor = if (isOwnMessage) accentColor else contentColor.copy(alpha = 0.1f)
    val textColor = if (isOwnMessage) Color.White else contentColor
    
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // Avatar for other users
            Box(
                Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onAuthorClick)
            ) {
                if (message.sender.profileImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.sender.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // Sender name (for others)
            if (!isOwnMessage) {
                BasicText(
                    message.sender.name ?: message.sender.username ?: "Unknown",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .clickable(onClick = onAuthorClick)
                        .padding(start = 4.dp, bottom = 2.dp)
                )
            }
            
            // Reply preview
            message.replyTo?.let { reply ->
                Box(
                    Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            contentColor.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        BasicText(
                            reply.sender?.name ?: "Unknown",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        BasicText(
                            reply.content,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
            
            // Message content
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        bubbleColor,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                            bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPress() }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    // Media content
                    if (message.mediaUrl != null) {
                        when (message.contentType) {
                            "image" -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(message.mediaUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (message.content.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            "file" -> {
                                Row(
                                    Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        BasicText(
                                            message.fileName ?: "File",
                                            style = TextStyle(
                                                color = textColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        if (message.fileSize != null) {
                                            BasicText(
                                                formatFileSize(message.fileSize),
                                                style = TextStyle(
                                                    color = textColor.copy(alpha = 0.7f),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                if (message.content.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    
                    // Text content
                    if (message.content.isNotEmpty()) {
                        BasicText(
                            message.content,
                            style = TextStyle(
                                color = textColor,
                                fontSize = 14.sp
                            )
                        )
                    }
                    
                    // Timestamp (only at cluster end — same sender, next within 5 min)
                    if (showTimestamp) {
                        BasicText(
                            formatTime(message.createdAt),
                            style = TextStyle(
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            
            // Reactions
            if (message.reactions.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.groupBy { it.emoji }.forEach { (emoji, reactions) ->
                        Box(
                            Modifier
                                .background(
                                    contentColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "$emoji ${reactions.size}",
                                style = TextStyle(fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
        
        if (isOwnMessage) {
            Spacer(Modifier.width(8.dp))
        }
    }
}

// ==================== Input Bar ====================

@Composable
private fun ChatInputBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Attachment button
        Box(
            Modifier
                .size(40.dp)
                .background(contentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
                .clickable { /* TODO: Add attachment */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = "Add image",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Input field
        Box(
            Modifier
                .weight(1f)
                .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = contentColor, fontSize = 15.sp),
                cursorBrush = SolidColor(accentColor),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            BasicText(
                                "Type a message...",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.4f),
                                    fontSize = 15.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Send button
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (value.isNotBlank() && !isSending) accentColor else contentColor.copy(alpha = 0.2f),
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(enabled = value.isNotBlank() && !isSending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) Color.White else contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== Helper Components ====================

@Composable
private fun DateDivider(contentColor: Color, date: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            date,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun TypingIndicator(
    backdrop: LayerBackdrop,
    contentColor: Color,
    typingUsers: List<GroupSocketManager.GroupTypingEvent>
) {
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassBackground(backdrop)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatars
        typingUsers.take(3).forEach { user ->
            Box(
                Modifier
                    .size(20.dp)
                    .background(contentColor.copy(alpha = 0.1f), CircleShape)
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        BasicText(
            when {
                typingUsers.size == 1 -> "${typingUsers.first().userName ?: "Someone"} is typing..."
                typingUsers.size == 2 -> "${typingUsers[0].userName ?: "Someone"} and ${typingUsers[1].userName ?: "another"} are typing..."
                else -> "Several people are typing..."
            },
            style = TextStyle(
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun ReplyPreview(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    message: GroupMessage,
    onDismiss: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(Modifier.width(8.dp))
        
        Column(Modifier.weight(1f)) {
            BasicText(
                "Replying to ${message.sender.name ?: message.sender.username}",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                message.content,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Icon(
            Icons.Default.Close,
            contentDescription = "Cancel reply",
            tint = contentColor.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDismiss)
        )
    }
}

// ==================== Utility Functions ====================

private fun formatDateHeader(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDate.take(19)) ?: return isoDate
        
        val today = Date()
        val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        
        when {
            todayFormat.format(date) == todayFormat.format(today) -> "Today"
            todayFormat.format(date) == todayFormat.format(Date(today.time - 86400000)) -> "Yesterday"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun formatTime(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDate.take(19)) ?: return ""
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        ""
    }
}

private fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
