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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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

private val DefaultOnDrawBackdrop: BackdropDrawScope.() -> Unit = { drawBackdrop() }

fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: BackdropDrawScope.() -> Unit = DefaultOnDrawBackdrop
): Modifier =
    this
        .then(SimpleGlassShadowElement(style))
        .then(SimpleGlassShapeElement(style, compositingStrategy))
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SimpleLiquidGlassElement(
                    state = state,
                    style = style,
                    onDrawBackdrop = onDrawBackdrop
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
    onDrawBackdrop: BackdropDrawScope.() -> Unit = DefaultOnDrawBackdrop,
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
                    onDrawBackdrop = onDrawBackdrop
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
    val onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ModifierNodeElement<SimpleLiquidGlassNode>() {

    override fun create(): SimpleLiquidGlassNode {
        return SimpleLiquidGlassNode(
            state = state,
            style = style,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun update(node: SimpleLiquidGlassNode) {
        node.update(
            state = state,
            style = style,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
        properties["onDrawBackdrop"] = onDrawBackdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassElement) return false

        if (state != other.state) return false
        if (style != other.style) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (onDrawBackdrop?.hashCode() ?: 0)
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassElement(
    val state: LiquidGlassProviderState,
    val style: () -> GlassStyle,
    val onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ModifierNodeElement<LiquidGlassNode>() {

    override fun create(): LiquidGlassNode {
        return LiquidGlassNode(
            state = state,
            style = style,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun update(node: LiquidGlassNode) {
        node.update(
            state = state,
            style = style,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["state"] = state
        properties["style"] = style
        properties["onDrawBackdrop"] = onDrawBackdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassElement) return false

        if (state != other.state) return false
        if (style != other.style) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (onDrawBackdrop?.hashCode() ?: 0)
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SimpleLiquidGlassNode(
    var state: LiquidGlassProviderState,
    var style: GlassStyle,
    var onDrawBackdrop: BackdropDrawScope.() -> Unit
) : GlobalPositionAwareModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var position: Offset by mutableStateOf(Offset.Zero)
    private var graphicsLayer: GraphicsLayer? = null

    private val innerRefractionShader = RuntimeShader(GlassShaders.refractionShaderString)
    private var dispersionShader: RuntimeShader? = null

    private val drawBackdropBlock: DrawScope.() -> Unit = {
        val position = position
        translate(-position.x, -position.y) {
            drawLayer(state.graphicsLayer)
        }
    }

    private val drawNode = delegate(CacheDrawModifierNode {
        val style = style

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

        val graphicsLayer = graphicsLayer
        graphicsLayer?.renderEffect = renderEffect

        onDrawWithContent {
            if (graphicsLayer != null) {
                graphicsLayer.record {
                    val backdropDrawScope = BackdropDrawScopeImpl(this, drawBackdropBlock)
                    onDrawBackdrop(backdropDrawScope)
                }

                drawLayer(graphicsLayer)
            }

            drawContent()
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
        onDrawBackdrop: BackdropDrawScope.() -> Unit
    ) {
        if (this.state != state ||
            this.style != style ||
            this.onDrawBackdrop != onDrawBackdrop
        ) {
            this.state = state
            this.style = style
            this.onDrawBackdrop = onDrawBackdrop
            drawNode.invalidateDrawCache()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassNode(
    var state: LiquidGlassProviderState,
    var style: () -> GlassStyle,
    var onDrawBackdrop: BackdropDrawScope.() -> Unit
) : GlobalPositionAwareModifierNode, ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var position: Offset by mutableStateOf(Offset.Zero)
    private var graphicsLayer: GraphicsLayer? = null

    private val glassNode = delegate(
        SimpleLiquidGlassNode(
            state = state,
            style = style(),
            onDrawBackdrop = onDrawBackdrop
        )
    )

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
        onDrawBackdrop: BackdropDrawScope.() -> Unit
    ) {
        if (this.state != state ||
            this.style != style ||
            this.onDrawBackdrop != onDrawBackdrop
        ) {
            this.state = state
            this.style = style
            this.onDrawBackdrop = onDrawBackdrop
            updateStyle()
        }
    }

    private fun updateStyle() {
        observeReads {
            glassNode.update(
                state = state,
                style = style(),
                onDrawBackdrop = onDrawBackdrop
            )
        }
    }
}
