package com.kyant.backdrop.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.Block
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassPlaygroundContent() {
    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnimation = remember { Animatable(1f) }
    val rotationAnimation = remember { Animatable(0f) }

    var isSheetExpanded by remember { mutableStateOf(true) }

    var cornerRadiusFrac by remember { mutableFloatStateOf(0.5f) }
    var blurRadiusDp by remember { mutableFloatStateOf(0f) }
    var refractionHeightFrac by remember { mutableFloatStateOf(0.1f) }
    var refractionAmountFrac by remember { mutableFloatStateOf(0.2f) }
    var dispersionIntensity by remember { mutableFloatStateOf(0f) }

    BackdropDemoScaffold { backdrop ->
        Box(
            Modifier
                .padding(top = 48f.dp)
                .statusBarsPadding()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(256f.dp / 2f * cornerRadiusFrac) },
                    highlight = { Highlight.Plain },
                    effects = {
                        val minDimension = size.minDimension
                        vibrancy()
                        blur(blurRadiusDp.dp.toPx())
                        refractionWithDispersion(
                            height = refractionHeightFrac * minDimension * 0.5f,
                            amount = refractionAmountFrac * minDimension,
                            hasDepthEffect = true,
                            dispersionIntensity = dispersionIntensity
                        )
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
                .size(256f.dp)
                .align(Alignment.TopCenter)
        )

        Block {
            if (isSheetExpanded) {
                val sheetBackdrop = rememberLayerBackdrop()
                Column(
                    Modifier
                        .padding(16f.dp)
                        .padding(bottom = 72f.dp)
                        .navigationBarsPadding()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(32f.dp) },
                            highlight = { Highlight.Plain },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                refraction(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                        )
                        .padding(24f.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16f.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Corner radius")
                        LiquidSlider(
                            value = { cornerRadiusFrac },
                            onValueChange = { cornerRadiusFrac = it },
                            valueRange = 0f..1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Blur radius")
                        LiquidSlider(
                            value = { blurRadiusDp },
                            onValueChange = { blurRadiusDp = it },
                            valueRange = 0f..32f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Refraction height")
                        LiquidSlider(
                            value = { refractionHeightFrac },
                            onValueChange = { refractionHeightFrac = it },
                            valueRange = 0f..1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Refraction amount")
                        LiquidSlider(
                            value = { refractionAmountFrac },
                            onValueChange = { refractionAmountFrac = it },
                            valueRange = 0f..1f,
                            backdrop = sheetBackdrop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Dispersion intensity")
                        LiquidSlider(
                            value = { dispersionIntensity },
                            onValueChange = { dispersionIntensity = it },
                            valueRange = 0f..1f,
                            backdrop = sheetBackdrop
                        )
                    }
                }
            }
        }

        Block {
            LiquidButton(
                { isSheetExpanded = !isSheetExpanded },
                backdrop,
                Modifier
                    .padding(20f.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomStart),
                tint = Color(0xFFFF8D28)
            ) {
                BasicText(
                    if (isSheetExpanded) "🔽" else "🔼",
                    style = TextStyle(Color.White, 15f.sp)
                )
            }
        }
    }
}
