package com.kyant.backdrop.catalog.destinations

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.utils.inspectDragGestures
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch

@Composable
fun BottomTabsContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val airplaneModeIcon = painterResource(R.drawable.flight_40px)
    val iconColorFilter = ColorFilter.tint(contentColor)

    val tabsBackdrop = rememberLayerBackdrop()
    val tabsLayer =
        rememberGraphicsLayer().apply {
            colorFilter = ColorFilter.tint(accentColor)
        }
    val innerShadowLayer =
        rememberGraphicsLayer().apply {
            compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
        }

    val animationScope = rememberCoroutineScope()
    var pressStartPosition by remember { mutableStateOf(Offset.Zero) }
    val progressAnimation = remember { Animatable(0f) }
    val scaleXAnimation = remember { Animatable(0f) }
    val scaleYAnimation = remember { Animatable(0f) }
    val velocityAnimation = remember { Animatable(0f) }
    val tabOffsetAnimation = remember { Animatable(0f) }

    val tabLayerBlock: GraphicsLayerScope.() -> Unit = {
        scaleX = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleXAnimation.value)
        scaleY = lerp(1f, 1f + 20f.dp.toPx() / size.height, scaleYAnimation.value)
        val velocity = velocityAnimation.value / size.width
        scaleX /= 1f + (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
        scaleY *= 1f + (velocity * 0.25f).fastCoerceIn(-0.15f, 0.15f)
    }

    val interactiveHighlightShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 offset;

half4 main(float2 coord) {
    float2 center = offset;
    float dist = distance(coord, center);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            Unit
        }
    }

    val drawInteractiveHighlightModifier = Modifier.drawWithContent {
        val progress = progressAnimation.value.fastCoerceIn(0f, 1f)
        if (progress > 0f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && interactiveHighlightShader is RuntimeShader) {
                drawRect(
                    Color.White.copy(0.08f * progress),
                    blendMode = BlendMode.Plus
                )
                interactiveHighlightShader.apply {
                    val offset = pressStartPosition.x + tabOffsetAnimation.value
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", Color.White.copy(0.15f * progress).toArgb())
                    setFloatUniform("radius", size.minDimension * 2f)
                    setFloatUniform(
                        "offset",
                        offset.fastCoerceIn(0f, size.width),
                        size.height / 2f
                    )
                }
                drawRect(
                    ShaderBrush(interactiveHighlightShader),
                    blendMode = BlendMode.Plus
                )
            } else {
                drawRect(
                    Color.White.copy(0.25f * progress),
                    blendMode = BlendMode.Plus
                )
            }
        }

        drawContent()
    }

    BackdropDemoScaffold(
        initialPainterResId = R.drawable.system_home_screen_light
    ) { backdrop ->
        Box(
            Modifier.padding(40f.dp),
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
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                refraction(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            layerBlock = {
                                val progress = progressAnimation.value
                                val scale = lerp(1f, 1f + 2f.dp.toPx() / size.height, progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(drawInteractiveHighlightModifier)
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
                            BasicText(
                                "Tab ${index + 1}",
                                style = TextStyle(contentColor, 12f.sp)
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .layout { measurable, constraints ->
                            val padding = (4f.dp.toPx() * (1f - progressAnimation.value)).fastRoundToInt()
                            val placeable = measurable.measure(constraints.offset(-padding * 2, 0))
                            layout(placeable.width + padding, placeable.height) {
                                placeable.place(padding, 4f.dp.roundToPx())
                            }
                        }
                        .graphicsLayer {
                            translationX = tabOffsetAnimation.value.fastCoerceIn(0f, size.width * 3)
                            tabLayerBlock()
                            shape = ContinuousCapsule
                            clip = true
                        }
                        .drawBehind {
                            if (progressAnimation.value != 0f) {
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
                    .layerBackdrop(tabsBackdrop),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            highlight = {
                                val progress = progressAnimation.value
                                Highlight.Default.copy(alpha = progress)
                            },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(drawInteractiveHighlightModifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                )

                Box(
                    Modifier
                        .graphicsLayer {
                            val progress = progressAnimation.value
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
                        val padding = (4f.dp.toPx() * (1f - progressAnimation.value)).fastRoundToInt()
                        val placeable = measurable.measure(constraints.offset(-padding * 2, 0))
                        layout(placeable.width + padding, placeable.height) {
                            placeable.place(padding, 0)
                        }
                    }
                    .graphicsLayer {
                        translationX = tabOffsetAnimation.value.fastCoerceIn(0f, size.width * 3)
                    }
                    .pointerInput(Unit) {
                        val onDragStart = {
                            animationScope.launch {
                                launch {
                                    progressAnimation.animateTo(
                                        1f,
                                        spring(1f, 800f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleXAnimation.animateTo(
                                        1f,
                                        spring(0.3f, 300f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleYAnimation.animateTo(
                                        1f,
                                        spring(0.5f, 300f, 0.001f)
                                    )
                                }
                            }
                        }

                        val onDragStop = {
                            animationScope.launch {
                                launch {
                                    progressAnimation.animateTo(
                                        0f,
                                        spring(1f, 800f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleXAnimation.animateTo(
                                        0f,
                                        spring(0.3f, 300f, 0.001f)
                                    )
                                }
                                launch {
                                    scaleYAnimation.animateTo(
                                        0f,
                                        spring(0.5f, 300f, 0.001f)
                                    )
                                }
                            }
                        }

                        val velocityTracker = VelocityTracker()

                        val updateVelocity: (Float) -> Unit = { v ->
                            val alpha = 0.05f
                            val targetVelocity = velocityAnimation.velocity * (1f - alpha) + v * alpha
                            animationScope.launch {
                                velocityAnimation.snapTo(targetVelocity)
                            }
                        }
                        val settleVelocity: () -> Unit = {
                            animationScope.launch {
                                velocityAnimation.animateTo(
                                    0f,
                                    spring(0.5f, 500f, 1f)
                                )
                            }
                        }

                        inspectDragGestures(
                            onDragStart = { down ->
                                velocityTracker.addPointerInputChange(down)

                                pressStartPosition = down.position
                                onDragStart()
                            },
                            onDragEnd = { change ->
                                velocityTracker.addPointerInputChange(change)
                                settleVelocity()
                                velocityTracker.resetTracking()

                                onDragStop()
                            },
                            onDragCancel = {
                                settleVelocity()
                                velocityTracker.resetTracking()

                                onDragStop()
                            }
                        ) { change, dragAmount ->
                            velocityTracker.addPointerInputChange(change)
                            updateVelocity(velocityTracker.calculateVelocity().x)

                            val targetOffset =
                                (tabOffsetAnimation.value + dragAmount.x)
                                    .fastCoerceIn(0f, size.width.toFloat() * 3)
                            animationScope.launch {
                                tabOffsetAnimation.snapTo(targetOffset)
                            }
                        }
                    }
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { ContinuousCapsule },
                        highlight = {
                            val progress = progressAnimation.value
                            Highlight.Default.copy(alpha = progress)
                        },
                        shadow = {
                            val progress = progressAnimation.value
                            Shadow(
                                radius = 24f.dp * progress,
                                offset = DpOffset(0f.dp, 4f.dp * progress)
                            )
                        },
                        effects = {
                            val progress = progressAnimation.value
                            refractionWithDispersion(
                                10f.dp.toPx() * progress,
                                10f.dp.toPx() * progress
                            )
                        },
                        layerBlock = tabLayerBlock,
                        onDrawSurface = {
                            val progress = progressAnimation.value

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

                            drawRect(
                                if (isLightTheme) Color.Black.copy(0.1f)
                                else Color.White.copy(0.1f),
                                alpha = 1f - progress
                            )
                        }
                    )
                    .height(56f.dp)
                    .fillMaxWidth(0.25f)
            )
        }
    }
}
