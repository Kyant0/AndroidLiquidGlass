package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.vormexSurface
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Post Card component - Instagram/LinkedIn style card for displaying posts in feed
 * Features: Double-tap to like, clean action bar, minimalist design
 */
@Composable
fun PostCard(
    post: FullPost,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentUserId: String?,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (String) -> Unit,
    onSave: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onEditPost: (FullPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onReportPost: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onLikesClick: (String) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onImageClick: (String, Int) -> Unit,
    onMentionClick: (String) -> Unit = { username -> onProfileClick(username) },
    modifier: Modifier = Modifier,
    isLightTheme: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    // State for mention profile preview
    var showMentionPreview by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showFullScreenVideo by remember { mutableStateOf(false) }
    var videoUrlToPlay by remember { mutableStateOf("") }
    val isOwner = currentUserId == post.authorId
    
    // Heart animation for double-tap
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            // Animate heart in
            heartAlpha.snapTo(1f)
            heartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            delay(600)
            // Animate heart out
            heartAlpha.animateTo(0f, tween(200))
            heartScale.snapTo(0f)
            showDoubleTapHeart = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Card,
                cornerRadius = 0.dp,
                blurRadius = 20.dp,
                lensRadius = 6.dp,
                lensDepth = 12.dp
            )
    ) {
        Column {
            // Header: Author info with menu (Instagram style - compact)
            PostHeaderInstagram(
                post = post,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = isOwner,
                showMenu = showMenu,
                onMenuToggle = { showMenu = it },
                onProfileClick = { onProfileClick(post.authorId) },
                onEditClick = { onEditPost(post) },
                onDeleteClick = { onDeletePost(post.id) },
                onReportClick = { onReportPost(post.id) },
                onCopyLinkClick = { onCopyLink(post.id) }
            )
            
            // Content with double-tap to like
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!post.isLiked) {
                                    onLike(post.id)
                                }
                                showDoubleTapHeart = true
                            }
                        )
                    }
            ) {
                PostContentInstagram(
                    post = post,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onVotePoll = onVotePoll,
                    onImageClick = { postId, index ->
                        selectedImageIndex = index
                        showImageViewer = true
                        onImageClick(postId, index)
                    },
                    onVideoClick = { videoUrl ->
                        videoUrlToPlay = videoUrl
                        showFullScreenVideo = true
                    }
                )
                
                // Double-tap heart overlay
                if (showDoubleTapHeart) {
                    BasicText(
                        text = "❤️",
                        style = TextStyle(fontSize = 80.sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .scale(heartScale.value)
                            .graphicsLayer { alpha = heartAlpha.value }
                    )
                }
            }
            
            // Instagram-style action bar
            PostActionsInstagram(
                post = post,
                contentColor = contentColor,
                accentColor = accentColor,
                onLike = { onLike(post.id) },
                onComment = { onComment(post.id) },
                onShare = { onShare(post.id) },
                onSave = { onSave(post.id) }
            )
            
            // Engagement stats (Instagram style - below actions)
            EngagementStatsInstagram(
                post = post,
                contentColor = contentColor,
                onLikesClick = { onLikesClick(post.id) }
            )
            
            // Text content below engagement (Instagram style)
            post.content?.let { content ->
                if (content.isNotBlank() && post.type.uppercase() != "POLL") {
                    PostCaptionInstagram(
                        authorName = post.author.name ?: post.author.username ?: "User",
                        content = content,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onProfileClick = { onProfileClick(post.authorId) },
                        onMentionClick = { username -> onMentionClick(username) },
                        onMentionLongPress = { username ->
                            mentionUsername = username
                            showMentionPreview = true
                        }
                    )
                }
            }
            
            // Comment preview
            if (post.commentsCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onComment(post.id) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    BasicText(
                        text = "View all ${post.commentsCount} comments",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    )
                }
            }
            
            // Timestamp (Instagram style)
            BasicText(
                text = formatTimeAgo(post.createdAt).uppercase(),
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    
    // Full screen image viewer dialog
    if (showImageViewer && post.mediaUrls.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            FullScreenImageViewer(
                images = post.mediaUrls,
                initialIndex = selectedImageIndex,
                onDismiss = { showImageViewer = false }
            )
        }
    }
    
    // Full screen video player dialog
    if (showFullScreenVideo && videoUrlToPlay.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenVideo = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            com.kyant.backdrop.catalog.linkedin.FullScreenVideoPlayer(
                videoUrl = videoUrlToPlay,
                onDismiss = { showFullScreenVideo = false }
            )
        }
    }
    
    // Mention profile preview popup (glass theme)
    if (showMentionPreview && mentionUsername.isNotEmpty()) {
        MentionProfilePreviewPopup(
            username = mentionUsername,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { showMentionPreview = false },
            onViewProfile = { 
                showMentionPreview = false
                onMentionClick(mentionUsername)
            }
        )
    }
}

/**
 * Instagram-style compact header
 */
@Composable
private fun PostHeaderInstagram(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onCopyLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image with story-like ring for verified users
        val authorName = post.author.name ?: post.author.username ?: "U"
        val initials = authorName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "U" }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                    )
                )
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!post.author.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.author.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BasicText(
                        text = initials,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = authorName,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable(onClick = onProfileClick)
                )
                // Verified badge placeholder
                post.author.headline?.let {
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = "•",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = it.take(20) + if (it.length > 20) "..." else "",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
        
        // Menu button
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onMenuToggle(true) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "•••",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuToggle(false) }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText("🔗", style = TextStyle(fontSize = 16.sp))
                            Spacer(Modifier.width(12.dp))
                            BasicText("Copy link", style = TextStyle(contentColor, 14.sp))
                        }
                    },
                    onClick = {
                        onMenuToggle(false)
                        onCopyLinkClick()
                    }
                )
                
                if (isOwner) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("✏️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(12.dp))
                                BasicText("Edit", style = TextStyle(contentColor, 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onEditClick()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("🗑️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(12.dp))
                                BasicText("Delete", style = TextStyle(Color(0xFFE53935), 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onDeleteClick()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("⚠️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(12.dp))
                                BasicText("Report", style = TextStyle(Color(0xFFE53935), 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onReportClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Instagram-style action bar (like, comment, share on left, save on right)
 */
@Composable
private fun PostActionsInstagram(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left actions: Like, Comment, Share
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button with animation
            var likePressed by remember { mutableStateOf(false) }
            val likeScale by animateFloatAsState(
                targetValue = if (likePressed) 0.7f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                finishedListener = { likePressed = false },
                label = "likeScale"
            )
            
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(likeScale)
                    .clickable {
                        likePressed = true
                        onLike()
                    },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (post.isLiked) "❤️" else "🤍",
                    style = TextStyle(fontSize = 24.sp)
                )
            }
            
            // Comment button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onComment),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "💬",
                    style = TextStyle(fontSize = 22.sp)
                )
            }
            
            // Share button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "📤",
                    style = TextStyle(fontSize = 22.sp)
                )
            }
        }
        
        // Right: Save button
        var savePressed by remember { mutableStateOf(false) }
        val saveScale by animateFloatAsState(
            targetValue = if (savePressed) 0.7f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            finishedListener = { savePressed = false },
            label = "saveScale"
        )
        
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(saveScale)
                .clickable {
                    savePressed = true
                    onSave()
                },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = if (post.isSaved) "🔖" else "📑",
                style = TextStyle(fontSize = 22.sp)
            )
        }
    }
}

/**
 * Instagram-style engagement stats (likes count, below actions)
 */
@Composable
private fun EngagementStatsInstagram(
    post: FullPost,
    contentColor: Color,
    onLikesClick: () -> Unit
) {
    if (post.likesCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLikesClick)
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = formatLikesCount(post.likesCount),
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Format likes count like Instagram (e.g., "1,234 likes")
 */
private fun formatLikesCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM likes", count / 1_000_000f)
        count >= 1_000 -> String.format("%,d likes", count)
        count == 1 -> "1 like"
        else -> "$count likes"
    }
}

/**
 * Instagram-style caption with author name and clickable mentions
 */
@Composable
private fun PostCaptionInstagram(
    authorName: String,
    content: String,
    contentColor: Color,
    accentColor: Color,
    onProfileClick: () -> Unit,
    onMentionClick: (String) -> Unit = {},
    onMentionLongPress: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val maxLines = if (expanded) Int.MAX_VALUE else 2
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Inline author name + content with color tag parsing
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = contentColor)) {
                append(authorName)
            }
            append(" ")
            // Parse content for color tags and mentions
            appendFormattedContent(content, contentColor)
        }
        
        // Use ClickableText with gesture detection for mentions
        val layoutResult = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        
        BasicText(
            text = annotatedString,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.pointerInput(annotatedString) {
                detectTapGestures(
                    onTap = { offset ->
                        layoutResult.value?.let { result ->
                            val position = result.getOffsetForPosition(offset)
                            val annotations = annotatedString.getStringAnnotations(
                                tag = "MENTION",
                                start = position,
                                end = position
                            )
                            if (annotations.isNotEmpty()) {
                                onMentionClick(annotations.first().item)
                            } else {
                                expanded = !expanded
                            }
                        }
                    },
                    onLongPress = { offset ->
                        layoutResult.value?.let { result ->
                            val position = result.getOffsetForPosition(offset)
                            val annotations = annotatedString.getStringAnnotations(
                                tag = "MENTION",
                                start = position,
                                end = position
                            )
                            if (annotations.isNotEmpty()) {
                                onMentionLongPress(annotations.first().item)
                            }
                        }
                    }
                )
            }
        )
    }
}

/**
 * Instagram-style content (full-width images, cleaner look)
 */
@Composable
private fun PostContentInstagram(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    onVotePoll: (String, String) -> Unit,
    onImageClick: (String, Int) -> Unit,
    onVideoClick: (String) -> Unit = {}
) {
    // Type-specific content
    when (post.type.uppercase()) {
        "IMAGE" -> {
            if (post.mediaUrls.isNotEmpty()) {
                ImageCarouselInstagram(
                    images = post.mediaUrls,
                    onImageClick = { index -> onImageClick(post.id, index) }
                )
            }
        }
        "VIDEO" -> {
            post.videoUrl?.let { videoUrl ->
                VideoContentInstagram(
                    videoUrl = videoUrl,
                    thumbnail = post.videoThumbnail,
                    duration = post.videoDuration,
                    onClick = { onVideoClick(videoUrl) }
                )
            }
        }
        "LINK" -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                post.linkUrl?.let { linkUrl ->
                    LinkPreview(
                        url = linkUrl,
                        title = post.linkTitle,
                        description = post.linkDescription,
                        image = post.linkImage,
                        domain = post.linkDomain,
                        contentColor = contentColor
                    )
                }
            }
        }
        "POLL" -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Show poll question as content (with color/formatting)
                post.content?.let { content ->
                    if (content.isNotBlank()) {
                        FormattedContent(
                            content = content,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                if (post.pollOptions.isNotEmpty()) {
                    PollContentInstagram(
                        postId = post.id,
                        options = post.pollOptions,
                        endsAt = post.pollEndsAt,
                        userVotedOptionId = post.userVotedOptionId,
                        showResultsBeforeVote = post.showResultsBeforeVote,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onVote = { optionId -> onVotePoll(post.id, optionId) }
                    )
                }
            }
        }
        "ARTICLE" -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                ArticleContent(
                    title = post.articleTitle,
                    coverImage = post.articleCoverImage,
                    readTime = post.articleReadTime,
                    tags = post.articleTags,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        "CELEBRATION" -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                post.celebrationType?.let { type ->
                    CelebrationContent(
                        celebrationType = type,
                        contentColor = contentColor
                    )
                }
            }
        }
        "DOCUMENT" -> {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                DocumentContent(
                    documentName = post.documentName,
                    documentType = post.documentType,
                    documentSize = post.documentSize,
                    contentColor = contentColor
                )
            }
        }
        else -> {
            // Text-only post - show as formatted text (colors, etc.)
            post.content?.let { content ->
                if (content.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        FormattedContent(
                            content = content,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Instagram/Twitter style image grid - adapts layout based on image count
 * 1 image: Full width with actual aspect ratio
 * 2-3 images: Equal columns
 * 4 images: 2x2 grid
 * 5-9 images: First image spans 2x2, rest fill grid
 * 10+ images: Show first 9 with "+N" overlay on 9th
 */
@Composable
private fun ImageCarouselInstagram(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    val spacing = 2.dp
    val displayImages = images.take(9)
    val extraCount = (images.size - 9).coerceAtLeast(0)
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        when (images.size) {
            1 -> {
                // Single image - full width with actual aspect ratio
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[0])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image 1",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImageClick(0) }
                )
            }
            2 -> {
                // 2 images side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    images.forEachIndexed { index, url ->
                        GridImageTile(
                            imageUrl = url,
                            contentDescription = "Post image ${index + 1}",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(index) }
                        )
                    }
                }
            }
            3 -> {
                // 3 images: large left, 2 stacked right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    GridImageTile(
                        imageUrl = images[0],
                        contentDescription = "Post image 1",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onImageClick(0) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        GridImageTile(
                            imageUrl = images[1],
                            contentDescription = "Post image 2",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = { onImageClick(1) }
                        )
                        GridImageTile(
                            imageUrl = images[2],
                            contentDescription = "Post image 3",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = { onImageClick(2) }
                        )
                    }
                }
            }
            4 -> {
                // 2x2 grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        GridImageTile(
                            imageUrl = images[0],
                            contentDescription = "Post image 1",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(0) }
                        )
                        GridImageTile(
                            imageUrl = images[1],
                            contentDescription = "Post image 2",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(1) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        GridImageTile(
                            imageUrl = images[2],
                            contentDescription = "Post image 3",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(2) }
                        )
                        GridImageTile(
                            imageUrl = images[3],
                            contentDescription = "Post image 4",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(3) }
                        )
                    }
                }
            }
            else -> {
                // 5+ images: Big first image (2x2), rest in 3-column grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // First row: Big image (2 cols) + 1 small image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        // Big image spanning 2 columns height
                        GridImageTile(
                            imageUrl = displayImages[0],
                            contentDescription = "Post image 1",
                            modifier = Modifier
                                .weight(2f)
                                .aspectRatio(1f),
                            onClick = { onImageClick(0) }
                        )
                        // Right column: 2 stacked images
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            if (displayImages.size > 1) {
                                GridImageTile(
                                    imageUrl = displayImages[1],
                                    contentDescription = "Post image 2",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    onClick = { onImageClick(1) }
                                )
                            }
                            if (displayImages.size > 2) {
                                GridImageTile(
                                    imageUrl = displayImages[2],
                                    contentDescription = "Post image 3",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    onClick = { onImageClick(2) }
                                )
                            }
                        }
                    }
                    
                    // Remaining images in rows of 3
                    if (displayImages.size > 3) {
                        val remainingImages = displayImages.drop(3)
                        remainingImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowImages.forEachIndexed { colIndex, url ->
                                    val imageIndex = 3 + rowIndex * 3 + colIndex
                                    val isLastVisibleImage = imageIndex == 8 && extraCount > 0
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    ) {
                                        GridImageTile(
                                            imageUrl = url,
                                            contentDescription = "Post image ${imageIndex + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            onClick = { onImageClick(imageIndex) }
                                        )
                                        
                                        // "+N" overlay for extra images
                                        if (isLastVisibleImage) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicText(
                                                    text = "+$extraCount",
                                                    style = TextStyle(
                                                        color = Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                // Fill empty spots if row is incomplete
                                repeat(3 - rowImages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable grid image tile with Coil loading
 */
@Composable
private fun GridImageTile(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * Instagram-style video content
 */
@Composable
private fun VideoContentInstagram(
    videoUrl: String,
    thumbnail: String?,
    duration: Int?,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail or placeholder
        if (!thumbnail.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )
        }
        
        // Play button overlay (smaller, more modern)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "▶",
                style = TextStyle(Color.White, 20.sp)
            )
        }
        
        // Duration badge
        duration?.let { dur ->
            val minutes = dur / 60
            val seconds = dur % 60
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                BasicText(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = TextStyle(Color.White, 12.sp, FontWeight.Medium)
                )
            }
        }
    }
}

/**
 * Instagram-style poll with cleaner design
 */
@Composable
private fun PollContentInstagram(
    postId: String,
    options: List<PollOption>,
    endsAt: String?,
    userVotedOptionId: String?,
    showResultsBeforeVote: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: (String) -> Unit
) {
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || showResultsBeforeVote
    val totalVotes = options.sumOf { it.votes }
    val isPollEnded = endsAt?.let { isPollExpired(it) } ?: false
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.id == userVotedOptionId
            val percentage = if (totalVotes > 0) (option.votes.toFloat() / totalVotes * 100) else 0f
            
            val animatedPercentage by animateFloatAsState(
                targetValue = if (showResults) percentage / 100f else 0f,
                animationSpec = tween(500),
                label = "pollPercentage"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(contentColor.copy(alpha = 0.06f))
                    .clickable(enabled = !hasVoted && !isPollEnded) { onVote(option.id) }
            ) {
                // Progress bar
                if (showResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPercentage)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.25f)
                                else contentColor.copy(alpha = 0.08f)
                            )
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            BasicText(
                                text = "✓",
                                style = TextStyle(accentColor, 14.sp, FontWeight.Bold)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        BasicText(
                            text = option.text,
                            style = TextStyle(
                                color = if (isSelected) accentColor else contentColor,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    }
                    
                    if (showResults) {
                        BasicText(
                            text = "${percentage.toInt()}%",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
        
        // Poll info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = "$totalVotes vote${if (totalVotes != 1) "s" else ""}",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
            
            endsAt?.let {
                BasicText(
                    text = if (isPollEnded) "Final results" else "Ends ${formatPollEndTime(it)}",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// ==================== LEGACY COMPONENTS (kept for compatibility) ====================

@Composable
private fun PostHeader(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaved: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image or initials
        val authorName = post.author.name ?: post.author.username ?: "U"
        val initials = authorName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "U" }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.8f))
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            if (!post.author.profileImage.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.author.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture of $authorName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BasicText(
                    text = initials,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = authorName,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            post.author.headline?.let { headline ->
                BasicText(
                    text = headline,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: post.author.username?.let { username ->
                BasicText(
                    text = "@$username",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = formatTimeAgo(post.createdAt),
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )
                
                // Visibility indicator
                val visibilityIcon = when (post.visibility) {
                    "PUBLIC" -> "🌐"
                    "CONNECTIONS" -> "👥"
                    "PRIVATE" -> "🔒"
                    else -> "🌐"
                }
                Spacer(modifier = Modifier.width(4.dp))
                BasicText(
                    text = visibilityIcon,
                    style = TextStyle(fontSize = 10.sp)
                )
            }
        }
        
        // Menu button
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onMenuToggle(true) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "⋮",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuToggle(false) }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText("🔗", style = TextStyle(fontSize = 16.sp))
                            Spacer(Modifier.width(8.dp))
                            BasicText("Copy Link", style = TextStyle(contentColor, 14.sp))
                        }
                    },
                    onClick = {
                        onMenuToggle(false)
                        onCopyLinkClick()
                    }
                )
                
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(if (isSaved) "🔖" else "📑", style = TextStyle(fontSize = 16.sp))
                            Spacer(Modifier.width(8.dp))
                            BasicText(if (isSaved) "Unsave" else "Save", style = TextStyle(contentColor, 14.sp))
                        }
                    },
                    onClick = {
                        onMenuToggle(false)
                        onSaveClick()
                    }
                )
                
                if (isOwner) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("✏️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(8.dp))
                                BasicText("Edit", style = TextStyle(contentColor, 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onEditClick()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("🗑️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(8.dp))
                                BasicText("Delete", style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onDeleteClick()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("⚠️", style = TextStyle(fontSize = 16.sp))
                                Spacer(Modifier.width(8.dp))
                                BasicText("Report", style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp))
                            }
                        },
                        onClick = {
                            onMenuToggle(false)
                            onReportClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostContent(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    onVotePoll: (String, String) -> Unit,
    onImageClick: (String, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Text content with formatting
        post.content?.let { content ->
            if (content.isNotBlank()) {
                FormattedContent(
                    content = content,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        
        // Type-specific content
        when (post.type.uppercase()) {
            "IMAGE" -> {
                if (post.mediaUrls.isNotEmpty()) {
                    ImageCarousel(
                        images = post.mediaUrls,
                        onImageClick = { index -> onImageClick(post.id, index) }
                    )
                }
            }
            "VIDEO" -> {
                post.videoUrl?.let { videoUrl ->
                    VideoContent(
                        videoUrl = videoUrl,
                        thumbnail = post.videoThumbnail,
                        duration = post.videoDuration
                    )
                }
            }
            "LINK" -> {
                post.linkUrl?.let { linkUrl ->
                    LinkPreview(
                        url = linkUrl,
                        title = post.linkTitle,
                        description = post.linkDescription,
                        image = post.linkImage,
                        domain = post.linkDomain,
                        contentColor = contentColor
                    )
                }
            }
            "POLL" -> {
                if (post.pollOptions.isNotEmpty()) {
                    PollContent(
                        postId = post.id,
                        options = post.pollOptions,
                        endsAt = post.pollEndsAt,
                        userVotedOptionId = post.userVotedOptionId,
                        showResultsBeforeVote = post.showResultsBeforeVote,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onVote = { optionId -> onVotePoll(post.id, optionId) }
                    )
                }
            }
            "ARTICLE" -> {
                ArticleContent(
                    title = post.articleTitle,
                    coverImage = post.articleCoverImage,
                    readTime = post.articleReadTime,
                    tags = post.articleTags,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
            "CELEBRATION" -> {
                post.celebrationType?.let { type ->
                    CelebrationContent(
                        celebrationType = type,
                        contentColor = contentColor
                    )
                }
            }
            "DOCUMENT" -> {
                DocumentContent(
                    documentName = post.documentName,
                    documentType = post.documentType,
                    documentSize = post.documentSize,
                    contentColor = contentColor
                )
            }
        }
    }
}

// Regex patterns for formatting
private val COLOR_TAG_REGEX = Regex("\\[color:(#[0-9a-fA-F]{3,8})\\](.*?)\\[/color\\]", RegexOption.DOT_MATCHES_ALL)
private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
private val ITALIC_REGEX = Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")
private val CODE_REGEX = Regex("`([^`]+)`")
private val MENTION_REGEX = Regex("@([a-zA-Z][a-zA-Z0-9_]{2,29})")

// Mention styling color - bold blue
private val MentionBlue = Color(0xFF1E90FF)

/**
 * Sealed class to represent different formatting tokens
 */
private sealed class FormattedToken {
    data class Plain(val text: String) : FormattedToken()
    data class Bold(val text: String) : FormattedToken()
    data class Italic(val text: String) : FormattedToken()
    data class Code(val text: String) : FormattedToken()
    data class Colored(val text: String, val hex: String) : FormattedToken()
    data class Mention(val username: String) : FormattedToken()
}

/**
 * Parse content into tokens with formatting information
 */
private fun parseFormattedContent(content: String): List<FormattedToken> {
    data class Match(val start: Int, val end: Int, val token: FormattedToken)
    
    val matches = mutableListOf<Match>()
    
    // Find all color tags
    COLOR_TAG_REGEX.findAll(content).forEach { match ->
        val hex = match.groupValues[1]
        val innerText = match.groupValues[2]
        matches.add(Match(match.range.first, match.range.last + 1, FormattedToken.Colored(innerText, hex)))
    }
    
    // Find all bold (only if not inside color tag)
    BOLD_REGEX.findAll(content).forEach { match ->
        val overlaps = matches.any { it.start <= match.range.first && it.end >= match.range.last + 1 }
        if (!overlaps) {
            matches.add(Match(match.range.first, match.range.last + 1, FormattedToken.Bold(match.groupValues[1])))
        }
    }
    
    // Find all italic (only if not inside other formatting)
    ITALIC_REGEX.findAll(content).forEach { match ->
        val overlaps = matches.any { 
            (match.range.first >= it.start && match.range.first < it.end) ||
            (match.range.last >= it.start && match.range.last < it.end)
        }
        if (!overlaps) {
            matches.add(Match(match.range.first, match.range.last + 1, FormattedToken.Italic(match.groupValues[1])))
        }
    }
    
    // Find all code (only if not inside other formatting)
    CODE_REGEX.findAll(content).forEach { match ->
        val overlaps = matches.any { 
            (match.range.first >= it.start && match.range.first < it.end) ||
            (match.range.last >= it.start && match.range.last < it.end)
        }
        if (!overlaps) {
            matches.add(Match(match.range.first, match.range.last + 1, FormattedToken.Code(match.groupValues[1])))
        }
    }
    
    // Find all @mentions (only if not inside other formatting)
    MENTION_REGEX.findAll(content).forEach { match ->
        val overlaps = matches.any { 
            (match.range.first >= it.start && match.range.first < it.end) ||
            (match.range.last >= it.start && match.range.last < it.end)
        }
        if (!overlaps) {
            matches.add(Match(match.range.first, match.range.last + 1, FormattedToken.Mention(match.groupValues[1])))
        }
    }
    
    // Sort by start position
    matches.sortBy { it.start }
    
    // Build final token list with plain text between matches
    val tokens = mutableListOf<FormattedToken>()
    var currentIndex = 0
    
    for (match in matches) {
        if (match.start > currentIndex) {
            tokens.add(FormattedToken.Plain(content.substring(currentIndex, match.start)))
        }
        tokens.add(match.token)
        currentIndex = match.end
    }
    
    if (currentIndex < content.length) {
        tokens.add(FormattedToken.Plain(content.substring(currentIndex)))
    }
    
    return tokens
}

/**
 * Helper extension to append content with all formatting parsed
 */
private fun AnnotatedString.Builder.appendFormattedContent(content: String, defaultColor: Color) {
    val tokens = parseFormattedContent(content)
    
    for (token in tokens) {
        when (token) {
            is FormattedToken.Plain -> {
                withStyle(SpanStyle(color = defaultColor)) { append(token.text) }
            }
            is FormattedToken.Bold -> {
                withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Bold)) { append(token.text) }
            }
            is FormattedToken.Italic -> {
                withStyle(SpanStyle(color = defaultColor, fontStyle = FontStyle.Italic)) { append(token.text) }
            }
            is FormattedToken.Code -> {
                withStyle(SpanStyle(
                    color = defaultColor.copy(alpha = 0.9f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = defaultColor.copy(alpha = 0.1f)
                )) { append(token.text) }
            }
            is FormattedToken.Colored -> {
                try {
                    val spanColor = Color(android.graphics.Color.parseColor(token.hex))
                    withStyle(SpanStyle(color = spanColor)) { append(token.text) }
                } catch (_: Exception) {
                    withStyle(SpanStyle(color = defaultColor)) { append(token.text) }
                }
            }
            is FormattedToken.Mention -> {
                // Bold blue mention with clickable annotation
                pushStringAnnotation(tag = "MENTION", annotation = token.username)
                withStyle(SpanStyle(
                    color = MentionBlue,
                    fontWeight = FontWeight.Bold
                )) { append("@${token.username}") }
                pop()
            }
        }
    }
}

@Composable
fun FormattedContent(
    content: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp,
    onMentionClick: (String) -> Unit = {},
    onMentionLongPress: (String) -> Unit = {}
) {
    val annotatedString = buildAnnotatedString {
        appendFormattedContent(content, contentColor)
    }
    
    val layoutResult = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    BasicText(
        text = annotatedString,
        style = TextStyle(
            color = contentColor,
            fontSize = fontSize,
            lineHeight = lineHeight
        ),
        onTextLayout = { layoutResult.value = it },
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures(
                onTap = { offset ->
                    layoutResult.value?.let { result ->
                        val position = result.getOffsetForPosition(offset)
                        val annotations = annotatedString.getStringAnnotations(
                            tag = "MENTION",
                            start = position,
                            end = position
                        )
                        if (annotations.isNotEmpty()) {
                            onMentionClick(annotations.first().item)
                        }
                    }
                },
                onLongPress = { offset ->
                    layoutResult.value?.let { result ->
                        val position = result.getOffsetForPosition(offset)
                        val annotations = annotatedString.getStringAnnotations(
                            tag = "MENTION",
                            start = position,
                            end = position
                        )
                        if (annotations.isNotEmpty()) {
                            onMentionLongPress(annotations.first().item)
                        }
                    }
                }
            )
        }
    )
}

@Composable
private fun ImageCarousel(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    if (images.size == 1) {
        // Single image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(images.first())
                .crossfade(true)
                .build(),
            contentDescription = "Post image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(0) }
        )
    } else {
        // Multiple images - carousel
        var selectedIndex by remember { mutableIntStateOf(0) }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Main image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(selectedIndex) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[selectedIndex])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image ${selectedIndex + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Counter badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    BasicText(
                        text = "${selectedIndex + 1}/${images.size}",
                        style = TextStyle(Color.White, 12.sp, FontWeight.Medium)
                    )
                }
            }
            
            // Thumbnails
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                images.forEachIndexed { index, url ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == selectedIndex) Color.White.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { selectedIndex = index }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (index == selectedIndex) 2.dp else 0.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoContent(
    videoUrl: String,
    thumbnail: String?,
    duration: Int?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail or placeholder
        if (!thumbnail.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Play button overlay
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "▶",
                style = TextStyle(Color.White, 24.sp)
            )
        }
        
        // Duration badge
        duration?.let { dur ->
            val minutes = dur / 60
            val seconds = dur % 60
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = TextStyle(Color.White, 12.sp)
                )
            }
        }
    }
}

@Composable
private fun LinkPreview(
    url: String,
    title: String?,
    description: String?,
    image: String?,
    domain: String?,
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .clickable { /* Open URL */ }
    ) {
        // Preview image
        if (!image.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image)
                    .crossfade(true)
                    .build(),
                contentDescription = "Link preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
        
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Domain
            domain?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )
            }
            
            // Title
            title?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Description
            description?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PollContent(
    postId: String,
    options: List<PollOption>,
    endsAt: String?,
    userVotedOptionId: String?,
    showResultsBeforeVote: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: (String) -> Unit
) {
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || showResultsBeforeVote
    val totalVotes = options.sumOf { it.votes }
    val isPollEnded = endsAt?.let { isPollExpired(it) } ?: false
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.id == userVotedOptionId
            val percentage = if (totalVotes > 0) (option.votes.toFloat() / totalVotes * 100) else 0f
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentColor.copy(alpha = 0.08f))
                    .clickable(enabled = !hasVoted && !isPollEnded) { onVote(option.id) }
            ) {
                // Progress bar (when showing results)
                if (showResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(44.dp)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.3f)
                                else contentColor.copy(alpha = 0.1f)
                            )
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            BasicText(
                                text = "✓",
                                style = TextStyle(accentColor, 14.sp, FontWeight.Bold)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        BasicText(
                            text = option.text,
                            style = TextStyle(
                                color = if (isSelected) accentColor else contentColor,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    }
                    
                    if (showResults) {
                        BasicText(
                            text = "${percentage.toInt()}%",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
        
        // Poll info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = "$totalVotes votes",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            )
            
            endsAt?.let {
                BasicText(
                    text = if (isPollEnded) "Poll ended" else "Ends ${formatPollEndTime(it)}",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ArticleContent(
    title: String?,
    coverImage: String?,
    readTime: Int?,
    tags: List<String>,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
    ) {
        // Cover image
        if (!coverImage.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverImage)
                    .crossfade(true)
                    .build(),
                contentDescription = "Article cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
        
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            title?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            // Read time
            readTime?.let {
                BasicText(
                    text = "$it min read",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
            
            // Tags
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                text = tag,
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationContent(
    celebrationType: String,
    contentColor: Color
) {
    val celebration = CelebrationType.entries.find { it.name == celebrationType }
        ?: CelebrationType.NEW_JOB
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.2f),
                        Color(0xFFA855F7).copy(alpha = 0.2f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText(
                text = celebration.emoji,
                style = TextStyle(fontSize = 32.sp)
            )
            BasicText(
                text = celebration.label,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun DocumentContent(
    documentName: String?,
    documentType: String?,
    documentSize: Long?,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Document icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "📄",
                style = TextStyle(fontSize = 24.sp)
            )
        }
        
        Column {
            BasicText(
                text = documentName ?: "Document",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                documentType?.let {
                    BasicText(
                        text = it.uppercase(),
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }
                documentSize?.let {
                    BasicText(
                        text = formatFileSize(it),
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EngagementStats(
    post: FullPost,
    contentColor: Color,
    onLikesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Likes with reaction summary
        Row(
            modifier = Modifier.clickable(onClick = onLikesClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show top reactions
            if (post.reactionSummary.isNotEmpty()) {
                post.reactionSummary.take(3).forEach { reaction ->
                    val icon = when (reaction.type) {
                        "LIKE" -> "👍"
                        "CELEBRATE" -> "🎉"
                        "SUPPORT" -> "❤️"
                        "INSIGHTFUL" -> "💡"
                        "CURIOUS" -> "❓"
                        else -> "👍"
                    }
                    BasicText(text = icon, style = TextStyle(fontSize = 14.sp))
                }
                Spacer(Modifier.width(4.dp))
            } else if (post.likesCount > 0) {
                BasicText(text = "👍", style = TextStyle(fontSize = 14.sp))
                Spacer(Modifier.width(4.dp))
            }
            
            if (post.likesCount > 0) {
                BasicText(
                    text = "${post.likesCount}",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
        }
        
        // Comments and shares
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (post.commentsCount > 0) {
                BasicText(
                    text = "${post.commentsCount} comments",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
            if (post.sharesCount > 0) {
                BasicText(
                    text = "${post.sharesCount} shares",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PostActions(
    post: FullPost,
    contentColor: Color,
    accentColor: Color,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Like button
        ActionButton(
            icon = if (post.isLiked) "👍" else "👍",
            label = "Like",
            isActive = post.isLiked,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = onLike
        )
        
        // Comment button
        ActionButton(
            icon = "💬",
            label = "Comment",
            isActive = false,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = onComment
        )
        
        // Share button
        ActionButton(
            icon = "📤",
            label = "Share",
            isActive = false,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = onShare
        )
        
        // Save button
        ActionButton(
            icon = if (post.isSaved) "🔖" else "📑",
            label = if (post.isSaved) "Saved" else "Save",
            isActive = post.isSaved,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = onSave
        )
    }
}

@Composable
private fun ActionButton(
    icon: String,
    label: String,
    isActive: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = icon,
            style = TextStyle(fontSize = 16.sp)
        )
        Spacer(Modifier.width(4.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = if (isActive) accentColor else contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

// ==================== Skeleton Loading ====================

@Composable
fun PostCardSkeleton(
    backdrop: LayerBackdrop,
    isLightTheme: Boolean
) {
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
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    val shimmer = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 300f, translateAnimation.value - 300f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
            }
            
            // Content skeleton
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (it == 2) 0.7f else if (it == 1) 0.9f else 1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
            }
            
            // Image skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer)
            )
            
            // Stats skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isLightTheme) Color.LightGray.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f))
            )
            
            // Actions skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmer)
                    )
                }
            }
        }
    }
}

// ==================== Helper Functions ====================

fun formatTimeAgo(dateString: String): String {
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        )
        
        var date: Date? = null
        for (format in formats) {
            format.timeZone = TimeZone.getTimeZone("UTC")
            try {
                date = format.parse(dateString)
                if (date != null) break
            } catch (e: Exception) {
                continue
            }
        }
        
        if (date == null) return dateString
        
        val now = Date()
        val diff = now.time - date.time
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        
        when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            weeks < 4 -> "${weeks}w"
            else -> SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}

fun isPollExpired(endsAt: String): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val endDate = format.parse(endsAt)
        endDate?.before(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}

fun formatPollEndTime(endsAt: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val endDate = format.parse(endsAt) ?: return endsAt
        
        val now = Date()
        val diff = endDate.time - now.time
        
        if (diff <= 0) return "now"
        
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        
        when {
            hours < 1 -> "in ${diff / (1000 * 60)}m"
            hours < 24 -> "in ${hours}h"
            else -> "in ${days}d"
        }
    } catch (e: Exception) {
        endsAt
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Glass-themed profile preview popup for @mentions
 * Shows a short preview of the mentioned user's profile on long-press
 * With smooth scale + fade enter/exit animations
 */
@Composable
fun MentionProfilePreviewPopup(
    username: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf<FullProfileResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "popup_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "popup_alpha",
        finishedListener = { alpha ->
            if (alpha == 0f) {
                onDismiss()
            }
        }
    )
    
    // Trigger enter animation on launch
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Handle dismiss with exit animation
    val dismissWithAnimation = {
        isVisible = false
    }
    
    // Fetch profile by username
    LaunchedEffect(username) {
        isLoading = true
        error = null
        com.kyant.backdrop.catalog.network.ApiClient.getProfile(context, username)
            .onSuccess { response ->
                profile = response
                isLoading = false
            }
            .onFailure { e ->
                error = e.message ?: "Failed to load profile"
                isLoading = false
            }
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = dismissWithAnimation,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        // Clear glass - minimal blur for transparency
                        blur(4f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    onDrawSurface = {
                        // Higher alpha for clear glass effect
                        drawRect(Color.White.copy(alpha = 0.35f))
                    }
                )
                .clip(RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            // Close button (X) at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f))
                    .clickable { dismissWithAnimation() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "✕",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BasicText(
                            text = "Loading @$username...",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BasicText(
                            text = "😕",
                            style = TextStyle(fontSize = 40.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicText(
                            text = "User not found",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        BasicText(
                            text = "@$username",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                
                profile != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        // Profile header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile image
                            val user = profile!!.user
                            val initials = (user.name ?: user.username ?: "U")
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .take(2)
                                .joinToString("")
                                .ifEmpty { "U" }
                            
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatar.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(user.avatar)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    BasicText(
                                        text = initials,
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                // Name
                                BasicText(
                                    text = user.name ?: user.username ?: "User",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // Username
                                BasicText(
                                    text = "@${user.username}",
                                    style = TextStyle(
                                        color = MentionBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                // Headline
                                user.headline?.let { headline ->
                                    BasicText(
                                        text = headline,
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem(
                                count = profile!!.stats.connectionsCount,
                                label = "Connections",
                                contentColor = contentColor
                            )
                            ProfileStatItem(
                                count = profile!!.stats.followersCount,
                                label = "Followers",
                                contentColor = contentColor
                            )
                            ProfileStatItem(
                                count = profile!!.stats.totalPosts,
                                label = "Posts",
                                contentColor = contentColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // View Profile button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor)
                                .clickable { 
                                    dismissWithAnimation()
                                    // Small delay to let animation start, then navigate
                                    kotlinx.coroutines.MainScope().launch {
                                        kotlinx.coroutines.delay(150)
                                        onViewProfile()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "View Profile",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    count: Int,
    label: String,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = formatCount(count),
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
