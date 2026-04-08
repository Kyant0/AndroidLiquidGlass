package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.animation.core.*
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
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.vormexSurface
import com.kyant.backdrop.catalog.network.models.FullComment
import com.kyant.backdrop.catalog.network.models.MentionUser

/**
 * Comments Bottom Sheet - Full comment section with nested replies
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
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (isLightTheme) "light" else "dark"
    )
    val sheetBackground = appearance.sheetColor
    val commentCardBackground = appearance.cardColor
    val chromeSurface = appearance.subtleColor
    val inputBackground = appearance.inputColor
    val surfaceBorderColor = appearance.cardBorderColor
    val dividerColor = appearance.dividerColor
    
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
                .vormexSurface(
                    backdrop = backdrop,
                    tone = VormexSurfaceTone.Sheet,
                    cornerRadius = 28.dp,
                    blurRadius = 32.dp,
                    lensRadius = 16.dp,
                    lensDepth = 32.dp,
                    surfaceColor = sheetBackground,
                    borderColor = appearance.sheetBorderColor
                )
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
                    chromeSurface = chromeSurface,
                    surfaceBorderColor = surfaceBorderColor,
                    dividerColor = dividerColor,
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
                                contentColor = contentColor,
                                accentColor = accentColor,
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chromeSurface)
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
                            .background(chromeSurface)
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
                            .background(commentCardBackground)
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
                    chromeSurface = chromeSurface,
                    inputBackground = inputBackground,
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
    chromeSurface: Color,
    surfaceBorderColor: Color,
    dividerColor: Color,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(surfaceBorderColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "Comments",
                    style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                )
                BasicText(
                    text = "$commentCount ${if (commentCount == 1) "reply" else "replies"}",
                    style = TextStyle(contentColor.copy(alpha = 0.56f), 13.sp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(chromeSurface)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                BasicText("✕", style = TextStyle(contentColor, 18.sp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )
    }
}

@Composable
private fun CommentActionChip(
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(contentColor, 11.sp, FontWeight.Medium)
        )
    }
}

@Composable
private fun ReactionChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = if (isActive) activeColor else contentColor.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun CommentCard(
    comment: FullComment,
    contentColor: Color,
    accentColor: Color,
    currentUserId: String,
    indentLevel: Int,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            Box(
                modifier = Modifier
                    .size(if (indentLevel > 0) 26.dp else 30.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(comment.author.id) }
                    .background(accentColor.copy(alpha = 0.18f)),
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
                            fontSize = if (indentLevel > 0) 8.sp else 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onProfileClick(comment.author.id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        text = comment.author.name ?: "Unknown",
                        style = TextStyle(contentColor, if (indentLevel > 0) 11.sp else 12.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        text = formatTimeAgo(comment.createdAt),
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                    )
                }

                FormattedCommentContent(
                    content = comment.content,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    fontSize = if (indentLevel > 0) 12.sp else 13.sp,
                    lineHeight = if (indentLevel > 0) 17.sp else 18.sp,
                    onMentionClick = { username -> onProfileClick(username) }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReactionChip(
                        label = if (comment.likesCount > 0) {
                            if (comment.isLiked) "❤️ ${comment.likesCount}" else "🤍 ${comment.likesCount}"
                        } else {
                            if (comment.isLiked) "❤️" else "🤍"
                        },
                        isActive = comment.isLiked,
                        activeColor = Color(0xFFe74c3c),
                        contentColor = contentColor,
                        onClick = onLike
                    )
                    CommentActionChip(
                        label = "💬",
                        contentColor = contentColor.copy(alpha = 0.76f),
                        onClick = onReply
                    )
                    if (comment.author.id == currentUserId) {
                        CommentActionChip(
                            label = "🗑️",
                            contentColor = Color.Red.copy(alpha = 0.72f),
                            onClick = onDelete
                        )
                    }
                }
            }
    }
}

@Composable
private fun CommentItem(
    comment: FullComment,
    contentColor: Color,
    accentColor: Color,
    surfaceBorderColor: Color,
    currentUserId: String,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    indentLevel: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 2).dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CommentCard(
            comment = comment,
            contentColor = contentColor,
            accentColor = accentColor,
            currentUserId = currentUserId,
            indentLevel = indentLevel,
            onLike = onLike,
            onReply = onReply,
            onDelete = onDelete,
            onProfileClick = onProfileClick
        )

        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (indentLevel == 0) 14.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comment.replies.forEach { reply ->
                    CommentItem(
                        comment = reply,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        surfaceBorderColor = surfaceBorderColor,
                        currentUserId = currentUserId,
                        onLike = { /* Need to implement for reply */ },
                        onReply = { /* Reply to parent or nested */ },
                        onDelete = { /* Delete reply */ },
                        onProfileClick = onProfileClick,
                        indentLevel = indentLevel + 1
                    )
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
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 18.sp,
    onMentionClick: (String) -> Unit = {},
    onMentionLongPress: (String) -> Unit = {}
) {
    // Use FormattedContent from PostCard which supports mentions
    FormattedContent(
        content = content,
        contentColor = contentColor.copy(alpha = 0.9f),
        accentColor = accentColor,
        fontSize = fontSize,
        lineHeight = lineHeight,
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
    chromeSurface: Color,
    inputBackground: Color,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(chromeSurface)
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
        
        // Text field
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(inputBackground)
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
                    .background(if (isLightTheme) Color.White else Color(0xFF172235))
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
