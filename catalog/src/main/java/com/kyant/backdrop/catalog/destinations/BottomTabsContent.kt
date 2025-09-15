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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
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
import com.kyant.backdrop.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
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
    val activeTabContainerColor =
        if (isLightTheme) Color(0xFF787880).copy(0.16f)
        else Color(0xFF787880).copy(0.32f)
    val activeIconColorFilter = ColorFilter.tint(accentColor)
    val iconColorFilter = ColorFilter.tint(LocalContentColor.current)

    val backdrop = rememberLayerBackdrop(null)

    val tabsBackdrop = rememberLayerBackdrop(null)

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
            Row(
                Modifier
                    .drawBackdrop(
                        backdrop,
                        { ContinuousRoundedRectangle(48f.dp) },
                        highlight = {
                            Highlight(
                                style = { com.kyant.backdrop.highlight.HighlightStyle.Soft }
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    ) {
                        saturation()
                        blur(if (isLightTheme) 8f.dp.toPx() else 8f.dp.toPx())
                        refraction(24f.dp.toPx(), 24f.dp.toPx())
                    }
                    .height(72f.dp)
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

            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .backdrop(tabsBackdrop)
                    .drawBackdrop(
                        backdrop,
                        { ContinuousRoundedRectangle(48f.dp) },
                        highlight = {
                            Highlight(
                                style = { com.kyant.backdrop.highlight.HighlightStyle.Soft }
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    ) {
                        saturation()
                        blur(if (isLightTheme) 8f.dp.toPx() else 8f.dp.toPx())
                        refraction(24f.dp.toPx(), 24f.dp.toPx())
                    }
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor))
                    .height(60f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Column(
                        Modifier
                            .clip(ContinuousCapsule)
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

            val scaleXAnimation = remember { Animatable(1f) }
            val scaleYAnimation = remember { Animatable(1f) }

            val animationScope = rememberCoroutineScope()
            val tabOffsetAnimation = remember { Animatable(0f) }

            Box(
                Modifier
                    .padding(4f.dp)
                    .graphicsLayer {
                        translationX = tabOffsetAnimation.value.fastCoerceIn(0f, size.width * 3)
                        scaleX = scaleYAnimation.value
                        scaleY = scaleYAnimation.value
                    }
                    .draggable(
                        rememberDraggableState { delta ->
                            val targetProgress = tabOffsetAnimation.value + delta * scaleXAnimation.value
                            animationScope.launch { tabOffsetAnimation.snapTo(targetProgress) }
                        },
                        Orientation.Horizontal,
                        startDragImmediately = true,
                        onDragStarted = {
                            animationScope.launch {
                                launch {
                                    scaleXAnimation.animateTo(
                                        1.33f,
                                        spring(0.3f, 200f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleYAnimation.animateTo(
                                        1.33f,
                                        spring(0.5f, 200f, 0.001f)
                                    )
                                }
                            }
                        },
                        onDragStopped = { velocity ->
                            animationScope.launch {
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
                        }
                    )
                    .drawBackdrop(
                        tabsBackdrop,
                        { ContinuousCapsule },
                        onDrawBackdrop = { drawBackdrop ->
                            scale(
                                1f / scaleYAnimation.value,
                                1f / scaleYAnimation.value,
                                Offset.Zero
                            ) {
                                drawBackdrop()
                            }
                        },
                        onDrawSurface = {
                            drawRect(
                                activeTabContainerColor,
                                alpha = 0f
                            )
                        }
                    ) {
                        saturation()
                        refractionWithDispersion(16f.dp.toPx(), 16f.dp.toPx())
                    }
                    .height(64f.dp)
                    .fillMaxWidth(0.25f)
            )
        }
    }
}