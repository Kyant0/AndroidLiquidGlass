package com.kyant.backdrop.catalog.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.kyant.backdrop.catalog.utils.rememberMomentumAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule

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

    val trackBackdrop = rememberLayerBackdrop()

    val momentumAnimation = rememberMomentumAnimation(maxScale = 1.5f)

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
                    Orientation.Horizontal
                )
                .then(momentumAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                    shape = { ContinuousCapsule },
                    highlight = {
                        val progress = momentumAnimation.progress
                        Highlight.Ambient.copy(alpha = progress)
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = momentumAnimation.progress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    effects = {
                        val progress = momentumAnimation.progress
                        blur(8f.dp.toPx() * (1f - progress))
                        refractionWithDispersion(
                            height = 6f.dp.toPx() * progress,
                            amount = size.height / 2f * progress
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
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
