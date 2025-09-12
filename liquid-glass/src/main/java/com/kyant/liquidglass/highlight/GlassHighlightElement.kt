package com.kyant.liquidglass.highlight

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.toIntSize
import com.kyant.liquidglass.GlassStyle
import kotlin.math.ceil
import kotlin.math.min

internal class SimpleGlassHighlightElement(
    val style: GlassStyle
) : ModifierNodeElement<SimpleGlassHighlightNode>() {

    override fun create(): SimpleGlassHighlightNode {
        return SimpleGlassHighlightNode(style)
    }

    override fun update(node: SimpleGlassHighlightNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassHighlight"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassHighlightElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class GlassHighlightElement(
    val style: () -> GlassStyle
) : ModifierNodeElement<GlassHighlightNode>() {

    override fun create(): GlassHighlightNode {
        return GlassHighlightNode(style)
    }

    override fun update(node: GlassHighlightNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassHighlight"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassHighlightElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class SimpleGlassHighlightNode(
    var style: GlassStyle
) : DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var highlight: GlassHighlight? = style.highlight
        set(value) {
            if (field != value) {
                field = value
                drawNode.invalidateDrawCache()
            }
        }

    private var shape: CornerBasedShape = style.shape
        set(value) {
            if (field != value) {
                field = value
                drawNode.invalidateDrawCache()
            }
        }

    private var graphicsLayer: GraphicsLayer? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val highlight = highlight
        if (highlight == null) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }

        val width = highlight.width
        val color = highlight.color

        val strokeWidthPx =
            min(
                if (width == Dp.Hairline) 1f else ceil(width.toPx()),
                ceil(size.minDimension / 2f),
            )
        val halfStroke = strokeWidthPx / 2
        val borderTopLeft = Offset(halfStroke, halfStroke)
        val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        val outline =
            if (width.isSpecified && color.isSpecified) {
                shape.createOutline(borderSize, layoutDirection, this)
            } else {
                null
            }

        if (outline != null) {
            graphicsLayer?.let { layer ->
                val borderRenderEffect = highlight.style.createRenderEffect(size, strokeWidthPx)
                layer.renderEffect = borderRenderEffect?.asComposeRenderEffect()
                layer.blendMode = highlight.blendMode

                layer.record(this, layoutDirection, size.toIntSize()) {
                    draw(
                        this@CacheDrawModifierNode,
                        layoutDirection,
                        drawContext.canvas,
                        drawContext.size,
                        drawContext.graphicsLayer
                    ) {
                        translate(borderTopLeft.x, borderTopLeft.y) {
                            drawOutline(
                                outline = outline,
                                brush = SolidColor(color),
                                style = Stroke(strokeWidthPx)
                            )
                        }
                    }
                }
            }
        }

        onDrawBehind {
            if (outline != null) {
                graphicsLayer?.let { layer ->
                    drawLayer(layer)
                }
            }
        }
    })

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
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
        style: GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            highlight = style.highlight
            shape = style.shape
        }
    }
}

internal class GlassHighlightNode(
    var style: () -> GlassStyle
) : ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var highlight: GlassHighlight? = style().highlight
        set(value) {
            if (field != value) {
                field = value
                drawNode.invalidateDrawCache()
            }
        }

    private var shape: CornerBasedShape = style().shape
        set(value) {
            if (field != value) {
                field = value
                drawNode.invalidateDrawCache()
            }
        }

    private var graphicsLayer: GraphicsLayer? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val highlight = highlight
        if (highlight == null) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }

        val width = highlight.width
        val color = highlight.color

        val strokeWidthPx =
            min(
                if (width == Dp.Hairline) 1f else ceil(width.toPx()),
                ceil(size.minDimension / 2f),
            )
        val halfStroke = strokeWidthPx / 2
        val borderTopLeft = Offset(halfStroke, halfStroke)
        val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        val outline =
            if (width.isSpecified && color.isSpecified) {
                shape.createOutline(borderSize, layoutDirection, this)
            } else {
                null
            }

        if (outline != null) {
            graphicsLayer?.let { layer ->
                val borderRenderEffect = highlight.style.createRenderEffect(size, strokeWidthPx)
                layer.renderEffect = borderRenderEffect?.asComposeRenderEffect()
                layer.blendMode = highlight.blendMode

                layer.record(this, layoutDirection, size.toIntSize()) {
                    draw(
                        this@CacheDrawModifierNode,
                        layoutDirection,
                        drawContext.canvas,
                        drawContext.size,
                        drawContext.graphicsLayer
                    ) {
                        translate(borderTopLeft.x, borderTopLeft.y) {
                            drawOutline(
                                outline = outline,
                                brush = SolidColor(color),
                                style = Stroke(strokeWidthPx)
                            )
                        }
                    }
                }
            }
        }

        onDrawBehind {
            if (outline != null) {
                graphicsLayer?.let { layer ->
                    drawLayer(layer)
                }
            }
        }
    })

    override fun onObservedReadsChanged() {
        updateHighlight()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }

        updateHighlight()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
    }

    fun update(
        style: () -> GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            updateHighlight()
        }
    }

    private fun updateHighlight() {
        observeReads {
            val style = style()
            highlight = style.highlight
            shape = style.shape
        }
    }
}
