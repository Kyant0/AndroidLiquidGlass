package com.kyant.backdrop

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightElement
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.ShadowElement

private val DefaultHighlight = { Highlight() }
private val DefaultShadow = { Shadow() }
private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shapeProvider: () -> Shape,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    effects: BackdropEffectScope.() -> Unit
): Modifier {
    val shapeProvider = CachedBackdropShapeProvider(shapeProvider)
    return this
        .then(
            if (shadow != null) {
                ShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = shadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (highlight != null) {
                HighlightElement(
                    shapeProvider = shapeProvider,
                    highlight = highlight
                )
            } else {
                Modifier
            }
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                onDrawBackdrop = onDrawBackdrop,
                onDrawSurface = onDrawSurface
            )
        )
}

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shapeProvider: BackdropShapeProvider,
    val effects: BackdropEffectScope.() -> Unit,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode {
        return DrawBackdropNode(
            backdrop = backdrop,
            shapeProvider = shapeProvider,
            effects = effects,
            onDrawBackdrop = onDrawBackdrop,
            onDrawSurface = onDrawSurface
        )
    }

    override fun update(node: DrawBackdropNode) {
        node.backdrop = backdrop
        node.shapeProvider = shapeProvider
        node.effects = effects
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.drawNode.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["shapeProvider"] = shapeProvider
        properties["effects"] = effects
        properties["onDrawBackdrop"] = onDrawBackdrop
        properties["onDrawSurface"] = onDrawSurface
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (shapeProvider != other.shapeProvider) return false
        if (effects != other.effects) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false
        if (onDrawSurface != other.onDrawSurface) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shapeProvider.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + onDrawBackdrop.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shapeProvider: BackdropShapeProvider,
    var effects: BackdropEffectScope.() -> Unit,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?
) : LayoutModifierNode, GlobalPositionAwareModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private var drawBackdropBlock: DrawScope.() -> Unit by mutableStateOf({})

    private val effectScope =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : BackdropEffectScope {

                private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

                override var density: Float by mutableFloatStateOf(1f)
                override var fontScale: Float by mutableFloatStateOf(1f)
                override var size: Size by mutableStateOf(Size.Unspecified)
                override var layoutDirection: LayoutDirection by mutableStateOf(LayoutDirection.Ltr)
                override val shape: Shape get() = shapeProvider.innerShape
                override var renderEffect: RenderEffect? = null

                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
                    return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
                }
            }
        } else {
            null
        }

    val drawNode = delegate(CacheDrawModifierNode {
        if (effectScope != null) {
            effectScope.density = density
            effectScope.fontScale = fontScale
            effectScope.size = size
            effectScope.layoutDirection = layoutDirection
            effectScope.renderEffect = null

            effects(effectScope)

            graphicsLayer?.renderEffect = effectScope.renderEffect?.asComposeRenderEffect()
        }

        onDrawWithContent {
            graphicsLayer?.let { layer ->
                layer.record { onDrawBackdrop(drawBackdropBlock) }
                drawLayer(layer)
            }
            onDrawSurface?.invoke(this)
            drawContent()
        }
    })

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layerBlock)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            drawBackdropBlock = { with(backdrop) { drawBackdrop(coordinates) } }
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer = graphicsContext.createGraphicsLayer()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
    }
}
