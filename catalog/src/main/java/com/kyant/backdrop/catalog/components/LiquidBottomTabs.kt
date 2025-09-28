package com.kyant.backdrop.catalog.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.utils.inspectDragGestures
import com.kyant.backdrop.catalog.utils.rememberMomentumAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val tabsBackdrop = rememberLayerBackdrop()
    val tabsLayer =
        rememberGraphicsLayer().apply {
            colorFilter = ColorFilter.tint(accentColor)
        }

    BoxWithConstraints(
        modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(maxHeight = 64f.dp.roundToPx()))
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val animationScope = rememberCoroutineScope()
        var pressStartPosition by remember { mutableStateOf(Offset.Zero) }
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
        val tabOffsetAnimation = remember(selectedTabIndex, tabWidth) {
            Animatable(
                Snapshot.withoutReadObservation {
                    selectedTabIndex() * tabWidth
                }
            )
        }
        var isDragging by remember { mutableStateOf(false) }

        val momentumAnimation = rememberMomentumAnimation(maxScale = 1.333f)

        val onDragStop = {
            animationScope.launch {
                val prevIndex = selectedTabIndex()
                val targetIndex = (tabOffsetAnimation.value / tabWidth).fastRoundToInt()
                if (prevIndex == targetIndex) {
                    val targetOffset = targetIndex * tabWidth
                    launch {
                        tabOffsetAnimation.animateTo(
                            targetOffset,
                            spring(1f, 800f, 0.5f)
                        )
                    }
                }
            }
        }

        val settle = {
            val targetTabIndex = (tabOffsetAnimation.value / tabWidth).fastRoundToInt()
            onTabSelected(targetTabIndex)
        }

        LaunchedEffect(Unit) {
            var job: Job? = null

            snapshotFlow { selectedTabIndex() }
                .drop(1)
                .collect { index ->
                    val targetOffset = index * tabWidth
                    launch {
                        tabOffsetAnimation.animateTo(
                            targetOffset,
                            spring(1f, 800f, 0.5f)
                        )
                    }

                    job?.cancel()
                    job = null
                    if (!isDragging) {
                        job = launch {
                            momentumAnimation.startPressingAnimation()
                            delay(200L)
                            momentumAnimation.endPressingAnimation()
                        }
                    }
                    isDragging = false
                }
        }

        val interactiveHighlightShader = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RuntimeShader(
                    """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 offset;

half4 main(float2 coord) {
    float2 center = offset;
    float dist = distance(coord, center);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
                )
            } else {
                Unit
            }
        }

        val drawInteractiveHighlightModifier = Modifier.drawWithContent {
            val progress = momentumAnimation.progress
            if (progress > 0f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && interactiveHighlightShader is RuntimeShader) {
                    drawRect(
                        Color.White.copy(0.08f * progress),
                        blendMode = BlendMode.Plus
                    )
                    interactiveHighlightShader.apply {
                        val offset = pressStartPosition.x + tabOffsetAnimation.value
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(0.15f * progress).toArgb())
                        setFloatUniform("radius", size.minDimension * 2f)
                        setFloatUniform(
                            "offset",
                            offset.fastCoerceIn(0f, size.width),
                            size.height / 2f
                        )
                    }
                    drawRect(
                        ShaderBrush(interactiveHighlightShader),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.25f * progress),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        refraction(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = momentumAnimation.progress
                        val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(drawInteractiveHighlightModifier)
                .drawWithContent {
                    drawContent()
                    tabsLayer.record { this@drawWithContent.drawContent() }
                }
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        Box(
            Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        highlight = {
                            val progress = momentumAnimation.progress
                            Highlight.Default.copy(alpha = progress)
                        },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(drawInteractiveHighlightModifier)
                    .height(56f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .graphicsLayer {
                        val progress = momentumAnimation.progress
                        val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                        scaleX = scale
                        scaleY = scale
                        translationX = panelOffset
                    }
                    .drawBehind {
                        drawLayer(tabsLayer)
                    }
                    .height(64f.dp)
                    .fillMaxWidth()
            )
        }

        Box(
            Modifier
                .layout { measurable, constraints ->
                    val progress = momentumAnimation.progress
                    val padding = (4f.dp.toPx() * (1f - progress)).fastRoundToInt()
                    val placeable = measurable.measure(constraints.offset(-padding * 2, 0))
                    layout(placeable.width + padding, placeable.height) {
                        placeable.place(padding, 0)
                    }
                }
                .graphicsLayer {
                    translationX =
                        tabOffsetAnimation.value.fastCoerceIn(0f, size.width * (tabsCount - 1)) +
                                panelOffset
                }
                .pointerInput(Unit) {
                    inspectDragGestures(
                        onDragStart = { down ->
                            isDragging = true
                            pressStartPosition = down.position
                        },
                        onDragEnd = {
                            settle()
                            onDragStop()
                            animationScope.launch {
                                offsetAnimation.animateTo(
                                    0f,
                                    spring(1f, 300f, 0.5f)
                                )
                            }
                        },
                        onDragCancel = {
                            settle()
                            onDragStop()
                        }
                    ) { _, dragAmount ->
                        val targetTabOffset =
                            (tabOffsetAnimation.value + dragAmount.x)
                                .fastCoerceIn(0f, size.width.toFloat() * (tabsCount - 1))
                        animationScope.launch { tabOffsetAnimation.snapTo(targetTabOffset) }

                        val targetOffset = offsetAnimation.value + dragAmount.x
                        animationScope.launch { offsetAnimation.snapTo(targetOffset) }
                    }
                }
                .then(momentumAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val progress = momentumAnimation.progress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = momentumAnimation.progress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = momentumAnimation.progress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    effects = {
                        val progress = momentumAnimation.progress
                        refractionWithDispersion(
                            10f.dp.toPx() * progress,
                            12f.dp.toPx() * progress
                        )
                    },
                    layerBlock = {
                        scaleX = momentumAnimation.scaleX
                        scaleY = momentumAnimation.scaleY
                        val velocity = momentumAnimation.velocity / 5000f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.15f, 0.15f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.15f, 0.15f)
                    },
                    onDrawSurface = {
                        val progress = momentumAnimation.progress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}
