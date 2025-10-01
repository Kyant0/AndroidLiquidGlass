package com.kyant.backdrop

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightElement
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.InnerShadowElement
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.ShadowElement
import kotlin.math.ceil

private val DefaultHighlight = { Highlight.Default }
private val DefaultShadow = { Shadow.Default }
private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }
private val DefaultOnDrawContent: ContentDrawScope.() -> Unit = { drawContent() }

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    innerShadow: (() -> InnerShadow?)? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    contentEffects: (BackdropEffectScope.() -> Unit)? = null,
    onDrawContent: ContentDrawScope.() -> Unit = DefaultOnDrawContent,
    drawContent: Boolean = true
): Modifier {
    val shapeProvider = ShapeProvider(shape)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            if (innerShadow != null) {
                InnerShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = innerShadow
                )
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
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                exportedBackdrop = exportedBackdrop,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = onDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront,
                contentEffects = contentEffects,
                onDrawContent = onDrawContent,
                drawContent = drawContent
            )
        )
}

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shapeProvider: ShapeProvider,
    val effects: (BackdropEffectScope.() -> Unit)?,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val exportedBackdrop: LayerBackdrop?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?,
    val contentEffects: (BackdropEffectScope.() -> Unit)?,
    val onDrawContent: ContentDrawScope.() -> Unit,
    val drawContent: Boolean
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode {
        return DrawBackdropNode(
            backdrop = backdrop,
            shapeProvider = shapeProvider,
            effects = effects,
            layerBlock = layerBlock,
            exportedBackdrop = exportedBackdrop,
            onDrawBehind = onDrawBehind,
            onDrawBackdrop = onDrawBackdrop,
            onDrawSurface = onDrawSurface,
            onDrawFront = onDrawFront,
            contentEffects = contentEffects,
            onDrawContent = onDrawContent,
            drawContent = drawContent
        )
    }

    override fun update(node: DrawBackdropNode) {
        node.backdrop = backdrop
        node.shapeProvider = shapeProvider
        node.effects = effects
        node.layerBlock = layerBlock
        node.exportedBackdrop = exportedBackdrop
        node.onDrawBehind = onDrawBehind
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        node.contentEffects = contentEffects
        node.onDrawContent = onDrawContent
        node.drawContent = drawContent
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["shapeProvider"] = shapeProvider
        properties["effects"] = effects
        properties["layerBlock"] = layerBlock
        properties["exportedBackdrop"] = exportedBackdrop
        properties["onDrawBehind"] = onDrawBehind
        properties["onDrawBackdrop"] = onDrawBackdrop
        properties["onDrawSurface"] = onDrawSurface
        properties["onDrawFront"] = onDrawFront
        properties["contentEffects"] = contentEffects
        properties["onDrawContent"] = onDrawContent
        properties["drawContent"] = drawContent
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (shapeProvider != other.shapeProvider) return false
        if (effects != other.effects) return false
        if (layerBlock != other.layerBlock) return false
        if (exportedBackdrop != other.exportedBackdrop) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (onDrawFront != other.onDrawFront) return false
        if (contentEffects != other.contentEffects) return false
        if (onDrawContent != other.onDrawContent) return false
        if (drawContent != other.drawContent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shapeProvider.hashCode()
        result = 31 * result + (effects?.hashCode() ?: 0)
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (exportedBackdrop?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + onDrawBackdrop.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + (onDrawFront?.hashCode() ?: 0)
        result = 31 * result + (contentEffects?.hashCode() ?: 0)
        result = 31 * result + onDrawContent.hashCode()
        result = 31 * result + drawContent.hashCode()
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shapeProvider: ShapeProvider,
    var effects: (BackdropEffectScope.() -> Unit)?,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var exportedBackdrop: LayerBackdrop?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?,
    var contentEffects: (BackdropEffectScope.() -> Unit)?,
    var onDrawContent: ContentDrawScope.() -> Unit,
    var drawContent: Boolean
) : LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private val effectScope =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : BackdropEffectScopeImpl() {

                override val shape: Shape get() = shapeProvider.innerShape
            }
        } else {
            null
        }

    private var backdropGraphicsLayer: GraphicsLayer? = null
    private var contentGraphicsLayer: GraphicsLayer? = null

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
    }

    private var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    private val recordBackdropBlock: (DrawScope.() -> Unit) = {
        onDrawBackdrop {
            with(backdrop) {
                translate(1f, 1f) {
                    drawBackdrop(
                        density = requireDensity(),
                        coordinates = layoutCoordinates,
                        layerBlock = layerBlock
                    )
                }
            }
        }
    }
    private val drawBackdropLayer: DrawScope.() -> Unit = {
        val layer = backdropGraphicsLayer
        if (layer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val safeSize =
                    IntSize(
                        ceil(size.width).toInt() + 2,
                        ceil(size.height).toInt() + 2
                    )
                layer.record(
                    density = requireDensity(),
                    layoutDirection = layoutDirection,
                    size = safeSize,
                    block = recordBackdropBlock
                )
                translate(-1f, -1f) {
                    drawLayer(layer)
                }
            } else {
                recordBackdropBlock()
            }
        }
    }
    private val drawContentLayer: ContentDrawScope.() -> Unit = draw@{
        val layer = contentGraphicsLayer
        if (layer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val safeSize =
                    IntSize(
                        ceil(size.width).toInt() + 2,
                        ceil(size.height).toInt() + 2
                    )
                layer.record(safeSize) {
                    translate(1f, 1f) {
                        this@draw.onDrawContent()
                    }
                }
                translate(-1f, -1f) {
                    drawLayer(layer)
                }
            } else {
                onDrawContent()
            }
        }
    }

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
            if (effectScope != null && effectScope.update(this)) {
                updateEffects()
            }
        }

        onDrawBehind?.invoke(this)
        drawBackdropLayer()
        onDrawSurface?.invoke(this)
        drawContentLayer()
        if (drawContent) drawContent()
        onDrawFront?.invoke(this)

        exportedBackdrop?.graphicsLayer?.record {
            onDrawBehind?.invoke(this)
            drawBackdropLayer()
            onDrawSurface?.invoke(this)
            onDrawFront?.invoke(this)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (backdrop.isCoordinatesDependent) {
                layoutCoordinates = coordinates
            } else {
                if (layoutCoordinates != null) {
                    layoutCoordinates = null
                }
            }
            exportedBackdrop?.currentCoordinates = coordinates
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        observeEffects()
        invalidateDraw()
    }

    private fun observeEffects() {
        if (effectScope != null) {
            observeReads { updateEffects() }
        }
    }

    private fun updateEffects() {
        if (effectScope != null) {
            val graphicsContext = requireGraphicsContext()

            val backdropEffects = effects
            if (backdropEffects != null) {
                if (backdropGraphicsLayer == null) {
                    backdropGraphicsLayer = graphicsContext.createGraphicsLayer()
                }

                effectScope.renderEffect = null
                backdropEffects(effectScope)
                backdropGraphicsLayer?.renderEffect = effectScope.renderEffect?.asComposeRenderEffect()
            } else {
                backdropGraphicsLayer?.let { layer ->
                    graphicsContext.releaseGraphicsLayer(layer)
                    backdropGraphicsLayer = null
                }
            }

            val contentEffects = contentEffects
            if (contentEffects != null) {
                if (contentGraphicsLayer == null) {
                    val graphicsContext = requireGraphicsContext()
                    contentGraphicsLayer = graphicsContext.createGraphicsLayer()
                }

                effectScope.renderEffect = null
                contentEffects(effectScope)
                contentGraphicsLayer?.renderEffect = effectScope.renderEffect?.asComposeRenderEffect()
            } else {
                contentGraphicsLayer?.let { layer ->
                    graphicsContext.releaseGraphicsLayer(layer)
                    contentGraphicsLayer = null
                }
            }
        }
    }

    override fun onAttach() {
        observeEffects()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        backdropGraphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            backdropGraphicsLayer = null
        }
        contentGraphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            contentGraphicsLayer = null
        }
        effectScope?.reset()
        layoutCoordinates = null
        exportedBackdrop?.currentCoordinates = null
    }
}
