package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.models.RewardCard
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val HIDDEN_GEM_CARD_TYPE = "hidden_gem"
private val PremiumCardShape = RoundedCornerShape(30.dp)
private val PremiumPanelShape = RoundedCornerShape(24.dp)
private val RewardHeadlineFontFamily = FontFamily(Font(R.font.kaushan_script))
private val RewardAccentFontFamily = FontFamily(Font(R.font.pacifico))

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

    LaunchedEffect(isTrending) {
        if (isTrending) {
            visible = true
            delay(2000)
            visible = false
        }
    }

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
                            text = message?.takeIf { it.isNotBlank() } ?: "$viewsToday profile views today",
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

enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP,
    NONE
}

@Composable
fun SwipeableRewardCardsOverlay(
    sessionId: String,
    cards: List<RewardCard>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentTheme: String = "light",
    onCardShown: (RewardCard) -> Unit,
    onSkip: (RewardCard) -> Unit,
    onOpenProfile: (RewardCard) -> Unit,
    onConnect: (RewardCard) -> Unit,
    onDismissAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlayColor = remember(currentTheme) {
        when (currentTheme) {
            "light" -> Color.Black.copy(alpha = 0.45f)
            "dark" -> Color.Black.copy(alpha = 0.7f)
            else -> Color.Black.copy(alpha = 0.6f)
        }
    }

    val textColor = remember(currentTheme, contentColor) {
        if (currentTheme == "light") Color(0xFF1A1A1A) else contentColor
    }

    var currentIndex by remember(sessionId) { mutableStateOf(0) }
    var showOverlay by remember(sessionId, cards) { mutableStateOf(cards.isNotEmpty()) }
    var swipePreviewProgress by remember(sessionId) { mutableStateOf(0f) }
    var swipePreviewDirection by remember(sessionId) { mutableStateOf(SwipeDirection.NONE) }
    val remainingCards = (cards.size - currentIndex).coerceAtLeast(0)

    LaunchedEffect(sessionId, cards.map { it.id }.joinToString(separator = "|")) {
        if (cards.isNotEmpty()) {
            cards.forEach(onCardShown)
        }
    }

    LaunchedEffect(currentIndex, cards.size) {
        swipePreviewProgress = 0f
        swipePreviewDirection = SwipeDirection.NONE
        if (currentIndex >= cards.size && cards.isNotEmpty()) {
            delay(250)
            showOverlay = false
        }
    }

    AnimatedVisibility(
        visible = showOverlay && cards.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250)),
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
                    .padding(horizontal = 18.dp)
            ) {
                BasicText(
                    text = "Today's Rewards",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 26.sp,
                        fontFamily = RewardHeadlineFontFamily,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                )

                Spacer(Modifier.height(6.dp))

                BasicText(
                    text = "Swipe left to skip • Swipe right to connect",
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 0.35.sp
                    )
                )

                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            when {
                                cards.size <= 1 -> 232.dp
                                cards.size == 2 -> 254.dp
                                else -> 282.dp
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val visibleCount = minOf(3, cards.size - currentIndex)

                    for (i in (visibleCount - 1) downTo 0) {
                        val cardIndex = currentIndex + i
                        if (cardIndex >= cards.size) continue

                        val card = cards[cardIndex]
                        SwipeableStackedCard(
                            card = card,
                            stackPosition = i,
                            isTopCard = i == 0,
                            backdrop = backdrop,
                            contentColor = textColor,
                            accentColor = accentColor,
                            currentTheme = currentTheme,
                            stackRevealProgress = swipePreviewProgress,
                            stackRevealDirection = swipePreviewDirection,
                            onSwipeStateChange = { direction, progress ->
                                swipePreviewDirection = direction
                                swipePreviewProgress = progress
                            },
                            onSwipe = { swipedCard, direction ->
                                when (direction) {
                                    SwipeDirection.RIGHT -> onConnect(swipedCard)
                                    SwipeDirection.LEFT, SwipeDirection.UP -> onSkip(swipedCard)
                                    SwipeDirection.NONE -> Unit
                                }
                                currentIndex += 1
                            },
                            onClick = { tappedCard -> onOpenProfile(tappedCard) },
                            modifier = Modifier.zIndex((visibleCount - i).toFloat())
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                BasicText(
                    text = "$remainingCards cards remaining",
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                )

                Spacer(Modifier.height(24.dp))

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
                            showOverlay = false
                            onDismissAll()
                        }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        text = "Skip All",
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.35.sp
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
    stackPosition: Int,
    isTopCard: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentTheme: String,
    stackRevealProgress: Float,
    stackRevealDirection: SwipeDirection,
    onSwipeStateChange: (SwipeDirection, Float) -> Unit,
    onSwipe: (RewardCard, SwipeDirection) -> Unit,
    onClick: (RewardCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var offsetX by remember(card.id) { mutableStateOf(0f) }
    var offsetY by remember(card.id) { mutableStateOf(0f) }
    var isDismissing by remember(card.id) { mutableStateOf(false) }
    var dismissDirection by remember(card.id) { mutableStateOf(SwipeDirection.NONE) }
    val swipeThreshold = with(density) { 120.dp.toPx() }
    val isHiddenGem = card.cardType == HIDDEN_GEM_CARD_TYPE

    val animatedOffsetX by animateFloatAsState(
        targetValue = when {
            isDismissing && dismissDirection == SwipeDirection.LEFT -> -1000f
            isDismissing && dismissDirection == SwipeDirection.RIGHT -> 1000f
            else -> offsetX
        },
        animationSpec = if (isDismissing) {
            tween(280, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        },
        finishedListener = {
            if (isDismissing && dismissDirection != SwipeDirection.UP) {
                onSwipe(card, dismissDirection)
            }
        },
        label = "reward_card_offset_x"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isDismissing && dismissDirection == SwipeDirection.UP) -800f else offsetY,
        animationSpec = if (isDismissing) {
            tween(280, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        },
        finishedListener = {
            if (isDismissing && dismissDirection == SwipeDirection.UP) {
                onSwipe(card, dismissDirection)
            }
        },
        label = "reward_card_offset_y"
    )

    val stackScale = 1f - (stackPosition * 0.06f)
    val stackOffsetY = stackPosition * 18f
    val stackOffsetX = stackPosition * 8f
    val stackRotation = stackPosition * 3f
    val horizontalSwipeProgress = (animatedOffsetX.absoluteValue / swipeThreshold).coerceIn(0f, 1f)
    val verticalSwipeProgress = ((-animatedOffsetY) / swipeThreshold).coerceIn(0f, 1f)
    val swipeProgress = maxOf(horizontalSwipeProgress, verticalSwipeProgress)
    val liveSwipeDirection = when {
        animatedOffsetY < -26f && (-animatedOffsetY) > animatedOffsetX.absoluteValue -> SwipeDirection.UP
        animatedOffsetX > 26f -> SwipeDirection.RIGHT
        animatedOffsetX < -26f -> SwipeDirection.LEFT
        else -> SwipeDirection.NONE
    }
    val revealProgress = if (isTopCard) 0f else stackRevealProgress.coerceIn(0f, 1f)
    val revealDirectionSign = when (stackRevealDirection) {
        SwipeDirection.RIGHT -> 1f
        SwipeDirection.LEFT -> -1f
        else -> 0f
    }
    val effectiveStackScale = if (isTopCard) {
        stackScale + swipeProgress * 0.016f
    } else {
        stackScale + revealProgress * (0.048f - stackPosition * 0.012f)
    }
    val effectiveStackOffsetY = if (isTopCard) {
        stackOffsetY - swipeProgress * 6f
    } else {
        stackOffsetY - revealProgress * (18f - stackPosition * 4f)
    }
    val effectiveStackOffsetX = if (isTopCard) {
        stackOffsetX
    } else {
        stackOffsetX - revealDirectionSign * revealProgress * (7f - stackPosition)
    }
    val dragRotation = if (isTopCard) {
        (animatedOffsetX / 22f).coerceIn(-15f, 15f)
    } else {
        stackRotation - revealDirectionSign * revealProgress * 2.2f
    }
    val isSwipingRight = animatedOffsetX > 30
    val isSwipingLeft = animatedOffsetX < -30
    val isSwipingUp = animatedOffsetY < -30
    val highlightColor = if (isHiddenGem) Color(0xFFF1D695) else Color(0xFFE2C16E)
    val frameBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF2DFB0),
            Color(0xFFC9A45C),
            Color(0xFF7A5928),
            Color(0xFFF2DFB0)
        )
    )
    val surfaceBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF163B33),
            Color(0xFF0C211D),
            Color(0xFF174238)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "reward_card_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reward_card_shimmer_offset"
    )

    LaunchedEffect(isTopCard, liveSwipeDirection, swipeProgress, isDismissing, dismissDirection) {
        if (isTopCard) {
            onSwipeStateChange(
                if (isDismissing) dismissDirection else liveSwipeDirection,
                if (isDismissing) 1f else swipeProgress
            )
        }
    }

    Box(
        modifier
            .fillMaxWidth(0.97f)
            .offset {
                IntOffset(
                    (effectiveStackOffsetX + animatedOffsetX).roundToInt(),
                    (effectiveStackOffsetY + animatedOffsetY).roundToInt()
                )
            }
            .graphicsLayer {
                scaleX = effectiveStackScale
                scaleY = effectiveStackScale
                alpha = if (isDismissing) {
                    1f - swipeProgress * 0.35f
                } else {
                    (1f - (stackPosition * 0.12f) + revealProgress * 0.06f).coerceAtMost(1f)
                }
                rotationZ = dragRotation
            }
            .shadow(
                elevation = if (isTopCard) (18f + swipeProgress * 6f).dp else (10 - stackPosition * 2).coerceAtLeast(3).dp,
                shape = PremiumCardShape,
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(PremiumCardShape)
            .background(brush = surfaceBrush, shape = PremiumCardShape)
            .border(width = 2.2.dp, brush = frameBrush, shape = PremiumCardShape)
            .then(
                if (isTopCard) {
                    Modifier.pointerInput(card.id) {
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
            .clickable(enabled = isTopCard && !isDismissing) { onClick(card) }
            .padding(16.dp)
    ) {
        PremiumCardBackground(
            isHiddenGem = isHiddenGem,
            shimmerOffset = shimmerOffset,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, highlightColor.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .clip(PremiumPanelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x2FFCF1CE),
                            Color(0x2618110A),
                            Color(0x300D0A08)
                        )
                    )
                )
                .border(1.dp, highlightColor.copy(alpha = 0.26f), PremiumPanelShape)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            RewardCardContent(
                card = card,
                contentColor = contentColor,
                highlightColor = highlightColor,
                isTopCard = isTopCard,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isTopCard) {
            SwipeFeedbackOverlay(
                direction = liveSwipeDirection,
                progress = swipeProgress,
                modifier = Modifier.fillMaxSize(),
                highlightColor = highlightColor
            )
        }

        if (isTopCard && !isDismissing) {
            AnimatedVisibility(
                visible = isSwipingRight,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
            ) {
                SwipeIndicatorChip(
                    label = "Connect",
                    background = Color(0xFF4CAF50)
                )
            }

            AnimatedVisibility(
                visible = isSwipingLeft,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = (-8).dp)
            ) {
                SwipeIndicatorChip(
                    label = "Skip",
                    background = Color(0xFFE53935)
                )
            }

            AnimatedVisibility(
                visible = isSwipingUp,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp)
            ) {
                SwipeIndicatorChip(
                    label = "Later",
                    background = Color(0xFFC08A2E)
                )
            }
        }

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
private fun SwipeFeedbackOverlay(
    direction: SwipeDirection,
    progress: Float,
    modifier: Modifier = Modifier,
    highlightColor: Color
) {
    if (direction == SwipeDirection.NONE || progress <= 0f) return

    val overlayAlpha = (0.12f + progress * 0.22f).coerceAtMost(0.32f)
    val overlayBrush = when (direction) {
        SwipeDirection.RIGHT -> Brush.horizontalGradient(
            colors = listOf(Color(0xFF7EF0A6).copy(alpha = overlayAlpha), Color.Transparent)
        )
        SwipeDirection.LEFT -> Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0xFFFF867D).copy(alpha = overlayAlpha))
        )
        SwipeDirection.UP -> Brush.verticalGradient(
            colors = listOf(Color.Transparent, highlightColor.copy(alpha = overlayAlpha))
        )
        SwipeDirection.NONE -> Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
    }
    val label = when (direction) {
        SwipeDirection.RIGHT -> "Connect"
        SwipeDirection.LEFT -> "Skip"
        SwipeDirection.UP -> "Later"
        SwipeDirection.NONE -> ""
    }

    Box(
        modifier
            .clip(PremiumCardShape)
            .background(brush = overlayBrush)
            .graphicsLayer { alpha = progress }
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(0f, 3f),
                    blurRadius = 12f
                )
            ),
            modifier = Modifier
                .align(
                    when (direction) {
                        SwipeDirection.RIGHT -> Alignment.CenterStart
                        SwipeDirection.LEFT -> Alignment.CenterEnd
                        SwipeDirection.UP -> Alignment.BottomCenter
                        SwipeDirection.NONE -> Alignment.Center
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SwipeIndicatorChip(
    label: String,
    background: Color
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun RewardCardContent(
    card: RewardCard,
    contentColor: Color,
    highlightColor: Color,
    isTopCard: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = Color(0xFFF1DDA5)
    val bodyColor = Color(0xFFF6EDD0)
    val supportColor = Color(0xFFD0C0A0)
    val accentLabel = card.badge?.takeIf { it.isNotBlank() }
        ?: if (card.cardType == HIDDEN_GEM_CARD_TYPE) "Hidden Gem" else "Selected For You"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        BasicText(
            text = accentLabel,
            style = TextStyle(
                color = highlightColor.copy(alpha = 0.98f),
                fontSize = 13.sp,
                fontFamily = RewardHeadlineFontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.28f),
                    offset = Offset(0f, 3f),
                    blurRadius = 8f
                )
            )
        )

        Spacer(Modifier.height(8.dp))

        PremiumDivider(highlightColor = highlightColor)

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Avatar(
                imageUrl = card.profileImage,
                name = card.name,
                highlightColor = highlightColor
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                BasicText(
                    text = card.name,
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 23.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.25.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(0f, 3f),
                            blurRadius = 12f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                BasicText(
                    text = card.headline?.takeIf { it.isNotBlank() } ?: "Recommended connection",
                    style = TextStyle(
                        color = bodyColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.15.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (card.isOnline) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8CF8A3))
                        )
                    }

                    BasicText(
                        text = card.secondaryMeta,
                        style = TextStyle(
                            color = if (card.isOnline) Color(0xFF8CF8A3) else supportColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.18.sp
                        )
                    )

                    if (!card.badge.isNullOrBlank()) {
                        BasicText(
                            text = "• ${card.badge}",
                            style = TextStyle(
                                color = highlightColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.18.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x2A1B1209))
                .border(1.dp, highlightColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            BasicText(
                text = "✦ ${card.primaryReason}",
                style = TextStyle(
                    color = highlightColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.22.sp
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x281A120B))
                .border(1.dp, highlightColor.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B2B13),
                                Color(0xFF6B5122),
                                Color(0xFF2E2110)
                            )
                        )
                    )
                    .border(1.dp, highlightColor.copy(alpha = 0.85f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                BasicText(
                    text = "⚡ Say Hi",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 13.sp,
                        fontFamily = RewardAccentFontFamily,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            BasicText(
                text = premiumReasonMessage(card.primaryReason),
                style = TextStyle(
                    color = bodyColor.copy(alpha = 0.96f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 15.sp,
                    letterSpacing = 0.14.sp
                ),
                modifier = Modifier.weight(1f)
            )
        }

        if (isTopCard) {
            Spacer(Modifier.height(12.dp))
            PremiumDivider(highlightColor = highlightColor.copy(alpha = 0.75f))
            Spacer(Modifier.height(9.dp))
            BasicText(
                text = "Tap to view profile",
                style = TextStyle(
                    color = supportColor.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.25.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun premiumReasonMessage(primaryReason: String): String {
    val reason = primaryReason.trim()
    if (reason.isBlank()) {
        return "Perfect match for you. Say hi to connect."
    }
    return "$reason. Say hi to connect."
}

@Composable
private fun PremiumCardBackground(
    isHiddenGem: Boolean,
    shimmerOffset: Float,
    modifier: Modifier = Modifier
) {
    val veinColor = if (isHiddenGem) Color(0xD8E6C980) else Color(0xB8BE9D59)
    val accentGlow = if (isHiddenGem) Color(0x3AF4D98E) else Color(0x2E59A48E)
    val ornamentColor = Color(0x44748375)
    val flourishColor = Color(0x38F0D593)
    val warmCenterGlow = Color(0x22F2DEB0)

    Canvas(modifier = modifier) {
        drawCircle(
            color = warmCenterGlow,
            radius = size.minDimension * 0.5f,
            center = Offset(size.width * 0.5f, size.height * 0.48f)
        )
        drawCircle(
            color = accentGlow,
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.18f, size.height * 0.2f)
        )
        drawCircle(
            color = accentGlow.copy(alpha = 0.22f),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.84f, size.height * 0.82f)
        )

        val firstVein = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.1f)
            quadraticBezierTo(
                size.width * 0.35f,
                size.height * 0.2f,
                size.width * 0.48f,
                size.height * 0.45f
            )
            quadraticBezierTo(
                size.width * 0.62f,
                size.height * 0.72f,
                size.width * 0.9f,
                size.height * 0.82f
            )
        }
        drawPath(firstVein, color = veinColor, style = Stroke(width = 2.6.dp.toPx()))

        val secondVein = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.72f)
            quadraticBezierTo(
                size.width * 0.34f,
                size.height * 0.58f,
                size.width * 0.52f,
                size.height * 0.62f
            )
            quadraticBezierTo(
                size.width * 0.76f,
                size.height * 0.68f,
                size.width * 0.88f,
                size.height * 0.48f
            )
        }
        drawPath(secondVein, color = veinColor.copy(alpha = 0.7f), style = Stroke(width = 1.8.dp.toPx()))

        val thirdVein = Path().apply {
            moveTo(size.width * 0.68f, size.height * 0.12f)
            quadraticBezierTo(
                size.width * 0.72f,
                size.height * 0.26f,
                size.width * 0.64f,
                size.height * 0.42f
            )
            quadraticBezierTo(
                size.width * 0.58f,
                size.height * 0.56f,
                size.width * 0.62f,
                size.height * 0.84f
            )
        }
        drawPath(thirdVein, color = veinColor.copy(alpha = 0.56f), style = Stroke(width = 1.4.dp.toPx()))

        val topFlourish = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.16f)
            quadraticBezierTo(
                size.width * 0.32f,
                size.height * 0.06f,
                size.width * 0.44f,
                size.height * 0.14f
            )
            quadraticBezierTo(
                size.width * 0.5f,
                size.height * 0.18f,
                size.width * 0.56f,
                size.height * 0.14f
            )
            quadraticBezierTo(
                size.width * 0.68f,
                size.height * 0.06f,
                size.width * 0.8f,
                size.height * 0.16f
            )
        }
        drawPath(topFlourish, color = flourishColor, style = Stroke(width = 1.5.dp.toPx()))

        val bottomFlourish = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.84f)
            quadraticBezierTo(
                size.width * 0.32f,
                size.height * 0.94f,
                size.width * 0.44f,
                size.height * 0.86f
            )
            quadraticBezierTo(
                size.width * 0.5f,
                size.height * 0.82f,
                size.width * 0.56f,
                size.height * 0.86f
            )
            quadraticBezierTo(
                size.width * 0.68f,
                size.height * 0.94f,
                size.width * 0.8f,
                size.height * 0.84f
            )
        }
        drawPath(bottomFlourish, color = flourishColor.copy(alpha = 0.84f), style = Stroke(width = 1.5.dp.toPx()))

        val shimmerStart = (shimmerOffset % (size.width * 2f)) - size.width
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x24FFF6D7),
                    Color.Transparent
                ),
                start = Offset(shimmerStart, 0f),
                end = Offset(shimmerStart + size.width * 0.45f, size.height)
            )
        )

        val inset = 22.dp.toPx()
        val segment = 18.dp.toPx()
        val dotRadius = 2.dp.toPx()
        val lineWidth = 1.2.dp.toPx()

        drawLine(ornamentColor, Offset(inset, inset + segment), Offset(inset, inset), lineWidth)
        drawLine(ornamentColor, Offset(inset, inset), Offset(inset + segment, inset), lineWidth)
        drawCircle(ornamentColor, dotRadius, Offset(inset + segment + 7.dp.toPx(), inset))
        drawCircle(ornamentColor, dotRadius, Offset(inset, inset + segment + 7.dp.toPx()))

        drawLine(
            ornamentColor,
            Offset(size.width - inset, inset + segment),
            Offset(size.width - inset, inset),
            lineWidth
        )
        drawLine(
            ornamentColor,
            Offset(size.width - inset - segment, inset),
            Offset(size.width - inset, inset),
            lineWidth
        )
        drawCircle(ornamentColor, dotRadius, Offset(size.width - inset - segment - 7.dp.toPx(), inset))
        drawCircle(ornamentColor, dotRadius, Offset(size.width - inset, inset + segment + 7.dp.toPx()))

        drawLine(
            ornamentColor,
            Offset(inset, size.height - inset - segment),
            Offset(inset, size.height - inset),
            lineWidth
        )
        drawLine(
            ornamentColor,
            Offset(inset, size.height - inset),
            Offset(inset + segment, size.height - inset),
            lineWidth
        )
        drawCircle(ornamentColor, dotRadius, Offset(inset + segment + 7.dp.toPx(), size.height - inset))
        drawCircle(ornamentColor, dotRadius, Offset(inset, size.height - inset - segment - 7.dp.toPx()))

        drawLine(
            ornamentColor,
            Offset(size.width - inset, size.height - inset - segment),
            Offset(size.width - inset, size.height - inset),
            lineWidth
        )
        drawLine(
            ornamentColor,
            Offset(size.width - inset - segment, size.height - inset),
            Offset(size.width - inset, size.height - inset),
            lineWidth
        )
        drawCircle(
            ornamentColor,
            dotRadius,
            Offset(size.width - inset - segment - 7.dp.toPx(), size.height - inset)
        )
        drawCircle(
            ornamentColor,
            dotRadius,
            Offset(size.width - inset, size.height - inset - segment - 7.dp.toPx())
        )
    }
}

@Composable
private fun PremiumDivider(highlightColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(highlightColor.copy(alpha = 0.32f))
        )
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(highlightColor.copy(alpha = 0.92f))
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(highlightColor.copy(alpha = 0.32f))
        )
    }
}

@Composable
private fun Avatar(
    imageUrl: String?,
    name: String,
    highlightColor: Color
) {
    Box(
        Modifier
            .size(72.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF4E4BC),
                        highlightColor,
                        Color(0xFF735423)
                    )
                ),
                shape = CircleShape
            )
            .border(1.dp, Color(0xFFF8E8BF), CircleShape)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF183127))
                .border(1.dp, highlightColor.copy(alpha = 0.65f), CircleShape)
                .padding(4.dp)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF356E64)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = name
                            .split(" ")
                            .mapNotNull { token -> token.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                            .ifEmpty { "V" },
                        style = TextStyle(
                            color = Color(0xFFFFF4D7),
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
