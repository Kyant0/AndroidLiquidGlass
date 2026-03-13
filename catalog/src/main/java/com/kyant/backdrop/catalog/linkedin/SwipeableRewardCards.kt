package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.HiddenGemUser
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ==================== Trending Banner (Auto-dismiss after 2 seconds) ====================

@Composable
fun TrendingBannerAutoHide(
    isTrending: Boolean,
    rank: Int?,
    viewsToday: Int,
    message: String?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    // Show for 2 seconds then auto-hide
    LaunchedEffect(isTrending) {
        if (isTrending) {
            visible = true
            delay(2000) // Show for 2 seconds
            visible = false
        }
    }
    
    // Bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn() + scaleIn(initialScale = 0.8f),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    }
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B).copy(alpha = 0.25f),
                            Color(0xFFFFE66D).copy(alpha = 0.2f),
                            Color(0xFF4ECDC4).copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated fire icon
                Box(Modifier.offset(y = (-bounce).dp)) {
                    BasicText(
                        text = "🔥",
                        style = TextStyle(fontSize = 28.sp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "You're Trending Today!",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (rank != null) {
                            BasicText(
                                text = "#$rank",
                                style = TextStyle(
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            BasicText(
                                text = " • ",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        BasicText(
                            text = "$viewsToday profile views today",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== Stacked Reward Cards Overlay ====================

data class RewardCard(
    val id: String,
    val type: RewardCardType,
    val user: Any?, // DailyMatchUser or HiddenGemUser
    val message: String = ""
)

enum class RewardCardType {
    DAILY_MATCH,
    HIDDEN_GEM
}

enum class SwipeDirection {
    LEFT, RIGHT, UP, NONE
}

@Composable
fun SwipeableRewardCardsOverlay(
    dailyMatches: List<DailyMatchUser>,
    hiddenGem: HiddenGemUser?,
    hiddenGemMessage: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentTheme: String = "glass",
    onMatchClick: (String) -> Unit,
    onHiddenGemConnect: () -> Unit,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val (backgroundColor, cardBackgroundColor, overlayColor) = remember(currentTheme) {
        when (currentTheme) {
            "light" -> Triple(
                Color.White,
                Color(0xFFF5F5F5),
                Color.Black.copy(alpha = 0.5f)
            )
            "dark" -> Triple(
                Color(0xFF1A1A1A),
                Color(0xFF2D2D2D),
                Color.Black.copy(alpha = 0.7f)
            )
            else -> Triple( // glass
                Color.Transparent,
                Color.White.copy(alpha = 0.1f),
                Color.Black.copy(alpha = 0.6f)
            )
        }
    }
    
    val textColor = remember(currentTheme) {
        when (currentTheme) {
            "light" -> Color(0xFF1A1A1A)
            else -> Color.White
        }
    }
    
    // Build list of cards
    val cards = remember(dailyMatches, hiddenGem) {
        val list = mutableListOf<RewardCard>()
        
        // Add daily matches first (will be on top)
        dailyMatches.take(3).forEachIndexed { index, match ->
            list.add(RewardCard(
                id = "match_$index",
                type = RewardCardType.DAILY_MATCH,
                user = match,
                message = when (index) {
                    0 -> "Perfect match for you!"
                    1 -> "Similar interests detected"
                    else -> "High reply rate!"
                }
            ))
        }
        
        // Hidden gem at end (will be at bottom)
        if (hiddenGem != null) {
            list.add(RewardCard(
                id = "hidden_gem",
                type = RewardCardType.HIDDEN_GEM,
                user = hiddenGem,
                message = hiddenGemMessage
            ))
        }
        
        list
    }
    
    // Current card index (0 = top card)
    var currentIndex by remember { mutableStateOf(0) }
    var showOverlay by remember { mutableStateOf(cards.isNotEmpty()) }
    
    val remainingCards = cards.size - currentIndex
    
    // Auto-dismiss overlay when all cards are swiped
    LaunchedEffect(currentIndex) {
        if (currentIndex >= cards.size && cards.isNotEmpty()) {
            delay(300)
            showOverlay = false
            onDismissAll()
        }
    }
    
    AnimatedVisibility(
        visible = showOverlay && cards.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(overlayColor)
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Header
                BasicText(
                    text = "✨ Today's Rewards",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(Modifier.height(6.dp))
                
                BasicText(
                    text = "Swipe left to skip • Swipe right to connect",
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                )
                
                Spacer(Modifier.height(32.dp))
                
                // Stacked cards container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Render visible cards (max 3 visible at once)
                    val visibleCount = minOf(3, cards.size - currentIndex)
                    
                    for (i in (visibleCount - 1) downTo 0) {
                        val cardIndex = currentIndex + i
                        if (cardIndex < cards.size) {
                            val card = cards[cardIndex]
                            val isTopCard = i == 0
                            
                            SwipeableStackedCard(
                                card = card,
                                stackPosition = i,
                                isTopCard = isTopCard,
                                backdrop = backdrop,
                                contentColor = if (currentTheme == "light") Color(0xFF1A1A1A) else contentColor,
                                accentColor = accentColor,
                                cardBackgroundColor = cardBackgroundColor,
                                currentTheme = currentTheme,
                                onSwipe = { direction ->
                                    when (direction) {
                                        SwipeDirection.RIGHT -> {
                                            // Connect action
                                            when (card.type) {
                                                RewardCardType.DAILY_MATCH -> {
                                                    (card.user as? DailyMatchUser)?.id?.let { onMatchClick(it) }
                                                }
                                                RewardCardType.HIDDEN_GEM -> {
                                                    onHiddenGemConnect()
                                                }
                                            }
                                        }
                                        SwipeDirection.LEFT, SwipeDirection.UP -> {
                                            // Skip - just move to next
                                        }
                                        SwipeDirection.NONE -> {}
                                    }
                                    currentIndex++
                                },
                                onClick = {
                                    when (card.type) {
                                        RewardCardType.DAILY_MATCH -> {
                                            (card.user as? DailyMatchUser)?.id?.let { onMatchClick(it) }
                                        }
                                        RewardCardType.HIDDEN_GEM -> {
                                            (card.user as? HiddenGemUser)?.id?.let { onMatchClick(it) }
                                        }
                                    }
                                },
                                modifier = Modifier.zIndex((visibleCount - i).toFloat())
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Card counter
                BasicText(
                    text = "$remainingCards cards remaining",
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                )
                
                Spacer(Modifier.height(24.dp))
                
                // Skip all button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            when (currentTheme) {
                                "light" -> Color(0xFFE0E0E0)
                                "dark" -> Color(0xFF3D3D3D)
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                        )
                        .clickable { 
                            currentIndex = cards.size
                        }
                        .padding(horizontal = 36.dp, vertical = 14.dp)
                ) {
                    BasicText(
                        text = "Skip All",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableStackedCard(
    card: RewardCard,
    stackPosition: Int, // 0 = top, 1 = second, etc.
    isTopCard: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    cardBackgroundColor: Color,
    currentTheme: String,
    onSwipe: (SwipeDirection) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Drag state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var dismissDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    
    val swipeThreshold = with(density) { 120.dp.toPx() }
    
    // Animated values
    val animatedOffsetX by animateFloatAsState(
        targetValue = when {
            isDismissing && dismissDirection == SwipeDirection.LEFT -> -1000f
            isDismissing && dismissDirection == SwipeDirection.RIGHT -> 1000f
            else -> offsetX
        },
        animationSpec = if (isDismissing) {
            tween(300, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        },
        finishedListener = {
            if (isDismissing) {
                onSwipe(dismissDirection)
            }
        },
        label = "offsetX"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = when {
            isDismissing && dismissDirection == SwipeDirection.UP -> -800f
            else -> offsetY
        },
        animationSpec = if (isDismissing) {
            tween(300, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        },
        finishedListener = {
            if (isDismissing && dismissDirection == SwipeDirection.UP) {
                onSwipe(dismissDirection)
            }
        },
        label = "offsetY"
    )
    
    // Stack visual effects - MORE VISIBLE STACKING
    val stackScale = 1f - (stackPosition * 0.06f)
    val stackOffsetY = stackPosition * 20f // More vertical offset to see cards behind
    val stackOffsetX = stackPosition * 8f // Slight horizontal offset
    val stackRotation = stackPosition * 3f // Slight rotation for fanned effect
    val stackAlpha = 1f - (stackPosition * 0.2f)
    
    // Card rotation based on drag
    val dragRotation = if (isTopCard) (animatedOffsetX / 20f).coerceIn(-15f, 15f) else stackRotation
    
    // Swipe indicators
    val swipeProgress = (animatedOffsetX.absoluteValue / swipeThreshold).coerceIn(0f, 1f)
    val isSwipingRight = animatedOffsetX > 30
    val isSwipingLeft = animatedOffsetX < -30
    
    val isGold = card.type == RewardCardType.HIDDEN_GEM
    
    // Shimmer animation for gold cards
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier
            .fillMaxWidth(0.92f)
            .offset { IntOffset(
                (stackOffsetX + animatedOffsetX).roundToInt(),
                (stackOffsetY + animatedOffsetY).roundToInt()
            )}
            .graphicsLayer {
                scaleX = stackScale
                scaleY = stackScale
                alpha = if (isDismissing) {
                    (1f - swipeProgress).coerceAtLeast(0f)
                } else {
                    stackAlpha
                }
                rotationZ = dragRotation
                // Slight 3D effect
                rotationY = if (isTopCard) animatedOffsetX / 40f else 0f
            }
            .then(
                if (isTopCard && !isDismissing) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > swipeThreshold -> {
                                        isDismissing = true
                                        dismissDirection = SwipeDirection.RIGHT
                                    }
                                    offsetX < -swipeThreshold -> {
                                        isDismissing = true
                                        dismissDirection = SwipeDirection.LEFT
                                    }
                                    offsetY < -swipeThreshold -> {
                                        isDismissing = true
                                        dismissDirection = SwipeDirection.UP
                                    }
                                    else -> {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        )
                    }
                } else Modifier
            )
            .shadow(
                elevation = if (isTopCard) 12.dp else (8 - stackPosition * 2).coerceAtLeast(2).dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .then(
                when {
                    isGold -> {
                        Modifier
                            .background(
                                when (currentTheme) {
                                    "light" -> Color(0xFFFFF8E1)
                                    "dark" -> Color(0xFF3D3520)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (currentTheme == "glass") {
                                    Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(24f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(24f.dp.toPx())
                                        }
                                    )
                                } else Modifier
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = if (currentTheme == "glass") 0.35f else 0.2f),
                                        Color(0xFFFFA500).copy(alpha = if (currentTheme == "glass") 0.3f else 0.15f),
                                        Color(0xFFFFD700).copy(alpha = if (currentTheme == "glass") 0.25f else 0.1f)
                                    ),
                                    start = Offset(shimmerOffset, 0f),
                                    end = Offset(shimmerOffset + 500f, 400f)
                                )
                            )
                    }
                    currentTheme == "glass" -> {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(24f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(20f.dp.toPx())
                                }
                            )
                            .background(Color.White.copy(alpha = 0.15f))
                    }
                    currentTheme == "light" -> {
                        Modifier.background(Color.White)
                    }
                    else -> { // dark
                        Modifier.background(Color(0xFF2D2D2D))
                    }
                }
            )
            .clickable(enabled = isTopCard && !isDismissing) { onClick() }
            .padding(20.dp)
    ) {
        // Swipe direction indicators (only on top card)
        if (isTopCard && !isDismissing) {
            // Connect indicator (right swipe)
            AnimatedVisibility(
                visible = isSwipingRight,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = "✓ Connect",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            // Skip indicator (left swipe)
            AnimatedVisibility(
                visible = isSwipingLeft,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = (-8).dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = "✗ Skip",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        
        // Card content
        when (card.type) {
            RewardCardType.DAILY_MATCH -> DailyMatchStackedContent(
                user = card.user as DailyMatchUser,
                message = card.message,
                contentColor = contentColor,
                accentColor = accentColor,
                currentTheme = currentTheme,
                isTopCard = isTopCard
            )
            RewardCardType.HIDDEN_GEM -> HiddenGemStackedContent(
                user = card.user as HiddenGemUser,
                message = card.message,
                contentColor = contentColor,
                accentColor = accentColor,
                currentTheme = currentTheme,
                isTopCard = isTopCard
            )
        }
        
        // Swipe hint handle on top card
        if (isTopCard && offsetX == 0f && offsetY == 0f) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(contentColor.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun DailyMatchStackedContent(
    user: DailyMatchUser,
    message: String,
    contentColor: Color,
    accentColor: Color,
    currentTheme: String,
    isTopCard: Boolean
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with online indicator
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.isOnline) 
                            Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))
                        else 
                            Brush.linearGradient(listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.2f)))
                    )
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImage ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = user.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(14.dp))
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "🎯",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        text = user.name ?: "Match",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isOnline) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "Online",
                            style = TextStyle(
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                if (user.headline != null) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = user.headline,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(Modifier.height(14.dp))
        
        // Bottom row with message and reply rate
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Match message
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicText(
                    text = message,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Reply rate
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = "⚡",
                    style = TextStyle(fontSize = 14.sp)
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = "${user.replyRate}%",
                    style = TextStyle(
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BasicText(
                    text = " reply",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                )
            }
        }
        
        // Tap hint for top card
        if (isTopCard) {
            Spacer(Modifier.height(12.dp))
            BasicText(
                text = "Tap to view profile →",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun HiddenGemStackedContent(
    user: HiddenGemUser,
    message: String,
    contentColor: Color,
    accentColor: Color,
    currentTheme: String,
    isTopCard: Boolean
) {
    Column {
        // Premium badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "💎",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        text = "WEEKLY GEM",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            BasicText(
                text = "✨ Premium Match",
                style = TextStyle(
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // User info
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Gold bordered avatar
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700))
                        )
                    )
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImage ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = user.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(14.dp))
            
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = user.name ?: "Hidden Gem",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (user.isOnline) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
                
                if (user.headline != null) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = user.headline,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Gold reply rate
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "⚡",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    BasicText(
                        text = " ${user.replyRate}% reply rate",
                        style = TextStyle(
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        Spacer(Modifier.height(14.dp))
        
        // Message
        BasicText(
            text = message.ifEmpty { "A highly connected professional just for you!" },
            style = TextStyle(
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        )
        
        // Tap hint for top card
        if (isTopCard) {
            Spacer(Modifier.height(10.dp))
            BasicText(
                text = "Tap to view profile →",
                style = TextStyle(
                    color = Color(0xFFFFD700).copy(alpha = 0.7f),
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}


