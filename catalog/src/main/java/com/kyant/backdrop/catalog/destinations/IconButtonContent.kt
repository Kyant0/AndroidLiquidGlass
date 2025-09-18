package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.inspectDragGestures
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.rememberBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun IconButtonContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val backdrop = rememberBackdrop()

    val animationScope = rememberCoroutineScope()
    val pressAnimation = remember { Animatable(0f) }
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scaleAnimation = remember { Animatable(0f) }

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
                .drawBackdrop(
                    backdrop,
                    { CircleShape },
                    layerBlock = {
                        val offset = offsetAnimation.value
                        val scale = 1f + 12f.dp.toPx() / size.minDimension * scaleAnimation.value
                        val offsetAngle = atan2(offset.y, offset.x)
                        val deltaScale = 4f.dp.toPx() / size.minDimension
                        translationX = 12f.dp.toPx() * tanh(0.5f * offset.x / size.width)
                        translationY = 12f.dp.toPx() * tanh(0.5f * offset.y / size.height)
                        scaleX = scale + deltaScale * abs(cos(offsetAngle) * offset.x / size.width)
                        scaleY = scale + deltaScale * abs(sin(offsetAngle) * offset.y / size.height)
                    },
                    onDrawSurface = { drawRect(containerColor) }
                ) {
                    saturation()
                    blur(8f.dp.toPx())
                    refraction(12f.dp.toPx(), size.minDimension / 2f)
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Color.White.copy(0.3f * pressAnimation.value),
                        blendMode = BlendMode.Plus
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .pointerInput(animationScope) {
                    val onDragStop: () -> Unit = {
                        animationScope.launch {
                            launch {
                                pressAnimation.animateTo(
                                    0f,
                                    spring(1f, 300f)
                                )
                            }
                            launch {
                                offsetAnimation.animateTo(
                                    Offset.Zero,
                                    spring(1f, 300f)
                                )
                            }
                            launch {
                                scaleAnimation.animateTo(
                                    0f,
                                    spring(0.5f, 300f)
                                )
                            }
                        }
                    }

                    inspectDragGestures(
                        onDragStart = { down ->
                            animationScope.launch {
                                launch {
                                    pressAnimation.animateTo(
                                        1f,
                                        spring(1f, 300f)
                                    )
                                }
                                launch {
                                    offsetAnimation.snapTo(Offset.Zero)
                                }
                                launch {
                                    scaleAnimation.animateTo(
                                        1f,
                                        spring(0.5f, 300f)
                                    )
                                }
                            }
                        },
                        onDragEnd = { onDragStop() },
                        onDragCancel = onDragStop
                    ) { _, dragAmount ->
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount)
                        }
                    }
                }
                .paint(
                    painterResource(R.drawable.flight_40px),
                    colorFilter = ColorFilter.tint(accentColor)
                )
                .size(64f.dp)
                .align(Alignment.Center)
        )
    }
}
