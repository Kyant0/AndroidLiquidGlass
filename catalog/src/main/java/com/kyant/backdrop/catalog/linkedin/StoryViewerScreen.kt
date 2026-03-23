package com.kyant.backdrop.catalog.linkedin

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kyant.backdrop.catalog.network.models.Story
import com.kyant.backdrop.catalog.network.models.StoryGroup
import kotlinx.coroutines.launch
import kotlin.math.abs

// Data class for story viewer
data class StoryViewer(
    val id: String,
    val viewedAt: String,
    val user: StoryViewerUser?
)

data class StoryViewerUser(
    val id: String,
    val name: String?,
    val username: String?,
    val profileImage: String?,
    val headline: String?
)

@Composable
fun StoryViewerDialog(
    storyGroups: List<StoryGroup>,
    accentColor: Color,
    initialGroupIndex: Int,
    onDismiss: () -> Unit,
    onStoryViewed: (String) -> Unit,
    onReact: (String, String) -> Unit = { _, _ -> },
    onReply: (String, String) -> Unit = { _, _ -> },
    onGetViewers: ((String, (List<StoryViewer>) -> Unit) -> Unit)? = null,
    onNextGroup: () -> Unit = {},
    onPreviousGroup: () -> Unit = {}
) {
    if (storyGroups.isEmpty()) {
        onDismiss()
        return
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        StoryViewer(
            storyGroups = storyGroups,
            accentColor = accentColor,
            initialGroupIndex = initialGroupIndex,
            onDismiss = onDismiss,
            onStoryViewed = onStoryViewed,
            onReact = onReact,
            onReply = onReply,
            onGetViewers = onGetViewers
        )
    }
}

@Composable
private fun StoryViewer(
    storyGroups: List<StoryGroup>,
    accentColor: Color,
    initialGroupIndex: Int,
    onDismiss: () -> Unit,
    onStoryViewed: (String) -> Unit,
    onReact: (String, String) -> Unit,
    onReply: (String, String) -> Unit,
    onGetViewers: ((String, (List<StoryViewer>) -> Unit) -> Unit)?
) {
    val pagerState = rememberPagerState(
        initialPage = initialGroupIndex.coerceIn(0, storyGroups.size - 1),
        pageCount = { storyGroups.size }
    )
    val scope = rememberCoroutineScope()
    
    // Swipe down to dismiss
    var offsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 300f
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                translationY = offsetY
                alpha = 1f - (abs(offsetY) / dismissThreshold * 0.3f).coerceIn(0f, 0.3f)
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // NOTE: Standard pager behavior - swipe left = next, swipe right = previous
            // This is already correct. User swipes LEFT to go NEXT.
        ) { groupIndex ->
            val storyGroup = storyGroups[groupIndex]
            StoryGroupViewer(
                storyGroup = storyGroup,
                accentColor = accentColor,
                isCurrentGroup = pagerState.currentPage == groupIndex,
                onDismiss = onDismiss,
                onStoryViewed = onStoryViewed,
                onReact = onReact,
                onReply = onReply,
                onGetViewers = onGetViewers,
                onComplete = {
                    // Move to next group or dismiss
                    if (groupIndex < storyGroups.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(groupIndex + 1)
                        }
                    } else {
                        onDismiss()
                    }
                },
                onPreviousGroup = {
                    if (groupIndex > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(groupIndex - 1)
                        }
                    }
                },
                onSwipeDown = { delta ->
                    offsetY = (offsetY + delta).coerceAtLeast(0f)
                },
                onSwipeDownEnd = {
                    if (offsetY > dismissThreshold) {
                        onDismiss()
                    } else {
                        scope.launch {
                            offsetY = 0f
                        }
                    }
                }
            )
        }
        
        // Close button
        Box(
            Modifier
                .padding(16.dp)
                .padding(top = 28.dp)
                .size(32.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "✕",
                style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun StoryGroupViewer(
    storyGroup: StoryGroup,
    accentColor: Color,
    isCurrentGroup: Boolean,
    onDismiss: () -> Unit,
    onStoryViewed: (String) -> Unit,
    onReact: (String, String) -> Unit,
    onReply: (String, String) -> Unit,
    onGetViewers: ((String, (List<StoryViewer>) -> Unit) -> Unit)?,
    onComplete: () -> Unit,
    onPreviousGroup: () -> Unit,
    onSwipeDown: (Float) -> Unit,
    onSwipeDownEnd: () -> Unit
) {
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    val stories = storyGroup.stories
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    if (stories.isEmpty()) {
        onComplete()
        return
    }
    
    val currentStory = stories.getOrNull(currentStoryIndex)
    
    // Progress animation
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Video duration tracking
    var videoDurationMs by remember { mutableLongStateOf(5000L) }
    
    // Story duration (5s for images, actual video duration for videos)
    val storyDurationMs = if (currentStory?.mediaType == "VIDEO") videoDurationMs else 5000L
    
    // Viewers modal state
    var showViewersModal by remember { mutableStateOf(false) }
    var viewers by remember { mutableStateOf<List<StoryViewer>>(emptyList()) }
    var isLoadingViewers by remember { mutableStateOf(false) }
    
    // Reply input state
    var isReplyExpanded by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Horizontal swipe tracking for story navigation
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f
    
    // Preload next story image
    LaunchedEffect(currentStoryIndex) {
        val nextStory = stories.getOrNull(currentStoryIndex + 1)
        if (nextStory?.mediaType == "IMAGE" && !nextStory.mediaUrl.isNullOrEmpty()) {
            val request = ImageRequest.Builder(context)
                .data(nextStory.mediaUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            context.imageLoader.enqueue(request)
        }
    }
    
    // Story progress animation
    LaunchedEffect(currentStoryIndex, isCurrentGroup, isPaused, storyDurationMs, isReplyExpanded) {
        if (isCurrentGroup && currentStory != null && !isPaused && !isReplyExpanded) {
            // Mark story as viewed
            onStoryViewed(currentStory.id)
            
            // Continue from current progress
            val remainingDuration = ((1f - progress.value) * storyDurationMs).toLong()
            if (remainingDuration > 0 && progress.value < 1f) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingDuration.toInt(),
                        easing = LinearEasing
                    )
                )
            }
            
            // Story completed - go to next
            if (progress.value >= 1f) {
                if (currentStoryIndex < stories.size - 1) {
                    currentStoryIndex++
                    progress.snapTo(0f)
                } else {
                    onComplete()
                }
            }
        }
    }
    
    // Reset progress when story changes
    LaunchedEffect(currentStoryIndex) {
        progress.snapTo(0f)
    }
    
    // Focus reply input when expanded
    LaunchedEffect(isReplyExpanded) {
        if (isReplyExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isReplyExpanded) {
                            // Close reply input on tap outside
                            isReplyExpanded = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else {
                            val screenWidth = size.width
                            // Tap LEFT side = PREVIOUS story (Instagram behavior)
                            if (offset.x < screenWidth / 3) {
                                if (currentStoryIndex > 0) {
                                    currentStoryIndex--
                                    scope.launch { progress.snapTo(0f) }
                                } else {
                                    onPreviousGroup()
                                }
                            // Tap RIGHT side = NEXT story (Instagram behavior)
                            } else if (offset.x > screenWidth * 2 / 3) {
                                if (currentStoryIndex < stories.size - 1) {
                                    currentStoryIndex++
                                    scope.launch { progress.snapTo(0f) }
                                } else {
                                    onComplete()
                                }
                            }
                        }
                    },
                    onLongPress = {
                        if (!isReplyExpanded) {
                            isPaused = true
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        isPaused = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Swipe LEFT (negative offset) = NEXT story
                        if (horizontalDragOffset < -swipeThreshold) {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                                scope.launch { progress.snapTo(0f) }
                            } else {
                                onComplete()
                            }
                        }
                        // Swipe RIGHT (positive offset) = PREVIOUS story
                        else if (horizontalDragOffset > swipeThreshold) {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                scope.launch { progress.snapTo(0f) }
                            } else {
                                onPreviousGroup()
                            }
                        }
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragOffset += dragAmount
                    }
                )
            }
    ) {
        // Story content
        currentStory?.let { story ->
            StoryContent(
                story = story,
                isPaused = isPaused || isReplyExpanded,
                onVideoDurationDetected = { durationMs ->
                    if (durationMs > 0) {
                        videoDurationMs = durationMs.coerceAtMost(60000L)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Progress bars at top
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .padding(top = 28.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                val storyProgress = when {
                    index < currentStoryIndex -> 1f
                    index == currentStoryIndex -> progress.value
                    else -> 0f
                }
                
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(storyProgress)
                            .background(Color.White)
                    )
                }
            }
        }
        
        // User info header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 52.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                val profileImage = storyGroup.user.profileImage
                if (!profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    val initials = (storyGroup.user.name ?: storyGroup.user.username ?: "U")
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                    BasicText(
                        initials,
                        style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                BasicText(
                    storyGroup.user.name ?: storyGroup.user.username ?: "User",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
                currentStory?.let { story ->
                    BasicText(
                        formatTimeAgo(story.createdAt),
                        style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                    )
                }
            }
        }
        
        // Bottom section
        currentStory?.let { story ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // For own stories - show eye icon and view count
                if (story.isOwn) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showViewersModal = true
                                isLoadingViewers = true
                                onGetViewers?.invoke(story.id) { viewerList ->
                                    viewers = viewerList
                                    isLoadingViewers = false
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EyeIcon(modifier = Modifier.size(24.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "${story.viewsCount}",
                            style = TextStyle(Color.White, 18.sp, FontWeight.Medium)
                        )
                    }
                } else {
                    // For others' stories - show reply input
                    AnimatedVisibility(
                        visible = !isReplyExpanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        // Collapsed - just show "Reply to story..." placeholder
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .clickable { isReplyExpanded = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicText(
                                "Reply to ${storyGroup.user.name ?: "story"}...",
                                style = TextStyle(Color.White.copy(alpha = 0.6f), 14.sp)
                            )
                        }
                    }
                    
                    // Expanded reply section with reactions
                    AnimatedVisibility(
                        visible = isReplyExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(12.dp)
                        ) {
                            // Quick reactions row
                            LazyRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                val emojis = listOf("❤️", "🔥", "👏", "😍", "😂", "😮", "💡", "🎉")
                                items(emojis) { emoji ->
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .clickable {
                                                onReact(story.id, emoji)
                                                isReplyExpanded = false
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicText(emoji, style = TextStyle(fontSize = 22.sp))
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Text input row
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .focusRequester(focusRequester),
                                    textStyle = TextStyle(Color.White, 14.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (replyText.isNotBlank()) {
                                                onReply(story.id, replyText.trim())
                                                replyText = ""
                                                isReplyExpanded = false
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (replyText.isEmpty()) {
                                                BasicText(
                                                    "Send a message...",
                                                    style = TextStyle(Color.White.copy(alpha = 0.5f), 14.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                
                                Spacer(Modifier.width(8.dp))
                                
                                // Send button
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (replyText.isNotBlank()) accentColor
                                            else Color.White.copy(alpha = 0.2f)
                                        )
                                        .clickable(enabled = replyText.isNotBlank()) {
                                            onReply(story.id, replyText.trim())
                                            replyText = ""
                                            isReplyExpanded = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        "➤",
                                        style = TextStyle(
                                            Color.White, 
                                            18.sp, 
                                            FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Paused indicator
        AnimatedVisibility(
            visible = isPaused && !isReplyExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("⏸", style = TextStyle(Color.White, 32.sp))
            }
        }
    }
    
    // Viewers bottom sheet modal
    if (showViewersModal) {
        ViewersBottomSheet(
            viewers = viewers,
            totalCount = currentStory?.viewsCount ?: 0,
            isLoading = isLoadingViewers,
            onDismiss = { showViewersModal = false }
        )
    }
}

@Composable
private fun EyeIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Clean Material Design style eye icon
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val strokeWidth = width * 0.08f
        
        // Outer eye shape - almond shape
        val eyePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.05f, centerY)
            cubicTo(
                width * 0.25f, height * 0.15f,
                width * 0.75f, height * 0.15f,
                width * 0.95f, centerY
            )
            cubicTo(
                width * 0.75f, height * 0.85f,
                width * 0.25f, height * 0.85f,
                width * 0.05f, centerY
            )
            close()
        }
        
        drawPath(
            path = eyePath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Iris (outer circle)
        drawCircle(
            color = tint,
            radius = width * 0.22f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Pupil (filled circle)
        drawCircle(
            color = tint,
            radius = width * 0.12f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
    }
}

@Composable
private fun ViewersBottomSheet(
    viewers: List<StoryViewer>,
    totalCount: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    // Glass theme colors
    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A2A3A).copy(alpha = 0.95f),
            Color(0xFF1A1A2E).copy(alpha = 0.98f)
        )
    )
    val glassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.1f)
        )
    )
    val accentCyan = Color(0xFF00D4FF)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .border(
                        width = 1.dp,
                        brush = glassBorder,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .background(glassBackground)
                    .clickable(enabled = false) {}
            ) {
                // Handle bar - glass themed
                Box(
                    Modifier
                        .padding(vertical = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accentCyan.copy(alpha = 0.5f), Color.White.copy(alpha = 0.3f))
                            )
                        )
                )
                
                // Header with glass styling
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Improved eye icon with glow effect
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(accentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            EyeIcon(modifier = Modifier.size(20.dp), tint = accentCyan)
                        }
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            "Viewers",
                            style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                        )
                    }
                    // View count badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            "$totalCount",
                            style = TextStyle(accentCyan, 16.sp, FontWeight.SemiBold)
                        )
                    }
                }
                
                // Subtle divider
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Spacer(Modifier.height(8.dp))
                
                if (isLoading) {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Loading spinner indicator
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(accentCyan.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("⏳", style = TextStyle(fontSize = 24.sp))
                            }
                            Spacer(Modifier.height(16.dp))
                            BasicText(
                                "Loading viewers...",
                                style = TextStyle(Color.White.copy(alpha = 0.7f), 14.sp)
                            )
                        }
                    }
                } else if (viewers.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                EyeIcon(modifier = Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.3f))
                            }
                            Spacer(Modifier.height(16.dp))
                            BasicText(
                                "No viewers yet",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 16.sp, FontWeight.Medium)
                            )
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                "Views will appear here when people watch your story",
                                style = TextStyle(Color.White.copy(alpha = 0.3f), 13.sp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        items(viewers) { viewer ->
                            ViewerItem(viewer = viewer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerItem(viewer: StoryViewer) {
    val user = viewer.user ?: return
    val accentCyan = Color(0xFF00D4FF)
    
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image with glass ring
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(accentCyan.copy(alpha = 0.6f), Color.White.copy(alpha = 0.3f))
                    ),
                    shape = CircleShape
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A3A)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.profileImage.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                val initials = (user.name ?: user.username ?: "U")
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                BasicText(
                    initials,
                    style = TextStyle(accentCyan, 16.sp, FontWeight.Bold)
                )
            }
        }
        
        Spacer(Modifier.width(14.dp))
        
        Column(Modifier.weight(1f)) {
            BasicText(
                user.name ?: "Unknown",
                style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                "@${user.username ?: "user"}",
                style = TextStyle(accentCyan.copy(alpha = 0.7f), 13.sp)
            )
        }
        
        // Time badge
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            BasicText(
                formatTimeAgo(viewer.viewedAt),
                style = TextStyle(Color.White.copy(alpha = 0.6f), 11.sp, FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun StoryContent(
    story: Story,
    isPaused: Boolean,
    onVideoDurationDetected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (story.mediaType) {
        "IMAGE" -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(story.mediaUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier.background(Color.Black)
            )
        }
        "VIDEO" -> {
            StoryVideoPlayer(
                videoUrl = story.mediaUrl ?: "",
                isPaused = isPaused,
                onDurationDetected = onVideoDurationDetected,
                modifier = modifier
            )
        }
        "TEXT" -> {
            val bgColor = try {
                Color(android.graphics.Color.parseColor(story.backgroundColor ?: "#1a1a2e"))
            } catch (e: Exception) {
                Color(0xFF1a1a2e)
            }
            
            Box(
                modifier.background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    story.textContent ?: "",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
    
    story.linkUrl?.let { _ ->
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicText(
                    story.linkTitle ?: "See More",
                    style = TextStyle(Color.Black, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun StoryVideoPlayer(
    videoUrl: String,
    isPaused: Boolean,
    onDurationDetected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val durationMs = duration
                        if (durationMs > 0) {
                            onDurationDetected(durationMs)
                        }
                    }
                }
            })
        }
    }
    
    LaunchedEffect(isPaused) {
        exoPlayer.playWhenReady = !isPaused
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = modifier.background(Color.Black)
    )
}

private fun formatTimeAgo(dateString: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        val date = sdf.parse(dateString) ?: return dateString
        val now = System.currentTimeMillis()
        val diff = now - date.time
        
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    } catch (e: Exception) {
        dateString
    }
}
