package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.network.models.FullComment
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.shapes.RoundedRectangle

/**
 * Comments Bottom Sheet - Full comment section with nested replies (Glass Theme)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean = true,
    postId: String,
    comments: List<FullComment>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isSendingComment: Boolean,
    hasMoreComments: Boolean,
    currentUserAvatar: String?,
    currentUserName: String,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onSendComment: (content: String, parentId: String?) -> Unit,
    onLikeComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSearchMentions: (String) -> Unit,
    onClearMentionSearch: () -> Unit,
    onClearError: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    val sheetSurfaceTop = if (isLightTheme) Color.White.copy(alpha = 0.78f) else Color(0xFF0F1724).copy(alpha = 0.9f)
    val sheetSurfaceBottom = if (isLightTheme) accentColor.copy(alpha = 0.08f) else accentColor.copy(alpha = 0.18f)
    val glassBubbleBackground = if (isLightTheme) Color.White.copy(alpha = 0.72f) else Color(0xFF182233).copy(alpha = 0.82f)
    val surfaceBorderColor = if (isLightTheme) Color.White.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.12f)
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<FullComment?>(null) }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var selectedMentions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(28f.dp) },
                    effects = {
                        vibrancy()
                        blur(32f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    sheetSurfaceTop,
                                    sheetSurfaceBottom,
                                    sheetSurfaceTop.copy(alpha = if (isLightTheme) 0.72f else 0.86f)
                                )
                            )
                        )
                    }
                )
                .border(1.dp, surfaceBorderColor, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Header
                CommentsHeader(
                    commentCount = comments.size,
                    contentColor = contentColor,
                    glassBubbleBackground = glassBubbleBackground,
                    surfaceBorderColor = surfaceBorderColor,
                    onClose = onDismiss
                )
                
                // Error message
                error?.let { errorMsg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicText(
                                text = errorMsg,
                                style = TextStyle(Color.Red.copy(alpha = 0.8f), 13.sp),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onClearError() }
                                    .padding(4.dp)
                            ) {
                                BasicText("✕", style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp))
                            }
                        }
                    }
                }
                
                // Comments list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoading && comments.isEmpty()) {
                        items(3) {
                            CommentSkeletonAnimated(isLightTheme = isLightTheme)
                        }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                glassBubbleBackground = glassBubbleBackground,
                                isLightTheme = isLightTheme,
                                surfaceBorderColor = surfaceBorderColor,
                                currentUserId = "", // Would need actual user ID
                                onLike = { onLikeComment(comment.id) },
                                onReply = { replyingTo = comment },
                                onDelete = { onDeleteComment(comment.id) },
                                onProfileClick = onProfileClick
                            )
                        }
                        
                        // Load more indicator
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = accentColor
                                    )
                                }
                            }
                        } else if (hasMoreComments) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onLoadMore)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        text = "Load more comments",
                                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                        
                        if (comments.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        BasicText("💬", style = TextStyle(fontSize = 48.sp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        BasicText(
                                            text = "No comments yet",
                                            style = TextStyle(contentColor.copy(alpha = 0.6f), 16.sp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        BasicText(
                                            text = "Be the first to comment",
                                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Reply indicator
                replyingTo?.let { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(contentColor.copy(alpha = 0.04f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(
                            text = "Replying to ${comment.author.name}",
                            style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { replyingTo = null }
                                .padding(4.dp)
                        ) {
                            BasicText("✕", style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp))
                        }
                    }
                }
                
                // Mention suggestions
                if (showMentionDropdown && mentionSearchResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.06f))
                            .padding(8.dp)
                    ) {
                        Column {
                            if (isSearchingMentions) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            } else {
                                mentionSearchResults.take(5).forEach { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val lastAtIndex = commentText.lastIndexOf('@')
                                                if (lastAtIndex >= 0) {
                                                    commentText = commentText.substring(0, lastAtIndex) + "@${user.username} "
                                                    selectedMentions = selectedMentions + user.id
                                                }
                                                showMentionDropdown = false
                                                onClearMentionSearch()
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!user.avatar.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(user.avatar)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                BasicText(
                                                    text = user.name?.firstOrNull()?.uppercase() ?: "?",
                                                    style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                                                )
                                            }
                                        }
                                        Column {
                                            BasicText(
                                                text = user.name ?: "Unknown",
                                                style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                                            )
                                            user.username?.let {
                                                BasicText(
                                                    text = "@$it",
                                                    style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Comment input
                CommentInput(
                    value = commentText,
                    onValueChange = { newText ->
                        commentText = newText
                        // Check for @mention
                        val lastAtIndex = newText.lastIndexOf('@')
                        if (lastAtIndex >= 0) {
                            val textAfterAt = newText.substring(lastAtIndex + 1)
                            val spaceIndex = textAfterAt.indexOf(' ')
                            val query = if (spaceIndex >= 0) null else textAfterAt
                            if (query != null && query.length >= 2) {
                                showMentionDropdown = true
                                onSearchMentions(query)
                            } else {
                                showMentionDropdown = false
                                onClearMentionSearch()
                            }
                        }
                    },
                    userAvatar = currentUserAvatar,
                    userName = currentUserName,
                    isSending = isSendingComment,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    glassBubbleBackground = glassBubbleBackground,
                    surfaceBorderColor = surfaceBorderColor,
                    onSend = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText, replyingTo?.id)
                            commentText = ""
                            selectedMentions = emptyList()
                            replyingTo = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentsHeader(
    commentCount: Int,
    contentColor: Color,
    glassBubbleBackground: Color,
    surfaceBorderColor: Color,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = "Comments ($commentCount)",
            style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
        )
        
        // Glass close button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(glassBubbleBackground)
                .border(1.dp, surfaceBorderColor, CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            BasicText("✕", style = TextStyle(contentColor, 18.sp))
        }
    }
}

@Composable
private fun CommentItem(
    comment: FullComment,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBubbleBackground: Color,
    isLightTheme: Boolean,
    surfaceBorderColor: Color,
    currentUserId: String,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    indentLevel: Int = 0,
    showConnectingLine: Boolean = false,
    isLastReply: Boolean = false
) {
    val context = LocalContext.current
    val lineColor = contentColor.copy(alpha = 0.15f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 28).dp)
    ) {
        // Connecting line column for nested comments
        if (indentLevel > 0 && showConnectingLine) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(IntrinsicSize.Min)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 2.dp.toPx()
                    // Vertical line from top
                    drawLine(
                        color = lineColor,
                        start = Offset(12.dp.toPx(), 0f),
                        end = Offset(12.dp.toPx(), if (isLastReply) size.height / 3 else size.height),
                        strokeWidth = strokeWidth
                    )
                    // Horizontal line to avatar
                    drawLine(
                        color = lineColor,
                        start = Offset(12.dp.toPx(), size.height / 3),
                        end = Offset(24.dp.toPx(), size.height / 3),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar - clickable to view profile
            Box(
                modifier = Modifier
                    .size(if (indentLevel > 0) 32.dp else 40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(comment.author.id) }
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!comment.author.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(comment.author.profileImage)
                            .build(),
                        contentDescription = "${comment.author.name}'s avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initials = comment.author.name?.split(" ")
                        ?.mapNotNull { it.firstOrNull()?.uppercase() }
                        ?.take(2)
                        ?.joinToString("") ?: "?"
                    BasicText(
                        text = initials,
                        style = TextStyle(
                            Color.White,
                            fontSize = if (indentLevel > 0) 10.sp else 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                // Comment bubble with glass effect using drawBackdrop
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(18f.dp) },
                            effects = {
                                vibrancy()
                                blur(10f.dp.toPx())
                                lens(8f.dp.toPx(), 14f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isLightTheme) {
                                                Color.White.copy(alpha = if (indentLevel > 0) 0.62f else 0.74f)
                                            } else {
                                                glassBubbleBackground.copy(alpha = if (indentLevel > 0) 0.82f else 0.9f)
                                            },
                                            accentColor.copy(alpha = if (indentLevel > 0) 0.05f else 0.08f)
                                        )
                                    )
                                )
                            }
                        )
                        .border(1.dp, surfaceBorderColor, RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onProfileClick(comment.author.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicText(
                                text = comment.author.name ?: "Unknown",
                                style = TextStyle(contentColor, if (indentLevel > 0) 13.sp else 14.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                text = formatTimeAgo(comment.createdAt),
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                        
                        // Comment content with formatting
                        FormattedCommentContent(
                            content = comment.content,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onMentionClick = { username -> onProfileClick(username) }
                        )
                    }
                }
                
                // Actions
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onLike)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicText(
                            text = if (comment.isLiked) "❤️" else "🤍",
                            style = TextStyle(fontSize = 12.sp)
                        )
                        if (comment.likesCount > 0) {
                            BasicText(
                                text = "${comment.likesCount}",
                                style = TextStyle(
                                    color = if (comment.isLiked) Color(0xFFe74c3c) else contentColor.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    
                    // Reply
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onReply)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            text = "Reply",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                        )
                    }
                    
                    // Delete (only for own comments)
                    if (comment.author.id == currentUserId) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                text = "Delete",
                                style = TextStyle(Color.Red.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                // Nested replies with connecting lines
                if (comment.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comment.replies.forEachIndexed { index, reply ->
                            CommentItem(
                                comment = reply,
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                glassBubbleBackground = glassBubbleBackground,
                                isLightTheme = isLightTheme,
                                surfaceBorderColor = surfaceBorderColor,
                                currentUserId = currentUserId,
                                onLike = { /* Need to implement for reply */ },
                                onReply = { /* Reply to parent or nested */ },
                                onDelete = { /* Delete reply */ },
                                onProfileClick = onProfileClick,
                                indentLevel = indentLevel + 1,
                                showConnectingLine = true,
                                isLastReply = index == comment.replies.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedCommentContent(
    content: String,
    contentColor: Color,
    accentColor: Color,
    onMentionClick: (String) -> Unit = {},
    onMentionLongPress: (String) -> Unit = {}
) {
    // Use FormattedContent from PostCard which supports mentions
    FormattedContent(
        content = content,
        contentColor = contentColor.copy(alpha = 0.9f),
        accentColor = accentColor,
        onMentionClick = onMentionClick,
        onMentionLongPress = onMentionLongPress
    )
}

@Composable
private fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    userAvatar: String?,
    userName: String,
    isSending: Boolean,
    contentColor: Color,
    accentColor: Color,
    glassBubbleBackground: Color,
    surfaceBorderColor: Color,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(glassBubbleBackground.copy(alpha = 0.4f))
            .border(1.dp, surfaceBorderColor.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!userAvatar.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(userAvatar).build(),
                    contentDescription = "Your avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initials = userName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                BasicText(
                    text = initials,
                    style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                )
            }
        }
        
        // Glass text field
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(glassBubbleBackground.copy(alpha = 0.88f))
                .border(1.dp, surfaceBorderColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = false,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "Write a comment...",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        // Send button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (value.isNotBlank() && !isSending) accentColor
                    else contentColor.copy(alpha = 0.2f)
                )
                .clickable(enabled = value.isNotBlank() && !isSending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    text = "➤",
                    style = TextStyle(
                        color = if (value.isNotBlank()) Color.White else contentColor.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun CommentSkeletonAnimated(isLightTheme: Boolean) {
    // Shimmer colors based on theme
    val shimmerColors = if (isLightTheme) {
        listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            Color.DarkGray.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.5f),
            Color.DarkGray.copy(alpha = 0.3f)
        )
    }
    
    // Animation
    val transition = rememberInfiniteTransition(label = "comment_shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "comment_shimmer_translate"
    )
    
    val shimmer = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 300f, translateAnimation.value - 300f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar skeleton with shimmer
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(shimmer)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Glass bubble skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isLightTheme) Color.White.copy(alpha = 0.5f)
                        else Color(0xFF252538).copy(alpha = 0.5f)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Name skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    
                    // Content skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
            }
            
            // Actions skeleton
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
            }
        }
    }
}
