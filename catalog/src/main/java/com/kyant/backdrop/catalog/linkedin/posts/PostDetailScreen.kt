package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.network.models.*

/**
 * Post Detail Screen - Full post view with comments
 * Route: /post/{id} or navigation with postId
 */
@Composable
fun PostDetailScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    post: FullPost?,
    comments: List<FullComment>,
    isLoadingPost: Boolean,
    isLoadingComments: Boolean,
    isLoadingMoreComments: Boolean,
    hasMoreComments: Boolean,
    currentUserId: String,
    currentUserName: String,
    currentUserAvatar: String?,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    isSendingComment: Boolean,
    error: String?,
    autoOpenComments: Boolean = false,
    highlightCommentId: String? = null,
    onBack: () -> Unit,
    onLikePost: () -> Unit,
    onCommentPost: () -> Unit,
    onSharePost: () -> Unit,
    onSavePost: () -> Unit,
    onVotePoll: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onReplyComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    onSendComment: (String, String?) -> Unit,
    onSearchMentions: (String) -> Unit,
    onClearMentionSearch: () -> Unit,
    onClearError: () -> Unit,
    onUserClick: (String) -> Unit,
    onEditPost: () -> Unit,
    onDeletePost: () -> Unit,
    onReportPost: () -> Unit
) {
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )
    var showCommentsSheet by remember { mutableStateOf(autoOpenComments) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            PostDetailTopBar(
                contentColor = contentColor,
                onBack = onBack
            )
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (appearance.isGlassTheme) {
                            appearance.subtleColor.copy(alpha = 0.55f)
                        } else {
                            appearance.backgroundColor
                        }
                    ),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Post content
                item {
                    if (isLoadingPost && post == null) {
                        PostCardSkeleton(backdrop, true)
                    } else if (post != null) {
                        PostCard(
                            post = post,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            currentUserId = currentUserId,
                            onLike = { _ -> onLikePost() },
                            onComment = { _ -> showCommentsSheet = true },
                            onShare = { _ -> onSharePost() },
                            onSave = { _ -> onSavePost() },
                            onProfileClick = { _ -> onUserClick(post.author.id) },
                            onEditPost = { _ -> onEditPost() },
                            onDeletePost = { _ -> onDeletePost() },
                            onReportPost = { _ -> onReportPost() },
                            onCopyLink = { _ -> /* TODO */ },
                            onLikesClick = { _ -> /* TODO */ },
                            onVotePoll = { _, optionId -> onVotePoll(optionId) },
                            onImageClick = { _, _ -> /* TODO */ },
                            isLightTheme = appearance.isLightTheme
                        )
                    }
                }
                
                // Error message
                if (error != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    text = error,
                                    style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp),
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onClearError() }
                                        .padding(4.dp)
                                ) {
                                    BasicText("✕", style = TextStyle(Color.Red, 16.sp))
                                }
                            }
                        }
                    }
                }
                
                // Comments section header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "Comments (${comments.size})",
                            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                        )
                        
                        if (hasMoreComments && !isLoadingMoreComments) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onLoadMoreComments)
                                    .padding(8.dp)
                            ) {
                                BasicText(
                                    text = "View all",
                                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
                
                // Comments preview (first few)
                if (isLoadingComments && comments.isEmpty()) {
                    items(2) {
                        CommentPreviewSkeleton(contentColor)
                    }
                } else {
                    comments.take(3).forEach { comment ->
                        item(key = comment.id) {
                            CommentPreviewItem(
                                comment = comment,
                                isHighlighted = comment.id == highlightCommentId,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                onLike = { onLikeComment(comment.id) },
                                onReply = { showCommentsSheet = true },
                                onClick = { showCommentsSheet = true }
                            )
                        }
                    }
                }
                
                // Load more / View all comments button
                if (comments.size > 3 || hasMoreComments) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .clickable { showCommentsSheet = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "View all ${comments.size} comments",
                                style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                // Empty comments state
                if (comments.isEmpty() && !isLoadingComments) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                BasicText("💬", style = TextStyle(fontSize = 40.sp))
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicText(
                                    text = "No comments yet",
                                    style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                                )
                                BasicText(
                                    text = "Be the first to share your thoughts",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 12.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating comment input
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(appearance.sheetColor)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(appearance.inputColor)
                    .clickable { showCommentsSheet = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                BasicText(
                    text = "Add a comment...",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                )
            }
        }
        
        // Comments bottom sheet
        if (showCommentsSheet) {
            CommentsBottomSheet(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                postId = post?.id ?: "",
                comments = comments,
                isLoading = isLoadingComments,
                isLoadingMore = isLoadingMoreComments,
                isSendingComment = isSendingComment,
                hasMoreComments = hasMoreComments,
                currentUserAvatar = currentUserAvatar,
                currentUserName = currentUserName,
                mentionSearchResults = mentionSearchResults,
                isSearchingMentions = isSearchingMentions,
                error = error,
                onDismiss = { showCommentsSheet = false },
                onLoadMore = onLoadMoreComments,
                onSendComment = onSendComment,
                onLikeComment = onLikeComment,
                onDeleteComment = onDeleteComment,
                onSearchMentions = onSearchMentions,
                onClearMentionSearch = onClearMentionSearch,
                onClearError = onClearError
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailTopBar(
    contentColor: Color,
    onBack: () -> Unit
) {
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )

    TopAppBar(
        title = {
            BasicText(
                text = "Post",
                style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.08f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BasicText("←", style = TextStyle(contentColor, 20.sp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = appearance.sheetColor
        )
    )
}

@Composable
private fun CommentPreviewItem(
    comment: FullComment,
    isHighlighted: Boolean,
    contentColor: Color,
    accentColor: Color,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHighlighted) accentColor.copy(alpha = 0.1f)
                else contentColor.copy(alpha = 0.04f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
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
                val initials = comment.author.name?.split(" ")
                    ?.mapNotNull { it.firstOrNull()?.uppercase() }
                    ?.take(2)
                    ?.joinToString("") ?: "?"
                BasicText(
                    text = initials,
                    style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        text = comment.author.name ?: "Unknown",
                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        text = formatTimeAgo(comment.createdAt),
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
                
                BasicText(
                    text = comment.content,
                    style = TextStyle(contentColor.copy(alpha = 0.85f), 13.sp),
                    maxLines = 2
                )
                
                // Quick actions
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onLike)
                            .padding(4.dp),
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
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onReply)
                            .padding(4.dp)
                    ) {
                        BasicText(
                            text = "Reply",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentPreviewSkeleton(contentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                )
            }
        }
    }
}

/**
 * Post Not Found Error State
 */
@Composable
fun PostNotFoundState(
    contentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText("😕", style = TextStyle(fontSize = 64.sp))
            BasicText(
                text = "Post not found",
                style = TextStyle(contentColor, 20.sp, FontWeight.SemiBold)
            )
            BasicText(
                text = "This post may have been deleted or is not accessible",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.1f))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = "Go Back",
                    style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                )
            }
        }
    }
}
