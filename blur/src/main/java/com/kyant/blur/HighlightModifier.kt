package com.kyant.blur

import androidx.compose.ui.Modifier
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
import kotlin.math.ceil
import kotlin.math.min

fun Modifier.highlight(highlight: Highlight): Modifier =
    this then SimpleHighlightElement(highlight)

internal class SimpleHighlightElement(
    val highlight: Highlight
) : ModifierNodeElement<SimpleHighlightNode>() {

    override fun create(): SimpleHighlightNode {
        return SimpleHighlightNode(highlight)
    }

    override fun update(node: SimpleHighlightNode) {
        node.update(highlight)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "highlight"
        properties["highlight"] = highlight
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleHighlightElement) return false

        if (highlight != other.highlight) return false

        return true
    }

    override fun hashCode(): Int {
        return highlight.hashCode()
    }
}

internal class HighlightElement(
    val highlight: () -> Highlight
) : ModifierNodeElement<HighlightNode>() {

    override fun create(): HighlightNode {
        return HighlightNode(highlight)
    }

    override fun update(node: HighlightNode) {
        node.update(highlight)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "highlight"
        properties["highlight"] = highlight
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightElement) return false

        if (highlight != other.highlight) return false

        return true
    }

    override fun hashCode(): Int {
        return highlight.hashCode()
    }
}

internal class SimpleHighlightNode(
    var highlight: Highlight
) : DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val highlight = highlight
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
                highlight.shape.createOutline(borderSize, layoutDirection, this)
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
        highlight: Highlight
    ) {
        if (this.highlight != highlight) {
            this.highlight = highlight
            drawNode.invalidateDrawCache()
        }
    }
}

internal class HighlightNode(
    var highlight: () -> Highlight
) : ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private val drawNode = delegate(
        SimpleHighlightNode(highlight())
    )

    override fun onObservedReadsChanged() {
        updateHighlight()
    }

    override fun onAttach() {
        updateHighlight()
    }

    fun update(
        highlight: () -> Highlight
    ) {
        if (this.highlight != highlight) {
            this.highlight = highlight
            updateHighlight()
        }
    }

    private fun updateHighlight() {
        observeReads {
            val highlight = highlight()
            drawNode.update(highlight)
        }
    }
}
