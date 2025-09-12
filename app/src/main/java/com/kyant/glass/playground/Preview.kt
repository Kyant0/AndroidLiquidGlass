package com.kyant.glass.playground

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.expressa.prelude.*
import com.kyant.expressa.shape.RoundedRectangle
import com.kyant.glass.R
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import org.intellij.lang.annotations.Language

@Composable
fun Preview() {
    val state = remember { PreviewState() }

    val graphicsLayer = rememberGraphicsLayer()
    val backgroundColor = surfaceContainerLowest
    val containerSize = LocalWindowInfo.current.containerSize
    var rect by remember { mutableStateOf(Rect.Zero) }
    val painter =
        state.imageBitmap?.let { BitmapPainter(it) }
            ?: painterResource(R.drawable.homescreen)

    val providerState = rememberLiquidGlassProviderState(backgroundColor)

    Box(Modifier.fillMaxSize()) {
        // background
        Image(
            painter, null,
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    graphicsLayer.record {
                        drawRect(backgroundColor)
                        this@drawWithContent.drawContent()
                    }
                }
                .liquidGlassProvider(providerState),
            contentScale = ContentScale.Crop
        )

        @Language("AGSL")
        val colorShaderUtils = """
    const half3 rgbToY = half3(0.2126, 0.7152, 0.0722);
    
    float luma(half4 color) {
        return dot(toLinearSrgb(color.rgb), rgbToY);
    }
        """.trimIndent()

        @Language("AGSL")
        val refractionShaderUtils = """
    float circleMap(float x) {
        return 1.0 - sqrt(1.0 - x * x);
    }
    
    float sdRectangle(float2 coord, float2 halfSize) {
        float2 d = abs(coord) - halfSize;
        float outside = length(max(d, 0.0));
        float inside = min(max(d.x, d.y), 0.0);
        return outside + inside;
    }
    
    float sdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        return sdRectangle(coord, innerHalfSize) - cornerRadius;
    }
    
    float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        float2 cornerCoord = abs(coord) - innerHalfSize;
        
        if (cornerCoord.x >= 0.0 && cornerCoord.y >= 0.0) {
            return sign(coord) * normalize(cornerCoord);
        } else {
            return sign(coord) * ((-cornerCoord.x < -cornerCoord.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
        }
    }
    
    half4 refractionColor(float2 coord, float2 size, float cornerRadius, float eccentricFactor, float height, float amount) {
        float2 halfSize = size * 0.5;
        float2 centeredCoord = coord - halfSize;
        float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
        sd = min(sd, 0.0);
        
        if (sd <= 0.0 && -sd <= height) {
            float maxGradRadius = max(min(halfSize.x, halfSize.y), cornerRadius);
            float gradRadius = min(cornerRadius * 1.5, maxGradRadius);
            float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
            
            float refractedDistance = circleMap(1.0 - -sd / height) * amount;
            float2 refractedDirection = normalize(normal + eccentricFactor * normalize(centeredCoord));
            float2 refractedCoord = coord + refractedDistance * refractedDirection;
            if (refractedCoord.x < 0.0 || refractedCoord.x >= size.x ||
                refractedCoord.y < 0.0 || refractedCoord.y >= size.y) {
                return half4(0.0, 0.0, 0.0, 1.0);
            }
            
            return image.eval(refractedCoord);
        } else {
            return image.eval(coord);
        }
    }
        """.trimIndent()

        // glass
        Box(
            Modifier
                .graphicsLayer { // transform and clip
                    translationX = state.offset.x
                    translationY = state.offset.y
                    clip = true
                    shape = RoundedRectangle(state.cornerRadius.value)
                }
                .graphicsLayer { // dispersion effect
                    if (!state.isDispersionEnabled) return@graphicsLayer
                    renderEffect = RenderEffect.createRuntimeShaderEffect(
                        RuntimeShader(
                            """
    uniform shader image;
    
    uniform float2 size;
    uniform float cornerRadius;
    
    uniform float dispersionHeight;
    uniform float dispersionAmount;
    
    float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    float circleMap(float x) {
        return 1.0 - sqrt(1.0 - x * x);
    }
    
    float2 normalToTangent(float2 normal) {
        return float2(normal.y, -normal.x);
    }
    
    float sdRectangle(float2 coord, float2 halfSize) {
        float2 d = abs(coord) - halfSize;
        float outside = length(max(d, 0.0));
        float inside = min(max(d.x, d.y), 0.0);
        return outside + inside;
    }
    
    float sdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        return sdRectangle(coord, innerHalfSize) - cornerRadius;
    }
    
    float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        float2 cornerCoord = abs(coord) - innerHalfSize;
        
        if (cornerCoord.x >= 0.0 && cornerCoord.y >= 0.0) {
            return sign(coord) * normalize(cornerCoord);
        } else {
            return sign(coord) * ((-cornerCoord.x < -cornerCoord.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
        }
    }
    
    half4 main(float2 coord) {
        float2 halfSize = size * 0.5;
        float2 centeredCoord = coord - halfSize;
        float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
        
        if (sd < 0.0 && -sd < dispersionHeight) {
            float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
            float2 tangent = normalToTangent(normal);
            
            float dispersionDistance = circleMap(1.0 - -sd / dispersionHeight) * dispersionAmount;
            if (dispersionDistance < 2.0) {
                half4 color = image.eval(coord);
                return color;
            }
            
            half4 dispersedColor = half4(0.0);
            half4 weight = half4(0.0);
            float maxI = min(dispersionDistance, 20.0);
            for (float i = 0.0; i < 20.0; i++) {
                float t = i / maxI;
                if (t > 1.0) break;
                half4 color = image.eval(coord + tangent * float2(t - 0.5) * dispersionDistance);
                half rMask = step(0.5, t);
                half gMask = step(0.25, t) * step(t, 0.75);
                half bMask = step(t, 0.5);
                half4 mask = half4(rMask, gMask, bMask, 0.0);
                dispersedColor += color * mask;
                weight += mask;
            }
            dispersedColor /= weight;
            dispersedColor.a = image.eval(coord).a;
            return dispersedColor;
        } else {
            half4 color = image.eval(coord);
            return color;
        }
    }"""
                        ).apply {
                            val cornerRadius =
                                state.cornerRadius.value.toPx()
                                    .fastCoerceAtMost(size.minDimension / 2f)

                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("cornerRadius", cornerRadius)

                            setFloatUniform("dispersionHeight", state.refractionHeight.value.toPx())
                            setFloatUniform("dispersionAmount", -state.refractionAmount.value.toPx())
                        },
                        "image"
                    ).asComposeRenderEffect()
                }
                .graphicsLayer { // refraction & bleed
                    val refractionRenderEffect = RenderEffect.createRuntimeShaderEffect(
                        RuntimeShader(
                            """
    uniform shader image;
    
    uniform float2 size;
    uniform float cornerRadius;
    
    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float eccentricFactor;
    
    uniform float bleedOpacity;
    
    $colorShaderUtils
    $refractionShaderUtils
    
    half4 main(float2 coord) {
        half4 color = refractionColor(coord, size, cornerRadius, eccentricFactor, refractionHeight, refractionAmount);
        if (bleedOpacity <= 0.0) {
            return color;
        } else {
            float luma = luma(color);
            color *= 1.0 - bleedOpacity * luma;
            return color;
        }
    }"""
                        ).apply {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("cornerRadius", state.cornerRadius.value.toPx())

                            setFloatUniform("refractionHeight", state.refractionHeight.value.toPx())
                            setFloatUniform("refractionAmount", state.refractionAmount.value.toPx())
                            setFloatUniform("eccentricFactor", state.eccentricFactor.value)

                            setFloatUniform("bleedOpacity", state.bleedOpacity.value)
                        },
                        "image"
                    )

                    val bleedRenderEffect = RenderEffect.createRuntimeShaderEffect(
                        RuntimeShader(
                            """
    uniform shader image;
    
    uniform float2 size;
    uniform float cornerRadius;
    
    uniform float eccentricFactor;
    uniform float bleedAmount;
    
    $colorShaderUtils
    $refractionShaderUtils
    
    half4 main(float2 coord) {
        half4 color = refractionColor(coord, size, cornerRadius, eccentricFactor, cornerRadius * 3.5, bleedAmount);
        float luma = luma(color);
        color.rgb = mix(color.rgb, half3(1.0), 0.5 * circleMap(1.0 - luma));
        return color;
    }"""
                        ).apply {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("cornerRadius", state.cornerRadius.value.toPx())

                            setFloatUniform("eccentricFactor", state.eccentricFactor.value)
                            setFloatUniform("bleedAmount", state.bleedAmount.value.toPx())
                        },
                        "image"
                    )

                    renderEffect =
                        if (state.bleedOpacity.value > 0f) {
                            val bleedBlurRadiusPx = state.bleedBlurRadius.value.toPx()
                            val blurredBleedRenderEffect =
                                if (bleedBlurRadiusPx > 0f) {
                                    RenderEffect.createChainEffect(
                                        bleedRenderEffect,
                                        RenderEffect.createBlurEffect(
                                            bleedBlurRadiusPx,
                                            bleedBlurRadiusPx,
                                            Shader.TileMode.CLAMP
                                        )
                                    )
                                } else {
                                    bleedRenderEffect
                                }

                            RenderEffect.createBlendModeEffect(
                                blurredBleedRenderEffect,
                                refractionRenderEffect,
                                android.graphics.BlendMode.SRC_OVER
                            ).asComposeRenderEffect()
                        } else {
                            refractionRenderEffect.asComposeRenderEffect()
                        }
                }
                .graphicsLayer { // blur
                    val blurRadiusPx = state.blurRadius.value.toPx()
                    if (blurRadiusPx > 0f) {
                        renderEffect =
                            RenderEffect.createBlurEffect(
                                blurRadiusPx,
                                blurRadiusPx,
                                Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                    }
                }
                .graphicsLayer { // white point & chroma boost
                    val contrast = state.contrast.value
                    val whitePoint = state.whitePoint.value
                    val chromaMultiplier = state.chromaMultiplier.value

                    if (contrast != 0f || whitePoint != 0f || chromaMultiplier != 1f) {
                        renderEffect = RenderEffect.createRuntimeShaderEffect(
                            RuntimeShader(
                                """
    uniform shader image;
    
    uniform float contrast;
    uniform float whitePoint;
    uniform float chromaMultiplier;
    
    $colorShaderUtils
    
    half4 saturateColor(half4 color, float amount) {
        half3 linearSrgb = toLinearSrgb(color.rgb);
        float y = dot(linearSrgb, rgbToY);
        half3 gray = half3(y);
        half3 adjustedLinearSrgb = mix(gray, linearSrgb, amount);
        half3 adjustedSrgb = fromLinearSrgb(adjustedLinearSrgb);
        return half4(adjustedSrgb, color.a);
    }
    
    half4 main(float2 coord) {
        half4 color = image.eval(coord);
        
        color = saturateColor(color, chromaMultiplier);
        
        float3 target = (whitePoint > 0.0) ? float3(1.0) : float3(0.0);
        color.rgb = mix(color.rgb, target, abs(whitePoint));
        
        color.rgb = (color.rgb - 0.5) * (1.0 + contrast) + 0.5;
        
        return color;
    }"""
                            ).apply {
                                setFloatUniform("contrast", contrast)
                                setFloatUniform("whitePoint", whitePoint)
                                setFloatUniform("chromaMultiplier", chromaMultiplier)
                            },
                            "image"
                        ).asComposeRenderEffect()
                    }
                }
                .drawBehind {
                    clipRect(0f, 0f, rect.width, rect.height) {
                        translate(-rect.left, -rect.top) {
                            drawLayer(graphicsLayer)
                        }
                    }
                }
                .onGloballyPositioned { layoutCoordinates ->
                    rect = layoutCoordinates.boundsInParent()
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        state.offset += pan
                        state.size = DpSize(
                            (state.size.width * zoom).coerceIn(
                                state.minSize.width,
                                containerSize.width.toDp()
                            ),
                            (state.size.height * zoom).coerceIn(
                                state.minSize.height,
                                containerSize.height.toDp()
                            )
                        )
                    }
                }
                .layout { measurable, constraints ->
                    val size = state.size.toSize().roundToIntSize()
                    val width = size.width.fastCoerceAtMost(constraints.maxWidth)
                    val height = size.height.fastCoerceAtMost(constraints.maxHeight)
                    val placeable = measurable.measure(Constraints.fixed(width, height))

                    layout(width, height) {
                        placeable.place(0, 0)
                    }
                }
                .align(Alignment.Center)
        )

        if (state.displayControls) {
            PreviewControls(
                state,
                providerState,
                rect = { rect }
            )
        }

        AnimatedContent(
            state.isInConfigurationMode,
            Modifier
                .padding(16.dp)
                .safeDrawingPadding()
                .align(Alignment.BottomCenter)
        ) { isInConfigurationMode ->
            if (!isInConfigurationMode) {
                PreviewToolbar(state, providerState)
            } else {
                ConfigurationBottomSheet(state, providerState)
            }
        }
    }
}
