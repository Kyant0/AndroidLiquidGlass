package com.kyant.backdrop.highlight

import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropShapeProvider
import kotlin.math.ceil

internal class HighlightElement(
    val shapeProvider: BackdropShapeProvider,
    val highlight: () -> Highlight?
) : ModifierNodeElement<HighlightNode>() {

    override fun create(): HighlightNode {
        return HighlightNode(shapeProvider, highlight)
    }

    override fun update(node: HighlightNode) {
        node.shapeProvider = shapeProvider
        node.highlight = highlight
        node.drawNode.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "highlight"
        properties["shapeProvider"] = shapeProvider
        properties["highlight"] = highlight
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (highlight != other.highlight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + highlight.hashCode()
        return result
    }
}

internal class HighlightNode(
    var shapeProvider: BackdropShapeProvider,
    var highlight: () -> Highlight?
) : LayoutModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private val paint =
        Paint().apply {
            style = Paint.Style.STROKE
        }

    val drawNode = delegate(CacheDrawModifierNode {
        val highlight = highlight()
        val graphicsLayer = graphicsLayer
        if (highlight == null || graphicsLayer == null ||
            highlight.width.value <= 0f || highlight.color.isUnspecified
        ) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }

        val size = size
        val strokeWidth = ceil(highlight.width.toPx().fastCoerceAtMost(size.minDimension / 2f))
        val borderSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val outline = shapeProvider.shape.createOutline(borderSize, layoutDirection, this)

        paint.color = highlight.color.toArgb()
        paint.strokeWidth = strokeWidth
        paint.asComposePaint().blendMode = highlight.blendMode

        graphicsLayer.blendMode = highlight.blendMode
        graphicsLayer.record {
            translate(strokeWidth / 2f, strokeWidth / 2f) {
                val canvas = drawContext.canvas.nativeCanvas

                when (outline) {
                    is Outline.Rectangle -> {
                        val rect = RectF(0f, 0f, borderSize.width, borderSize.height)
                        canvas.drawRect(rect, paint)
                    }

                    is Outline.Rounded -> {
                        @Suppress("INVISIBLE_REFERENCE")
                        val path = outline.roundRectPath?.asAndroidPath()
                        if (path != null) {
                            canvas.drawPath(path, paint)
                        } else {
                            val rect = with(outline.roundRect) { RectF(left, top, right, bottom) }
                            val radius = outline.roundRect.topLeftCornerRadius.x
                            canvas.drawRoundRect(rect, radius, radius, paint)
                        }
                    }

                    is Outline.Generic -> {
                        val path = outline.path.asAndroidPath()
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        onDrawWithContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                graphicsLayer.renderEffect =
                    highlight.style().createRenderEffect(size, strokeWidth)?.asComposeRenderEffect()
            }
            drawContent()
            drawLayer(graphicsLayer)
        }
    })

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
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
