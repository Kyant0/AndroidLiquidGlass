package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

/**
 * Full screen image viewer with swipe navigation and zoom/pan support
 */
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        pageCount = { images.size }
    )
    // When the visible page is zoomed, pager swipe is disabled so horizontal swipes pan the image instead.
    var currentPageScale by remember { mutableFloatStateOf(1f) }
    
    // Preload adjacent images
    val context = LocalContext.current
    LaunchedEffect(pagerState.currentPage) {
        // Preload next and previous images
        val preloadIndices = listOf(
            pagerState.currentPage - 1,
            pagerState.currentPage + 1,
            pagerState.currentPage + 2
        ).filter { it in images.indices }
        
        preloadIndices.forEach { index ->
            val request = ImageRequest.Builder(context)
                .data(images[index])
                .size(Size.ORIGINAL)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            coil.ImageLoader(context).enqueue(request)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1, // Preload 1 page on each side
            userScrollEnabled = currentPageScale <= ZoomEpsilon
        ) { page ->
            // Stable slot + URL so pager recycling always shows the correct image at natural size.
            key(page, images[page]) {
                ZoomableImage(
                    imageUrl = images[page],
                    pageIndex = page,
                    currentPageIndex = pagerState.currentPage,
                    onCurrentPageScaleChanged = { currentPageScale = it }
                )
            }
        }
        
        // Top bar with close button and counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "✕",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            // Image counter
            if (images.size > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        // Page indicators at bottom
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                images.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

private const val ZoomEpsilon = 1.01f

/** Float noise when comparing [PointerEvent.calculateZoom] to 1f. */
private const val ZoomGestureEpsilon = 1e-3f

private const val RotationGestureEpsilon = 1e-4f

/**
 * Like [androidx.compose.foundation.gestures.detectTransformGestures], but the stock implementation
 * treats **single-finger** movement past touch slop as a transform and consumes it. That blocks
 * [HorizontalPager] horizontal swipes. Here, single-finger pan only counts toward slop when
 * [isZoomed] is true or when the gesture is clearly pinch/multi-touch.
 */
private suspend fun PointerInputScope.detectTransformGesturesUnlessPagerSwipe(
    isZoomed: () -> Boolean,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()
                val pointerCount = event.changes.count { it.pressed }

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    val pinchOrMultiTouch =
                        pointerCount >= 2 ||
                            abs(zoomChange - 1f) > ZoomGestureEpsilon ||
                            abs(rotationChange) > RotationGestureEpsilon

                    val panSlopAllowed = isZoomed() || pinchOrMultiTouch

                    if (
                        zoomMotion > touchSlop ||
                            rotationMotion > touchSlop ||
                            (panSlopAllowed && panMotion > touchSlop)
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = false
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}

/**
 * Pinch to zoom; one-finger drag pans only while zoomed.
 * At 1x scale, horizontal swipes reach [HorizontalPager] (custom slop rules; tap-to-dismiss removed).
 *
 * Opens with [ContentScale.Fit] so the **whole image** fits on screen. Using [ContentScale.None]
 * at intrinsic pixel size made large photos look cropped/zoomed because they are bigger than the display.
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    pageIndex: Int,
    currentPageIndex: Int,
    onCurrentPageScaleChanged: (Float) -> Unit
) {
    // New composition per URL: fresh zoom/pan (pager recycling safe).
    key(imageUrl) {
        // 1f = fit-to-screen baseline ([ContentScale.Fit]). Pinch multiplies from there.
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        SideEffect {
            if (pageIndex == currentPageIndex) {
                onCurrentPageScaleChanged(scale)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                // Pinch / two-finger pan; at 1x, single-finger swipes are NOT consumed (see detector).
                .pointerInput(Unit) {
                    detectTransformGesturesUnlessPagerSwipe(
                        isZoomed = { scale > ZoomEpsilon }
                    ) { _, pan, zoom, _ ->
                        val raw = (scale * zoom).coerceIn(0.25f, 6f)
                        if (raw <= ZoomEpsilon) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = raw
                            offset += pan
                        }
                    }
                }
                // One-finger pan only while zoomed (pager handles one-finger swipe at 1x).
                .pointerInput(scale) {
                    if (scale > ZoomEpsilon) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .size(Size.ORIGINAL)
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center
            ) {
                val state = painter.state

                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

/**
 * Simple loading indicator
 */
@Composable
private fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple circular loading animation
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }
        BasicText(
            text = "Loading...",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        )
    }
}
