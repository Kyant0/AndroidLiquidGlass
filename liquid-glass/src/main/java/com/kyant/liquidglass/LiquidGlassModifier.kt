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
import com.kyant.liquidglass.dispersion.Dispersion
import com.kyant.liquidglass.highlight.GlassHighlightElement
import com.kyant.liquidglass.highlight.SimpleGlassHighlightElement
import com.kyant.liquidglass.material.GlassBrushElement
import com.kyant.liquidglass.material.SimpleGlassBrushElement
import com.kyant.liquidglass.shadow.GlassShadowElement
import com.kyant.liquidglass.shadow.SimpleGlassShadowElement
import com.kyant.liquidglass.utils.GlassShaders

fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null
): Modifier =
    this
        .then(SimpleGlassShadowElement(style))
        .then(SimpleGlassShapeElement(style, compositingStrategy))
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SimpleLiquidGlassElement(
                    state = state,
                    style = style,
                    transformBlock = transformBlock
                )
            } else {
                Modifier
            }
        )
        .then(SimpleGlassBrushElement(style))
        .then(SimpleGlassHighlightElement(style))

fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this
        .then(GlassShadowElement(style))
        .then(GlassShapeElement(style, compositingStrategy))
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LiquidGlassElement(
                    state = state,
                    style = style,
                    transformBlock = transformBlock
                )
            } else {
                Modifier
            }
        )
        .then(GlassBrushElement(style))
        .then(GlassHighlightElement(style))

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SimpleLiquidGlassElement(
    val state: LiquidGlassProviderState,
    val style: GlassStyle,
    val transformBlock: (DrawTransform.() -> Unit)?
) : ModifierNodeElement<SimpleLiquidGlassNode>() {

    override fun create(): SimpleLiquidGlassNode {
        return SimpleLiquidGlassNode(
            state = state,
            style = style,
            transformBlock = transformBlock
        )
    }

    override fun update(node: SimpleLiquidGlassNode) {
        node.update(
            state = state,
            style = style,
            transformBlock = transformBlock
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
        properties["transformBlock"] = transformBlock
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassElement) return false

        if (state != other.state) return false
        if (style != other.style) return false
        if (transformBlock != other.transformBlock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (transformBlock?.hashCode() ?: 0)
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassElement(
    val state: LiquidGlassProviderState,
    val style: () -> GlassStyle,
    val transformBlock: (DrawTransform.() -> Unit)?
) : ModifierNodeElement<LiquidGlassNode>() {

    override fun create(): LiquidGlassNode {
        return LiquidGlassNode(
            state = state,
            style = style,
            transformBlock = transformBlock
        )
    }

    override fun update(node: LiquidGlassNode) {
        node.update(
            state = state,
            style = style,
            transformBlock = transformBlock
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
        properties["transformBlock"] = transformBlock
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassElement) return false

        if (state != other.state) return false
        if (style != other.style) return false
        if (transformBlock != other.transformBlock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (transformBlock?.hashCode() ?: 0)
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SimpleLiquidGlassNode(
    var state: LiquidGlassProviderState,
    var style: GlassStyle,
    var transformBlock: (DrawTransform.() -> Unit)?
) : GlobalPositionAwareModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var position: Offset by mutableStateOf(Offset.Zero)
    private var graphicsLayer: GraphicsLayer? = null

    private val innerRefractionShader = RuntimeShader(GlassShaders.refractionShaderString)
    private var dispersionShader: RuntimeShader? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val colorFilter = style.material.colorFilter
        val colorFilterEffect =
            if (colorFilter != null) {
                RenderEffect.createColorFilterEffect(colorFilter.asAndroidColorFilter())
            } else {
                null
            }

        val blurRadiusPx = style.material.blurRadius.toPx()
        val blurRenderEffect =
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

        val cornerRadiusPx = style.shape.topStart.toPx(size, this)

        val innerRefractionHeight = style.innerRefraction.height.toPx(size, this)
        val innerRefractionAmount = style.innerRefraction.amount.toPx(size, this)
        val depthEffect = style.innerRefraction.depthEffect
        val innerRefractionRenderEffect =
            RenderEffect.createRuntimeShaderEffect(
                innerRefractionShader.apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadius", cornerRadiusPx)

                    setFloatUniform("refractionHeight", innerRefractionHeight)
                    setFloatUniform("refractionAmount", innerRefractionAmount)
                    setFloatUniform("depthEffect", depthEffect)
                },
                "image"
            )

        val dispersion = style.dispersion
        val dispersionHeight: Float
        val dispersionAmount: Float
        when (dispersion) {
            Dispersion.None -> {
                dispersionHeight = 0f
                dispersionAmount = 0f
            }

            Dispersion.Automatic -> {
                dispersionHeight = innerRefractionHeight
                dispersionAmount = -innerRefractionAmount
            }

            is Dispersion.Fractional -> {
                dispersionHeight = innerRefractionHeight * dispersion.fraction
                dispersionAmount = -innerRefractionAmount * dispersion.fraction
            }
        }
        val dispersionEffect =
            if (dispersionHeight == 0f || dispersionAmount == 0f) {
                null
            } else {
                val shader =
                    dispersionShader
                        ?: RuntimeShader(GlassShaders.dispersionShaderString).also { dispersionShader = it }
                RenderEffect.createRuntimeShaderEffect(
                    shader.apply {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("cornerRadius", cornerRadiusPx)

                        setFloatUniform("dispersionHeight", dispersionHeight)
                        setFloatUniform("dispersionAmount", dispersionAmount)
                    },
                    "image"
                )
            }

        val refractionWithDispersionRenderEffect =
            if (dispersionEffect != null) {
                RenderEffect.createChainEffect(
                    dispersionEffect,
                    innerRefractionRenderEffect
                )
            } else {
                innerRefractionRenderEffect
            }

        val renderEffect =
            if (blurRenderEffect != null) {
                RenderEffect.createChainEffect(
                    refractionWithDispersionRenderEffect,
                    blurRenderEffect
                )
            } else {
                refractionWithDispersionRenderEffect
            }.asComposeRenderEffect()

        graphicsLayer?.renderEffect = renderEffect
        graphicsLayer?.record {
            val transformBlock = transformBlock
            val position = position
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

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
    }

    fun update(
        state: LiquidGlassProviderState,
        style: GlassStyle,
        transformBlock: (DrawTransform.() -> Unit)?
    ) {
        if (this.state != state ||
            this.style != style ||
            this.transformBlock != transformBlock
        ) {
            this.state = state
            this.style = style
            this.transformBlock = transformBlock
            drawNode.invalidateDrawCache()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassNode(
    var state: LiquidGlassProviderState,
    var style: () -> GlassStyle,
    var transformBlock: (DrawTransform.() -> Unit)?
) : GlobalPositionAwareModifierNode, ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var position: Offset by mutableStateOf(Offset.Zero)
    private var graphicsLayer: GraphicsLayer? = null

    private var currentStyle = style()

    private val innerRefractionShader = RuntimeShader(GlassShaders.refractionShaderString)

    private val drawNode = delegate(CacheDrawModifierNode {
        val style = currentStyle

        val colorFilter = style.material.colorFilter
        val colorFilterEffect =
            if (colorFilter != null) {
                RenderEffect.createColorFilterEffect(colorFilter.asAndroidColorFilter())
            } else {
                null
            }

        val blurRadiusPx = style.material.blurRadius.toPx()
        val blurRenderEffect =
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

        val cornerRadiusPx = style.shape.topStart.toPx(size, this)

        val innerRefractionHeight = style.innerRefraction.height.toPx(size, this)
        val innerRefractionAmount = style.innerRefraction.amount.toPx(size, this)
        val depthEffect = style.innerRefraction.depthEffect
        val innerRefractionRenderEffect =
            RenderEffect.createRuntimeShaderEffect(
                innerRefractionShader.apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadius", cornerRadiusPx)

                    setFloatUniform("refractionHeight", innerRefractionHeight)
                    setFloatUniform("refractionAmount", innerRefractionAmount)
                    setFloatUniform("depthEffect", depthEffect)
                },
                "image"
            )

        val renderEffect =
            if (blurRenderEffect != null) {
                RenderEffect.createChainEffect(
                    innerRefractionRenderEffect,
                    blurRenderEffect
                )
            } else {
                innerRefractionRenderEffect
            }.asComposeRenderEffect()

        graphicsLayer?.renderEffect = renderEffect
        graphicsLayer?.record {
            val transformBlock = transformBlock
            val position = position
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
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
    }

    fun update(
        state: LiquidGlassProviderState,
        style: () -> GlassStyle,
        transformBlock: (DrawTransform.() -> Unit)?
    ) {
        if (this.state != state ||
            this.style != style
        ) {
            this.state = state
            this.style = style
            updateStyle()
        }
        if (this.transformBlock != transformBlock) {
            this.transformBlock = transformBlock
            drawNode.invalidateDrawCache()
        }
    }

    private fun updateStyle() {
        observeReads { currentStyle = style() }
        drawNode.invalidateDrawCache()
    }
}
