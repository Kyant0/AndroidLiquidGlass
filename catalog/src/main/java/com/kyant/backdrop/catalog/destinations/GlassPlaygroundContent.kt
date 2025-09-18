package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.rememberBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassPlaygroundContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)

    val backdrop = rememberBackdrop()

    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.wallpaper_light),
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
                    { ContinuousRoundedRectangle(48f.dp) },
                    layerBlock = {
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = zoom
                        scaleY = zoom
                        rotationZ = rotation
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                ) {
                    saturation()
                    refraction(24f.dp.toPx(), size.minDimension / 2f)
                }
                .pointerInput(animationScope) {
                    fun Offset.rotateBy(angle: Float): Offset {
                        val angleInRadians = angle * (PI / 180)
                        val cos = cos(angleInRadians)
                        val sin = sin(angleInRadians)
                        return Offset((x * cos - y * sin).toFloat(), (x * sin + y * cos).toFloat())
                    }

                    detectTransformGestures { centroid, pan, gestureZoom, gestureRotate ->
                        val offset = offsetAnimation.value
                        val zoom = zoomAnimation.value
                        val rotation = rotationAnimation.value

                        val targetZoom = zoom * gestureZoom
                        val targetRotation = rotation + gestureRotate
                        val targetOffset = offset + pan.rotateBy(targetRotation) * targetZoom

                        animationScope.launch {
                            offsetAnimation.snapTo(targetOffset)
                            zoomAnimation.snapTo(targetZoom)
                            rotationAnimation.snapTo(targetRotation)
                        }
                    }
                }
                .paint(
                    painterResource(R.drawable.flight_40px),
                    colorFilter = ColorFilter.tint(accentColor)
                )
                .size(256f.dp)
                .align(Alignment.Center)
        )
    }
}
