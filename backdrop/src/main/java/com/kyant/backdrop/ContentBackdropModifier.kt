package com.kyant.backdrop

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightElement
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.ShadowElement

fun Modifier.contentBackdrop(
    shapeProvider: () -> Shape,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    effects: BackdropEffectScope.() -> Unit
): Modifier {
    val shapeProvider = CachedBackdropShapeProvider(shapeProvider)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
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
            ContentBackdropElement(
                shapeProvider = shapeProvider,
                layerBlock = layerBlock,
                onDrawBehind = onDrawBehind,
                onDrawSurface = onDrawSurface,
                effects = effects
            )
        )
}

private class ContentBackdropElement(
    val shapeProvider: BackdropShapeProvider,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val effects: BackdropEffectScope.() -> Unit
) : ModifierNodeElement<ContentBackdropNode>() {

    override fun create(): ContentBackdropNode {
        return ContentBackdropNode(
            shapeProvider = shapeProvider,
            layerBlock = layerBlock,
            onDrawBehind = onDrawBehind,
            onDrawSurface = onDrawSurface,
            effects = effects
        )
    }

    override fun update(node: ContentBackdropNode) {
        node.shapeProvider = shapeProvider
        node.layerBlock = layerBlock
        node.onDrawBehind = onDrawBehind
        node.onDrawSurface = onDrawSurface
        node.effects = effects
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "contentBackdrop"
        properties["shapeProvider"] = shapeProvider
        properties["layerBlock"] = layerBlock
        properties["onDrawBehind"] = onDrawBehind
        properties["onDrawSurface"] = onDrawSurface
        properties["effects"] = effects
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContentBackdropElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (layerBlock != other.layerBlock) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (effects != other.effects) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + effects.hashCode()
        return result
    }
}

private class ContentBackdropNode(
    var shapeProvider: BackdropShapeProvider,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var effects: BackdropEffectScope.() -> Unit
) : LayoutModifierNode, DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null

    private val effectScope =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : BackdropEffectScopeImpl() {

                override val shape: Shape get() = shapeProvider.innerShape
            }
        } else {
            null
        }

    private var isCacheValid = false
    private val cacheDrawBlock: DrawScope.() -> Unit = {
        if (effectScope != null) {
            effectScope.applyDrawScope(this)
            effects(effectScope)
            graphicsLayer?.renderEffect = effectScope.renderEffect?.asComposeRenderEffect()
        }
    }

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
    }

    private var layerScope: SimpleGraphicsLayerScope? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isCacheValid) {
                observeReads { cacheDrawBlock() }
                isCacheValid = true
            }
        }

        onDrawBehind?.invoke(this)

        val layer = graphicsLayer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && layer != null) {
            layer.record {
                this@draw.drawContent()
            }

            val layerBlock = layerBlock
            if (layerBlock != null) {
                val layerScope = layerScope ?: SimpleGraphicsLayerScope().also { layerScope = it }
                layerScope.apply(this, layerBlock)
                withTransform({ transform(layerScope.matrix) }) {
                    drawLayer(layer)
                }
            } else {
                drawLayer(layer)
            }
        } else {
            drawContent()
        }

        onDrawSurface?.invoke(this)
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        isCacheValid = false
        invalidateDraw()
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
