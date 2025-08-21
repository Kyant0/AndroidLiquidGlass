@file:OptIn(ExperimentalLuminanceSamplerApi::class)

package com.kyant.liquidglass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.liquidglass.highlight.GlassHighlightElement
import com.kyant.liquidglass.material.GlassBrushElement
import com.kyant.liquidglass.sampler.ExperimentalLuminanceSamplerApi
import com.kyant.liquidglass.sampler.LuminanceSampler
import com.kyant.liquidglass.shadow.GlassShadowElement
import com.kyant.liquidglass.utils.GlassShaders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null
): Modifier =
    this.liquidGlass(
        state = state,
        luminanceSampler = null,
        compositingStrategy = compositingStrategy,
        transformBlock = transformBlock,
        style = { style }
    )

fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this.liquidGlass(
        state = state,
        luminanceSampler = null,
        compositingStrategy = compositingStrategy,
        transformBlock = transformBlock,
        style = style
    )

@ExperimentalLuminanceSamplerApi
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    luminanceSampler: LuminanceSampler? = null
): Modifier =
    this.liquidGlass(
        state = state,
        luminanceSampler = luminanceSampler,
        compositingStrategy = compositingStrategy,
        transformBlock = transformBlock,
        style = { style }
    )

@ExperimentalLuminanceSamplerApi
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    luminanceSampler: LuminanceSampler? = null,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this
        .then(
            GlassShadowElement(
                style = style
            )
        )
        .then(
            GlassShapeElement(
                style = style,
                compositingStrategy = compositingStrategy
            )
        )
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LiquidGlassElement(
                    state = state,
                    style = style,
                    luminanceSampler = luminanceSampler,
                    transformBlock = transformBlock
                ) then GlassBrushElement(
                    style = style
                ) then GlassHighlightElement(
                    style = style
                )
            } else {
                Modifier
            }
        )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassElement(
    val state: LiquidGlassProviderState,
    val style: () -> GlassStyle,
    val luminanceSampler: LuminanceSampler?,
    val transformBlock: (DrawTransform.() -> Unit)?
) : ModifierNodeElement<LiquidGlassNode>() {

    override fun create(): LiquidGlassNode {
        return LiquidGlassNode(
            state = state,
            style = style,
            luminanceSampler = luminanceSampler,
            transformBlock = transformBlock
        )
    }

    override fun update(node: LiquidGlassNode) {
        node.update(
            state = state,
            style = style,
            luminanceSampler = luminanceSampler,
            transformBlock = transformBlock
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
        properties["luminanceSampler"] = luminanceSampler
        properties["transformBlock"] = transformBlock
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassElement) return false

        if (state != other.state) return false
        if (style != other.style) return false
        if (luminanceSampler != other.luminanceSampler) return false
        if (transformBlock != other.transformBlock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (luminanceSampler?.hashCode() ?: 0)
        result = 31 * result + (transformBlock?.hashCode() ?: 0)
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassNode(
    var state: LiquidGlassProviderState,
    var style: () -> GlassStyle,
    var luminanceSampler: LuminanceSampler?,
    var transformBlock: (DrawTransform.() -> Unit)?
) : GlobalPositionAwareModifierNode, ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var position: Offset by mutableStateOf(Offset.Zero)
    private var graphicsLayer: GraphicsLayer? = null

    private var samplerJob: Job? = null
    private var samplerLayer: GraphicsLayer? = null
    private var samplerChanged = true

    private var currentStyle = style()

    private var _colorFilter: ColorFilter? = null
    private var _colorFilterEffect: RenderEffect? = null

    private var _blurRadiusPx: Float = Float.NaN
    private var _blurRenderEffect: RenderEffect? = null

    private var _innerRefractionWithDepthEffect = false
    private var _innerRefractionShader: RuntimeShader? = null

    private var _size: Size = Size.Unspecified
    private var _cornerRadiusPx: Float = Float.NaN
    private var _innerRefractionHeight: Float = Float.NaN
    private var _innerRefractionAmount: Float = Float.NaN
    private var _depthEffect: Float = Float.NaN
    private var _innerRefractionRenderEffect: RenderEffect? = null

    private var _renderEffect: androidx.compose.ui.graphics.RenderEffect? = null
    private var transformBlockChanged = true
    private var _position: Offset? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val style = currentStyle

        val colorFilter = style.material.colorFilter
        val colorFilterChanged = _colorFilter != colorFilter
        if (colorFilterChanged) {
            _colorFilter = colorFilter
            _colorFilterEffect =
                if (colorFilter != null) {
                    RenderEffect.createColorFilterEffect(colorFilter.asAndroidColorFilter())
                } else {
                    null
                }
        }
        val colorFilterEffect = _colorFilterEffect

        val blurRadiusPx = style.material.blurRadius.toPx()
        val blurRadiusChanged = colorFilterChanged || _blurRadiusPx != blurRadiusPx
        if (blurRadiusChanged) {
            _blurRadiusPx = blurRadiusPx
            _blurRenderEffect =
                if (blurRadiusPx > 0f) {
                    if (colorFilterEffect != null) {
                        RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            colorFilterEffect,
                            Shader.TileMode.CLAMP
                        )
                    } else {
                        RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.CLAMP
                        )
                    }
                } else {
                    colorFilterEffect
                }
        }
        val blurRenderEffect = _blurRenderEffect

        val innerRefractionWithDepthEffect = style.innerRefraction.depthEffect > 0f
        val innerRefractionWithDepthEffectChanged =
            _innerRefractionWithDepthEffect != innerRefractionWithDepthEffect
        if (innerRefractionWithDepthEffectChanged || _innerRefractionShader == null) {
            _innerRefractionWithDepthEffect = innerRefractionWithDepthEffect
            _innerRefractionShader =
                if (innerRefractionWithDepthEffect) {
                    RuntimeShader(GlassShaders.refractionShaderWidthDepthEffectString)
                } else {
                    RuntimeShader(GlassShaders.refractionShaderString)
                }
        }
        val innerRefractionShader = _innerRefractionShader!!

        val size = size
        val sizeChanged = _size != size
        _size = size

        val cornerRadiusPx = style.shape.topStart.toPx(size, this)
        val cornerRadiusChanged = _cornerRadiusPx != cornerRadiusPx
        _cornerRadiusPx = cornerRadiusPx

        val innerRefractionHeight = style.innerRefraction.height.toPx(size, this)
        val innerRefractionAmount = style.innerRefraction.amount.toPx(size, this)
        val depthEffect = style.innerRefraction.depthEffect
        val innerRefractionChanged =
            sizeChanged || cornerRadiusChanged ||
                    _innerRefractionHeight != innerRefractionHeight ||
                    _innerRefractionAmount != innerRefractionAmount ||
                    _depthEffect != depthEffect
        if (innerRefractionChanged) {
            _innerRefractionHeight = innerRefractionHeight
            _innerRefractionAmount = innerRefractionAmount
            _depthEffect = depthEffect
            _innerRefractionRenderEffect =
                RenderEffect.createRuntimeShaderEffect(
                    innerRefractionShader.apply {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("cornerRadius", cornerRadiusPx)

                        setFloatUniform("refractionHeight", innerRefractionHeight)
                        setFloatUniform("refractionAmount", innerRefractionAmount)
                        if (innerRefractionWithDepthEffect) {
                            setFloatUniform("depthEffect", depthEffect)
                        }
                    },
                    "image"
                )
        }
        val innerRefractionRenderEffect = _innerRefractionRenderEffect!!

        val renderEffectChanged = blurRadiusChanged || innerRefractionChanged
        if (renderEffectChanged) {
            _renderEffect =
                if (blurRenderEffect != null) {
                    RenderEffect.createChainEffect(
                        innerRefractionRenderEffect,
                        blurRenderEffect
                    )
                } else {
                    innerRefractionRenderEffect
                }.asComposeRenderEffect()

            graphicsLayer?.renderEffect = _renderEffect
        }

        val position = position
        val positionChanged = _position != position
        _position = position
        if (renderEffectChanged || transformBlockChanged || positionChanged) {
            transformBlockChanged = false
            graphicsLayer?.record {
                val transformBlock = transformBlock
                if (transformBlock != null) {
                    withTransform(transformBlock) {
                        translate(-position.x, -position.y) {
                            drawLayer(state.graphicsLayer)
                        }
                    }
                } else {
                    translate(-position.x, -position.y) {
                        drawLayer(state.graphicsLayer)
                    }
                }
            }
        }

        if (samplerChanged) {
            if (luminanceSampler != null) {
                samplerLayer =
                    requireGraphicsContext().createGraphicsLayer().apply {
                        compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                    }
            }
        }
        if (samplerChanged || positionChanged) {
            samplerLayer?.record {
                translate(-position.x, -position.y) {
                    drawLayer(state.graphicsLayer)
                }
            }
        }
        samplerChanged = false

        onDrawBehind {
            graphicsLayer?.let { layer ->
                drawLayer(layer)
            }
        }
    })

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val providerPosition = state.position
            position = coordinates.positionOnScreen() - providerPosition
        }
    }

    override fun onObservedReadsChanged() {
        updateStyle()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            }

        updateStyle()
        updateSampler()
    }

    override fun onDetach() {
        samplerJob?.cancel()
        samplerJob = null

        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
        samplerLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            samplerLayer = null
            samplerChanged = true
        }
    }

    fun update(
        state: LiquidGlassProviderState,
        style: () -> GlassStyle,
        luminanceSampler: LuminanceSampler?,
        transformBlock: (DrawTransform.() -> Unit)?
    ) {
        if (this.state != state ||
            this.style != style
        ) {
            this.state = state
            this.style = style
            updateStyle()
        }
        if (this.luminanceSampler != luminanceSampler) {
            this.luminanceSampler = luminanceSampler
            updateSampler()
        }
        if (this.transformBlock != transformBlock) {
            this.transformBlock = transformBlock
            transformBlockChanged = true
            drawNode.invalidateDrawCache()
        }
    }

    private fun updateStyle() {
        observeReads { currentStyle = style() }
        drawNode.invalidateDrawCache()
    }

    private fun updateSampler() {
        samplerJob?.cancel()
        samplerJob = null
        samplerLayer?.let { layer ->
            requireGraphicsContext().releaseGraphicsLayer(layer)
            samplerLayer = null
        }
        this.luminanceSampler?.let { sampler ->
            samplerJob =
                coroutineScope.launch {
                    while (isActive) {
                        samplerLayer?.let { layer ->
                            sampler.sample(layer)
                        }
                        delay(sampler.sampleIntervalMillis)
                    }
                }
        }
        samplerChanged = true
        drawNode.invalidateDrawCache()
    }
}
