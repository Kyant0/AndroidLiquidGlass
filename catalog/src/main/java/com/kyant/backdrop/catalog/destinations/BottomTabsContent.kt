package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.Text
import com.kyant.backdrop.catalog.theme.LocalContentColor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.rememberBackdrop
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch

@Composable
fun BottomTabsContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val airplaneModeIcon = painterResource(R.drawable.flight_40px)
    val iconColorFilter = ColorFilter.tint(LocalContentColor.current)

    val backdrop = rememberBackdrop()
    val tabsBackdrop = rememberBackdrop()
    val tabsLayer =
        rememberGraphicsLayer().apply {
            colorFilter = ColorFilter.tint(accentColor)
        }

    val animationScope = rememberCoroutineScope()
    val pressAnimation = remember { Animatable(0f) }
    val scaleXAnimation = remember { Animatable(0f) }
    val scaleYAnimation = remember { Animatable(0f) }
    val tabOffsetAnimation = remember { Animatable(0f) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.system_home_screen_light),
            null,
            Modifier
                .backdrop(backdrop)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .padding(40f.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width =
                            placeable.width +
                                    (4f.dp.toPx() / placeable.height * placeable.width).fastRoundToInt() +
                                    48f.dp.roundToPx()
                        val height =
                            placeable.height +
                                    4f.dp.roundToPx() +
                                    48f.dp.roundToPx()
                        layout(width, height) {
                            placeable.place(
                                (width - placeable.width) / 2,
                                (height - placeable.height) / 2
                            )
                        }
                    }
            ) {
                Row(
                    Modifier
                        .graphicsLayer {
                            val progress = pressAnimation.value
                            val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                        .drawBackdrop(
                            backdrop,
                            { ContinuousCapsule },
                            onDrawBackdrop = { drawBackdrop ->
                                val progress = pressAnimation.value
                                val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                                scale(1f / scale, 1f / scale, Offset.Zero) {
                                    drawBackdrop()
                                }
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        ) {
                            saturation()
                            blur(8f.dp.toPx())
                            refraction(24f.dp.toPx(), 24f.dp.toPx())
                        }
                        .drawWithContent {
                            drawContent()
                            tabsLayer.record { this@drawWithContent.drawContent() }
                        }
                        .height(64f.dp)
                        .fillMaxWidth()
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        Column(
                            Modifier
                                .clip(ContinuousCapsule)
                                .clickable {}
                                .fillMaxHeight()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(28f.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                            Text(
                                "Tab ${index + 1}",
                                TextStyle(fontSize = 12f.sp)
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .layout { measurable, constraints ->
                            val padding = (4f.dp.toPx() * (1f - pressAnimation.value)).fastRoundToInt()
                            val placeable = measurable.measure(constraints.offset(-padding, 0))
                            layout(placeable.width + padding, placeable.height) {
                                placeable.place(padding, 4f.dp.roundToPx())
                            }
                        }
                        .graphicsLayer {
                            translationX = tabOffsetAnimation.value.fastCoerceIn(0f, size.width * 3)
                            scaleX = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleXAnimation.value)
                            scaleY = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleYAnimation.value)
                            shape = ContinuousCapsule
                            clip = true
                        }
                        .drawBehind {
                            if (pressAnimation.value != 0f) {
                                drawRect(Color.Black, blendMode = BlendMode.DstOut)
                            }
                        }
                        .height(56f.dp)
                        .fillMaxWidth(0.25f)
                )
            }

            Box(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .backdrop(tabsBackdrop),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop,
                            { ContinuousCapsule },
                            highlight = null,
                            shadow = null,
                            onDrawSurface = { drawRect(containerColor) }
                        ) {
                            saturation()
                            blur(8f.dp.toPx())
                        }
                        .height(56f.dp)
                        .fillMaxWidth()
                )

                Box(
                    Modifier
                        .graphicsLayer {
                            val progress = pressAnimation.value
                            val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                            scaleX = scale
                            scaleY = scale
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
                        val padding = (4f.dp.toPx() * (1f - pressAnimation.value)).fastRoundToInt()
                        val placeable = measurable.measure(constraints.offset(-padding, 0))
                        layout(placeable.width + padding, placeable.height) {
                            placeable.place(padding, 0)
                        }
                    }
                    .graphicsLayer {
                        translationX = tabOffsetAnimation.value.fastCoerceIn(0f, size.width * 3)
                        scaleX = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleXAnimation.value)
                        scaleY = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleYAnimation.value)
                    }
                    .draggable(
                        rememberDraggableState { delta ->
                            val scaleX = lerp(1f, 1.5f, scaleXAnimation.value)
                            val targetProgress =
                                (tabOffsetAnimation.value + delta * scaleX).fastCoerceAtLeast(0f)
                            animationScope.launch { tabOffsetAnimation.snapTo(targetProgress) }
                        },
                        Orientation.Horizontal,
                        startDragImmediately = true,
                        onDragStarted = {
                            animationScope.launch {
                                launch {
                                    pressAnimation.animateTo(
                                        1f,
                                        spring(1f, 800f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleXAnimation.animateTo(
                                        1f,
                                        spring(0.3f, 200f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleYAnimation.animateTo(
                                        1f,
                                        spring(0.5f, 200f, 0.001f)
                                    )
                                }
                            }
                        },
                        onDragStopped = { velocity ->
                            animationScope.launch {
                                launch {
                                    pressAnimation.animateTo(
                                        0f,
                                        spring(1f, 800f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleXAnimation.animateTo(
                                        0f,
                                        spring(0.3f, 200f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleYAnimation.animateTo(
                                        0f,
                                        spring(0.5f, 200f, 0.001f)
                                    )
                                }
                            }
                        }
                    )
                    .drawBackdrop(
                        tabsBackdrop,
                        { ContinuousCapsule },
                        highlight = {
                            val progress = pressAnimation.value
                            Highlight(color = Color.White.copy(0.38f * progress))
                        },
                        shadow = {
                            val progress = pressAnimation.value
                            Shadow(
                                elevation = 24f.dp * progress,
                                offset = DpOffset(0f.dp, 4f.dp * progress)
                            )
                        },
                        onDrawBackdrop = { drawBackdrop ->
                            val scaleX = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleXAnimation.value)
                            val scaleY = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleYAnimation.value)
                            scale(1f / scaleX, 1f / scaleY, Offset.Zero) {
                                drawBackdrop()
                            }
                        },
                        onDrawSurface = {
                            val progress = pressAnimation.value
                            drawRect(
                                if (isLightTheme) Color.Black.copy(0.1f)
                                else Color.White.copy(0.1f),
                                alpha = 1f - progress
                            )
                        }
                    ) {
                        val progress = pressAnimation.value
                        refractionWithDispersion(
                            12f.dp.toPx() * progress,
                            12f.dp.toPx() * progress
                        )
                    }
                    .height(56f.dp)
                    .fillMaxWidth(0.25f)
            )
        }
    }
}
