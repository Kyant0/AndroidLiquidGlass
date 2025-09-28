package com.kyant.backdrop.catalog.destinations

import android.graphics.Bitmap
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.scale
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.Block
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

@Composable
fun AdaptiveLuminanceGlassContent() {
    val isLightTheme = !isSystemInDarkTheme()

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(if (isLightTheme) 1f else 0f) }
    val contentColorAnimation = remember {
        Animatable(if (isLightTheme) Color.Black else Color.White)
    }
    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            withContext(Dispatchers.IO) {
                val imageBitmap = layer.toImageBitmap()
                val thumbnail =
                    imageBitmap.asAndroidBitmap()
                        .scale(5, 5, false)
                        .copy(Bitmap.Config.ARGB_8888, false)
                buffer.rewind()
                thumbnail.copyPixelsToBuffer(buffer)
            }
            val averageLuminance =
                (0 until 25).sumOf { index ->
                    val color = buffer.get(index)
                    val r = (color shr 16 and 0xFF) / 255f
                    val g = (color shr 8 and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                } / 25
            launch {
                contentColorAnimation.animateTo(
                    if (averageLuminance > 0.5f) Color.Black else Color.White,
                    tween(1000)
                )
            }
            luminanceAnimation.animateTo(
                averageLuminance.toFloat(),
                tween(1000)
            )
        }
    }

    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    BackdropDemoScaffold { backdrop ->
        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24f.dp) },
                    highlight = { Highlight.Plain },
                    effects = {
                        val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
                        colorControls(
                            brightness =
                                if (l > 0f) lerp(0.1f, 0.5f, l)
                                else lerp(0.1f, -0.2f, -l),
                            contrast =
                                if (l > 0f) lerp(1f, 0f, l)
                                else 1f,
                            saturation = 1.5f
                        )
                        blur(
                            if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                            else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                        )
                        refraction(24f.dp.toPx(), size.minDimension / 2f, true)
                    },
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
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        layer.record { drawBackdrop() }
                    }
                )
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
                .size(160f.dp),
            contentAlignment = Alignment.Center
        ) {
            Block {
                BasicText(
                    "luminance:\n${"%.2f".format(luminanceAnimation.value)}",
                    style = TextStyle(Color.Unspecified, 16f.sp, textAlign = TextAlign.Center),
                    color = { contentColorAnimation.value }
                )
            }
        }
    }
}
