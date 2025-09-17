package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.ProgressConverter
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.rememberUISensor
import com.kyant.backdrop.catalog.theme.LocalContentColor
import com.kyant.backdrop.contentBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.rememberBackdrop
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

    val backdrop = rememberBackdrop()
    val uiSensor = rememberUISensor()
    val glassShape = { itemShape }
    val glassHighlight = {
        Highlight(color = Color.White.copy(0.5f)) {
            HighlightStyle.Dynamic(angle = uiSensor.gravityAngle)
        }
    }
    val glassSurface: DrawScope.() -> Unit = { drawRect(containerColor) }
    val glassEffects: BackdropEffectScope.() -> Unit = {
        saturation()
        blur(8f.dp.toPx())
        refraction(24f.dp.toPx(), 48f.dp.toPx(), true)
    }

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

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            Modifier
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
                .fillMaxSize()
        ) {
            Image(
                painterResource(R.drawable.system_home_screen_light),
                null,
                Modifier
                    .backdrop(backdrop)
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
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                Modifier
                    .graphicsLayer {
                        val progress = progress

                        translationY = lerp(-56f.dp.toPx(), 0f, progress)
                        alpha = EaseIn.transform(progress)
                        scaleX = 1f - 0.1f / size.height * size.width * (progress - 1f).fastCoerceAtLeast(0f)
                        scaleY = 1f + 0.1f * (progress - 1f).fastCoerceAtLeast(0f)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .padding(top = 80f.dp)
                    .systemBarsPadding()
                    .displayCutoutPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .contentBackdrop(
                                { ContinuousRoundedRectangle(40f.dp) },
                                highlight = glassHighlight,
                                shadow = null,
                                onDrawSurface = glassSurface
                            ) {
                                refraction(12f.dp.toPx(), size.minDimension / 4f, true)
                            }
                            .drawBackdrop(
                                backdrop,
                                { ContinuousRoundedRectangle(40f.dp) },
                                highlight = null,
                                shadow = null,
                                onDrawSurface = glassSurface,
                                effects = glassEffects
                            )
                            .size(itemTwoSpanSize)
                            .padding(itemSpacing)
                    ) {
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(inactiveItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.TopStart)
                        )
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(activeItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.TopEnd)
                        )
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(activeItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.BottomStart)
                        )
                    }
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop,
                                glassShape,
                                highlight = glassHighlight,
                                shadow = null,
                                onDrawSurface = glassSurface,
                                effects = glassEffects
                            )
                            .size(itemTwoSpanSize)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .drawBackdrop(
                                        backdrop,
                                        glassShape,
                                        highlight = glassHighlight,
                                        shadow = null,
                                        onDrawSurface = glassSurface,
                                        effects = glassEffects
                                    )
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                            Box(
                                Modifier
                                    .drawBackdrop(
                                        backdrop,
                                        glassShape,
                                        highlight = glassHighlight,
                                        shadow = null,
                                        onDrawSurface = glassSurface,
                                        effects = glassEffects
                                    )
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop,
                                    glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    onDrawSurface = glassSurface,
                                    effects = glassEffects
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
                                    backdrop,
                                    glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    onDrawSurface = glassSurface,
                                    effects = glassEffects
                                )
                                .size(itemSize, itemTwoSpanSize)
                        )
                        Box(
                            Modifier
                                .drawBackdrop(
                                    backdrop,
                                    glassShape,
                                    highlight = glassHighlight,
                                    shadow = null,
                                    onDrawSurface = glassSurface,
                                    effects = glassEffects
                                )
                                .size(itemSize, itemTwoSpanSize)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .drawBackdrop(
                                backdrop,
                                glassShape,
                                highlight = glassHighlight,
                                shadow = null,
                                onDrawSurface = glassSurface,
                                effects = glassEffects
                            )
                            .size(itemTwoSpanSize)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .drawBackdrop(
                                        backdrop,
                                        glassShape,
                                        highlight = glassHighlight,
                                        shadow = null,
                                        onDrawSurface = glassSurface,
                                        effects = glassEffects
                                    )
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                            Box(
                                Modifier
                                    .drawBackdrop(
                                        backdrop,
                                        glassShape,
                                        highlight = glassHighlight,
                                        shadow = null,
                                        onDrawSurface = glassSurface,
                                        effects = glassEffects
                                    )
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .drawBackdrop(
                                        backdrop,
                                        glassShape,
                                        highlight = glassHighlight,
                                        shadow = null,
                                        onDrawSurface = glassSurface,
                                        effects = glassEffects
                                    )
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                    }
                }
            }
        }
    }
}
