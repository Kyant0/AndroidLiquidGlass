package com.kyant.glassmusic

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.drawHighlight
import com.kyant.backdrop.highlight.onDrawSurfaceWithHighlight
import com.kyant.backdrop.rememberLayerBackdrop
import com.kyant.backdrop.shadow.backdropShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun <T> BottomTabs(
    tabs: List<T>,
    selectedTabState: MutableState<T>,
    backdrop: Backdrop,
    background: Color,
    modifier: Modifier = Modifier,
    content: BottomTabsScope.(tab: T) -> BottomTabsScope.BottomTab
) {
    val bottomTabsBackdrop = rememberLayerBackdrop(null)

    val density = LocalDensity.current
    val height = 64.dp
    val padding = 4.dp
    val paddingPx = with(density) { padding.roundToPx() }
    val contentColor = Color.Black
    val itemBackground = Color(0xFF64B5F6)

    val scope = remember { BottomTabsScope() }
    val animationScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier
            .height(height)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures {}
            }
    ) {
        val widthWithoutPaddings =
            (constraints.maxWidth.toFloat() - paddingPx * 2f).fastCoerceAtLeast(0f)
        val tabWidth = widthWithoutPaddings / tabs.size
        val maxWidth = (widthWithoutPaddings - tabWidth).fastCoerceAtLeast(0f)

        val dragFraction by animateFloatAsState(
            if (isDragging) 1f else 0f,
            spring(1f, 300f)
        )

        Row(
            Modifier
                .backdrop(bottomTabsBackdrop)
                .graphicsLayer {
                    val maxScale = 1f + 4.dp / height
                    val scale = lerp(1f, maxScale, dragFraction)
                    scaleX = scale
                    scaleY = scale
                }
                .backdropShadow(CircleShape)
                .drawBackdrop(backdrop) {
                    shape = CircleShape
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        saturation()
                        blur(2f.dp)
                        refraction(height = 12f.dp.toPx(), amount = size.minDimension / 2f)
                    }
                    onDrawBackdrop { drawBackdrop ->
                        val maxScale = 1f + 4.dp / height
                        val scale = lerp(1f, maxScale, dragFraction)
                        scale(1f / scale, 1f / scale, Offset.Zero) {
                            drawBackdrop()
                        }
                    }
                    onDrawSurfaceWithHighlight { drawRect(Color.White.copy(alpha = 0.3f)) }
                }
                .fillMaxSize()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                key(tab) {
                    val itemBackgroundAlpha by animateFloatAsState(
                        if (selectedTabState.value == tab && !isDragging) {
                            0.8f
                        } else {
                            0f
                        },
                        spring(0.8f, 200f)
                    )

                    scope.content(tab).Content(
                        { contentColor },
                        Modifier
                            .clip(CircleShape)
                            .drawBehind {
                                drawRect(
                                    itemBackground,
                                    alpha = itemBackgroundAlpha
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    if (selectedTabState.value != tab) {
                                        selectedTabState.value = tab
                                        animationScope.launch {
                                            launch {
                                                offset.animateTo(
                                                    (tabs.indexOf(tab) * tabWidth).fastCoerceIn(0f, maxWidth),
                                                    spring(0.8f, 200f)
                                                )
                                            }
                                            launch {
                                                isDragging = true
                                                delay(200)
                                                isDragging = false
                                            }
                                        }
                                    }
                                }
                            }
                            .weight(1f)
                    )
                }
            }
        }

        val scaleXFraction by animateFloatAsState(
            if (!isDragging) 0f else 1f,
            spring(0.5f, 300f)
        )
        val scaleYFraction by animateFloatAsState(
            if (!isDragging) 0f else 1f,
            spring(0.5f, 600f)
        )

        Spacer(
            Modifier
                .layout { measurable, constraints ->
                    val width = tabWidth.fastRoundToInt()
                    val height = 56.dp.roundToPx()
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            (width * lerp(1f, 1.5f, scaleXFraction)).fastRoundToInt(),
                            (height * lerp(1f, 1.5f, scaleYFraction)).fastRoundToInt()
                        )
                    )

                    layout(width, height) {
                        placeable.place(
                            (width - placeable.width) / 2 + paddingPx,
                            (height - placeable.height) / 2 + paddingPx
                        )
                    }
                }
                .graphicsLayer {
                    translationX = offset.value
                    scaleX = lerp(1f, 0.9f, scaleXFraction)
                    scaleY = lerp(1f, 0.9f, scaleYFraction)
                }
                .background(background, CircleShape)
                .backdropShadow(CircleShape)
                .drawBackdrop(bottomTabsBackdrop) {
                    shape = CircleShape
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        refractionWithDispersion(
                            height = 12f.dp.toPx() * dragFraction,
                            amount = size.minDimension / 2f * dragFraction
                        )
                    }
                    onDrawBackdrop { drawBackdrop ->
                        scale(
                            lerp(1f, 0.8f, scaleXFraction),
                            lerp(1f, 0.8f, scaleYFraction)
                        ) {
                            val scaleX = lerp(1f, 0.9f, scaleXFraction)
                            val scaleY = lerp(1f, 0.9f, scaleYFraction)
                            scale(1f / scaleX, 1f / scaleY, Offset.Zero) {
                                drawBackdrop()
                            }
                        }
                    }
                    drawHighlight()
                }
                .draggable(
                    rememberDraggableState { delta ->
                        animationScope.launch {
                            offset.snapTo(
                                (offset.value + delta).fastCoerceIn(0f, maxWidth)
                            )
                        }
                    },
                    Orientation.Horizontal,
                    startDragImmediately = true,
                    onDragStarted = { isDragging = true },
                    onDragStopped = { velocity ->
                        isDragging = false
                        val currentIndex = offset.value / tabWidth
                        val targetIndex = when {
                            velocity > 0f -> ceil(currentIndex).toInt()
                            velocity < 0f -> floor(currentIndex).toInt()
                            else -> currentIndex.fastRoundToInt()
                        }.fastCoerceIn(0, tabs.lastIndex)
                        selectedTabState.value = tabs[targetIndex]
                        animationScope.launch {
                            offset.animateTo(
                                (targetIndex * tabWidth).fastCoerceIn(0f, maxWidth),
                                spring(0.8f, 380f)
                            )
                        }
                    }
                )
        )
    }
}
