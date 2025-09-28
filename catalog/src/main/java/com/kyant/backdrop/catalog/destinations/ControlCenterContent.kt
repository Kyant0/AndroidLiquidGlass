package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.utils.ProgressConverter
import com.kyant.backdrop.catalog.utils.rememberUISensor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

@Composable
fun ControlCenterContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor = Color.Black.copy(0.05f)
    val dimColor = Color.Black.copy(0.4f)

    val itemSpacing = 16f.dp
    val itemSize = 68f.dp
    val itemTwoSpanSize = itemSize * 2 + itemSpacing
    val itemShape = ContinuousRoundedRectangle(itemSize / 2f)

    val innerItemSize = 56f.dp
    val innerItemShape = ContinuousCapsule
    val innerItemIconScale = 0.8f

    val inactiveItemColor = Color.White.copy(0.2f)
    val activeItemColor = accentColor

    val airplaneModeIcon = painterResource(R.drawable.flight_40px)
    val iconColorFilter = ColorFilter.tint(Color.White)

    val animationScope = rememberCoroutineScope()
    val enterProgressAnimation = remember { Animatable(1f) }
    val safeEnterProgressAnimation = remember { Animatable(1f) }
    val progress by remember {
        derivedStateOf {
            val progress = enterProgressAnimation.value
            when {
                progress < 0f -> ProgressConverter.Default.convert(progress)
                progress <= 1f -> progress
                else -> 1f + ProgressConverter.Default.convert(progress - 1f)
            }
        }
    }
    val maxDragHeight = 1000f

    val uiSensor = rememberUISensor()
    val glassShape = { itemShape }
    val glassHighlight = {
        Highlight(
            style = HighlightStyle.Default(
                angle = uiSensor.gravityAngle,
                falloff = 2f
            )
        )
    }
    val glassLayer: GraphicsLayerScope.() -> Unit = {
        val progress = progress
        val safeProgress = safeEnterProgressAnimation.value
        translationY = -48f.dp.toPx() * (1f - progress)
        alpha = EaseIn.transform(safeProgress)
        scaleX /= 1f + 0.1f * (progress - 1f).fastCoerceAtLeast(0f)
        scaleY *= 1f + 0.1f * (progress - 1f).fastCoerceAtLeast(0f)
    }
    val glassSurface: DrawScope.() -> Unit = { drawRect(containerColor) }
    val glassEffects: BackdropEffectScope.() -> Unit = {
        val progress = safeEnterProgressAnimation.value
        vibrancy()
        refraction(
            24f.dp.toPx() * progress,
            48f.dp.toPx() * progress,
            true
        )
    }

    val spacerLayoutModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val progress = progress
        val height =
            itemSpacing.roundToPx() +
                    (32f.dp.toPx() * (progress - 1f).fastCoerceAtLeast(0f)).fastRoundToInt()
        layout(constraints.minWidth, height) {
            placeable.place(0, 0)
        }
    }
    val smallSpacerLayoutModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val progress = progress
        val height =
            itemSpacing.roundToPx() +
                    (16f.dp.toPx() * (progress - 1f).fastCoerceAtLeast(0f)).fastRoundToInt()
        layout(constraints.minWidth, height) {
            placeable.place(0, 0)
        }
    }

    val backdropModifier = Modifier
        .draggable(
            rememberDraggableState { delta ->
                val targetProgress = enterProgressAnimation.value + delta / maxDragHeight
                animationScope.launch {
                    launch {
                        enterProgressAnimation.snapTo(targetProgress)
                    }
                    launch {
                        safeEnterProgressAnimation.snapTo(targetProgress.fastCoerceIn(0f, 1f))
                    }
                }
            },
            Orientation.Vertical,
            onDragStopped = { velocity ->
                val targetProgress = when {
                    velocity < 0f -> 0f
                    velocity > 0f -> 1f
                    else -> if (enterProgressAnimation.value < 0.5f) 0f else 1f
                }
                animationScope.launch {
                    launch {
                        enterProgressAnimation.animateTo(
                            targetProgress,
                            if (targetProgress > 0.5f) {
                                spring(0.5f, 300f, 0.5f / maxDragHeight)
                            } else {
                                spring(1f, 300f, 0.01f)
                            },
                            velocity / maxDragHeight
                        )
                    }
                    launch {
                        safeEnterProgressAnimation.animateTo(
                            targetProgress,
                            spring(1f, 300f, 0.01f)
                        )
                    }
                }
            }
        )
        .drawWithContent {
            val progress = safeEnterProgressAnimation.value

            drawContent()
            drawRect(dimColor.copy(dimColor.alpha * progress))
        }
        .graphicsLayer {
            val progress = safeEnterProgressAnimation.value

            val blurRadius = 4f.dp.toPx() * progress
            if (blurRadius > 0f) {
                renderEffect = BlurEffect(blurRadius, blurRadius)
            }
        }

    BackdropDemoScaffold(
        backdropModifier,
        initialPainterResId = R.drawable.system_home_screen_light
    ) { backdrop ->
        Column(
            Modifier
                .padding(top = 80f.dp)
                .systemBarsPadding()
                .displayCutoutPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            highlight = glassHighlight,
                            shadow = null,
                            effects = glassEffects,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                        .padding(itemSpacing)
                ) {
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(inactiveItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.TopStart)
                    )
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(activeItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.TopEnd)
                    )
                    Box(
                        Modifier
                            .clip(innerItemShape)
                            .background(activeItemColor)
                            .scale(innerItemIconScale)
                            .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            .size(innerItemSize)
                            .align(Alignment.BottomStart)
                    )
                }
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            highlight = glassHighlight,
                            shadow = null,
                            effects = glassEffects,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                )
            }

            Spacer(spacerLayoutModifier)

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    effects = glassEffects,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    effects = glassEffects,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }

                    Spacer(smallSpacerLayoutModifier)

                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                highlight = glassHighlight,
                                shadow = null,
                                effects = glassEffects,
                                layerBlock = glassLayer
                            )
                            .size(itemTwoSpanSize, itemSize)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                highlight = glassHighlight,
                                shadow = null,
                                effects = glassEffects,
                                layerBlock = glassLayer,
                                onDrawSurface = glassSurface
                            )
                            .size(itemSize, itemTwoSpanSize)
                    )
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = glassShape,
                                highlight = glassHighlight,
                                shadow = null,
                                effects = glassEffects,
                                layerBlock = glassLayer,
                                onDrawSurface = glassSurface
                            )
                            .size(itemSize, itemTwoSpanSize)
                    )
                }
            }

            Spacer(spacerLayoutModifier)

            Row(
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = glassShape,
                            highlight = glassHighlight,
                            shadow = null,
                            effects = glassEffects,
                            layerBlock = glassLayer,
                            onDrawSurface = glassSurface
                        )
                        .size(itemTwoSpanSize)
                )

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    effects = glassEffects,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    effects = glassEffects,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }

                    Spacer(smallSpacerLayoutModifier)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    effects = glassEffects,
                                    layerBlock = glassLayer,
                                    onDrawSurface = glassSurface
                                )
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(itemSize)
                        )
                    }
                }
            }
        }
    }
}
