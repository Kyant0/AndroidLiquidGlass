package com.kyant.backdrop.catalog.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val fraction by remember {
        derivedStateOf {
            ((value() - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                .fastCoerceIn(0f, 1f)
        }
    }

    val animationScope = rememberCoroutineScope()
    val progressAnimationSpec = spring(0.5f, 300f, 0.001f)
    val progressAnimation = remember { Animatable(0f) }

    val trackBackdrop = rememberLayerBackdrop()
    val innerShadowLayer =
        rememberGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val trackWidth = constraints.maxWidth

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(trackColor)
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(ContinuousCapsule)
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val fraction = fraction
                        val width = (fraction * constraints.maxWidth).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = fraction
                    translationX =
                        (-size.width / 2f + fraction * trackWidth)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f)
                }
                .draggable(
                    rememberDraggableState { delta ->
                        val trackWidth = trackWidth - with(density) { 40f.dp.toPx() }
                        val targetFraction = fraction + delta / trackWidth
                        val targetValue = lerp(valueRange.start, valueRange.endInclusive, targetFraction)
                        onValueChange(targetValue.fastCoerceIn(valueRange.start, valueRange.endInclusive))
                    },
                    Orientation.Horizontal,
                    startDragImmediately = true,
                    onDragStarted = {
                        animationScope.launch {
                            progressAnimation.animateTo(1f, progressAnimationSpec)
                        }
                    },
                    onDragStopped = {
                        animationScope.launch {
                            progressAnimation.animateTo(0f, progressAnimationSpec)
                        }
                    }
                )
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                    shape = { ContinuousCapsule },
                    effects = {
                        refractionWithDispersion(6f.dp.toPx(), size.height / 2f)
                    },
                    highlight = {
                        val progress = progressAnimation.value
                        Highlight.AmbientDefault.copy(alpha = progress)
                    },
                    shadow = {
                        Shadow(
                            elevation = 4f.dp,
                            color = Color.Black.copy(0.05f)
                        )
                    },
                    layer = {
                        val progress = progressAnimation.value
                        val scale = lerp(1f, 1.5f, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        val progress = progressAnimation.value.fastCoerceIn(0f, 1f)

                        val shape = ContinuousCapsule
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val innerShadowOffset = 4f.dp.toPx() * progress
                        val innerShadowBlurRadius = 2f.dp.toPx() * progress

                        innerShadowLayer.alpha = progress
                        if (innerShadowBlurRadius > 0f) {
                            innerShadowLayer.renderEffect =
                                BlurEffect(
                                    innerShadowBlurRadius,
                                    innerShadowBlurRadius,
                                    TileMode.Decal
                                )
                        }
                        innerShadowLayer.record {
                            drawOutline(outline, Color.Black.copy(0.1f))
                            translate(0f, innerShadowOffset) {
                                drawOutline(outline, Color.Transparent, blendMode = BlendMode.Clear)
                            }
                        }
                        drawLayer(innerShadowLayer)

                        drawRect(Color.White.copy(1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
