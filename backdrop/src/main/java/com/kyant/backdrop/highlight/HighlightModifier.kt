package com.kyant.backdrop.highlight

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.RuntimeShaderCacheScopeImpl
import com.kyant.backdrop.ShapeProvider
import kotlin.math.ceil

internal class HighlightElement(
    val shapeProvider: ShapeProvider,
    val highlight: () -> Highlight?
) : ModifierNodeElement<HighlightNode>() {

    override fun create(): HighlightNode {
        return HighlightNode(shapeProvider, highlight)
    }

    override fun update(node: HighlightNode) {
        node.shapeProvider = shapeProvider
        node.highlight = highlight
        node.invalidateDraw()
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
    var shapeProvider: ShapeProvider,
    var highlight: () -> Highlight?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var highlightLayer: GraphicsLayer? = null
    private var maskLayer: GraphicsLayer? = null

    private val paint =
        Paint().apply {
            style = Paint.Style.STROKE
        }
    private var clipPath: Path? = null

    private val runtimeShaderCacheScope = RuntimeShaderCacheScopeImpl()

    private var prevStyle: HighlightStyle? = null

    override fun ContentDrawScope.draw() {
        val highlight = highlight()
        if (highlight == null || highlight.width.value <= 0f) {
            return drawContent()
        }

        drawContent()

        val highlightLayer = highlightLayer
        val maskLayer = maskLayer
        if (highlightLayer != null && maskLayer != null) {
            val size = size
            val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)

            configurePaint(highlight)

            val style = highlight.style
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (prevStyle != style) {
                    highlightLayer.renderEffect = with(style) {
                        createRenderEffect(
                            shape = shapeProvider.shape,
                            shaderCache = runtimeShaderCacheScope
                        )
                    }
                    prevStyle = style
                }
            }
            highlightLayer.record { drawHighlight(outline) }

            maskLayer.alpha = highlight.alpha
            maskLayer.blendMode = style.blendMode
            maskLayer.record { drawLayer(highlightLayer) }

            drawLayer(maskLayer)
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        highlightLayer = graphicsContext.createGraphicsLayer()
        maskLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        highlightLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            highlightLayer = null
        }
        maskLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            maskLayer = null
        }
        clipPath = null
        prevStyle = null
    }

    private fun DrawScope.configurePaint(highlight: Highlight) {
        paint.color = highlight.style.color.toArgb()
        paint.strokeWidth =
            ceil(highlight.width.toPx().fastCoerceAtMost(size.minDimension / 2f)) * 2f
        val blurRadius = highlight.blurRadius.toPx()
        paint.maskFilter =
            if (blurRadius > 0f) {
                BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            } else {
                null
            }
    }

    private fun DrawScope.drawHighlight(outline: Outline) {
        val canvas = drawContext.canvas.nativeCanvas

        @Suppress("UseKtx")
        canvas.save()

        when (outline) {
            is Outline.Rectangle -> {
                canvas.clipRect(0f, 0f, size.width, size.height)
                canvas.drawRect(0f, 0f, size.width, size.height, paint)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.clipPath(path)
                    canvas.drawPath(path, paint)
                } else {
                    val rr = outline.roundRect
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    val clipPath = clipPath?.apply { rewind() } ?: Path().also { clipPath = it }
                    clipPath.addRoundRect(rr.left, rr.top, rr.right, rr.bottom, radius, radius, Path.Direction.CW)
                    canvas.clipPath(clipPath)
                    canvas.drawRoundRect(rr.left, rr.top, rr.right, rr.bottom, radius, radius, paint)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.clipPath(path)
                canvas.drawPath(path, paint)
            }
        }

        canvas.restore()
    }
}
