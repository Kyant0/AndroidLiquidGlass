package com.kyant.backdrop.highlight

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropShapeProvider
import com.kyant.backdrop.RuntimeShaderCacheScope
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
        node.invalidateDrawCache()
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
) : DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint =
        Paint().apply {
            style = Paint.Style.STROKE
        }
    private val maskPaint =
        Paint().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = BlendMode.MODULATE
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            }
        }
    private var prevSize = Size.Unspecified
    private var clipPath: Path? = null

    private val runtimeShaderCacheScope = object : RuntimeShaderCacheScope {

        private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
            return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
        }
    }

    private var isCacheValid = false
    private var _highlight: Highlight? = null
    private val cacheDrawBlock: DrawScope.() -> Unit = cacheDrawBlock@{
        val highlight = highlight().also { _highlight = it }
        val graphicsLayer = graphicsLayer
        val size = size
        if (highlight == null || graphicsLayer == null ||
            highlight.width.value <= 0f || highlight.color.isUnspecified ||
            size.width < 0.5f || size.height < 0.5f
        ) {
            _highlight = null
            return@cacheDrawBlock
        }

        paint.color = highlight.color.toArgb()
        val strokeWidth = ceil(highlight.width.toPx().fastCoerceAtMost(size.minDimension / 2f))
        paint.strokeWidth = strokeWidth * 2f
        val blurRadius = highlight.blurRadius.toPx()
        if (blurRadius > 0f) {
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }

        if (prevSize != size) {
            bitmap = null
            canvas = null
            prevSize = size
        }

        @Suppress("UseKtx")
        val bitmap =
            bitmap ?: Bitmap.createBitmap(
                ceil(size.width).toInt(),
                ceil(size.height).toInt(),
                Bitmap.Config.ARGB_8888
            ).also { bitmap = it }
        val canvas =
            canvas?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
                } else {
                    drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                }
            } ?: Canvas(bitmap).also { canvas = it }

        when (val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> {
                val rect = RectF(0f, 0f, size.width, size.height)
                canvas.clipRect(rect)
                canvas.drawRect(rect, paint)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.clipPath(path)
                    canvas.drawPath(path, paint)
                } else {
                    val rect = with(outline.roundRect) { RectF(left, top, right, bottom) }
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    val clipPath = clipPath?.apply { rewind() } ?: Path().also { clipPath = it }
                    clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
                    canvas.clipPath(clipPath)
                    canvas.drawRoundRect(rect, radius, radius, paint)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.clipPath(path)
                canvas.drawPath(path, paint)
            }
        }

        bitmap.prepareToDraw()
    }

    override fun ContentDrawScope.draw() {
        if (!isCacheValid) {
            observeReads { cacheDrawBlock() }
            isCacheValid = true
        }

        drawContent()

        val highlight = _highlight
        val graphicsLayer = graphicsLayer
        val bitmap = bitmap
        if (highlight != null && graphicsLayer != null && bitmap != null) {
            val maskShader = with(highlight.style()) { runtimeShaderCacheScope.createShader(size) }
            maskPaint.shader = maskShader
            graphicsLayer.blendMode = highlight.blendMode
            graphicsLayer.record {
                val canvas = drawContext.canvas.nativeCanvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                if (maskShader != null) {
                    canvas.drawPaint(maskPaint)
                }
            }
            drawLayer(graphicsLayer)
        }
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
