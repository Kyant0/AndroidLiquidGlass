package com.kyant.backdrop.shadow

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.BackdropShapeProvider
import kotlin.math.ceil

internal class ShadowElement(
    val shapeProvider: BackdropShapeProvider,
    val shadow: () -> Shadow?
) : ModifierNodeElement<ShadowNode>() {

    override fun create(): ShadowNode {
        return ShadowNode(shapeProvider, shadow)
    }

    override fun update(node: ShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShadowElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal class ShadowNode(
    var shapeProvider: BackdropShapeProvider,
    var shadow: () -> Shadow?
) : DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint()
    private val maskPaint =
        Paint().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = BlendMode.DST_OUT
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
        }
    private val drawPaint = Paint()
    private var prevSize = Size.Unspecified

    private var _shadow: Shadow? = null
    private var isCacheValid = false
    private val cacheDrawBlock: DrawScope.() -> Unit = cacheDrawBlock@{
        val shadow = shadow().also { _shadow = it }
        val size = size
        if (shadow == null ||
            shadow.elevation.value <= 0f || shadow.color.isUnspecified
        ) {
            _shadow = null
            return@cacheDrawBlock
        }

        val elevation = shadow.elevation.toPx()
        val offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())
        val shadowSize = IntSize(
            ceil(size.width + elevation * 2).toInt(),
            ceil(size.height + elevation * 2).toInt()
        )
        if (shadowSize.width < 0.5f || shadowSize.height < 0.5f) {
            _shadow = null
            return@cacheDrawBlock
        }

        paint.setShadowLayer(elevation, offset.x, offset.y, shadow.color.toArgb())
        drawPaint.asComposePaint().blendMode = shadow.blendMode

        if (prevSize != size) {
            bitmap = null
            canvas = null
            prevSize = size
        }

        @Suppress("UseKtx")
        val bitmap =
            bitmap ?: Bitmap.createBitmap(
                shadowSize.width,
                shadowSize.height,
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

        val dx = elevation - offset.x
        val dy = elevation - offset.y
        canvas.translate(dx, dy)

        when (val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> {
                val rect = RectF(0f, 0f, size.width, size.height)
                canvas.drawRect(rect, paint)
                canvas.drawRect(rect, maskPaint)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.drawPath(path, paint)
                    canvas.drawPath(path, maskPaint)
                } else {
                    val rect = with(outline.roundRect) { RectF(left, top, right, bottom) }
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    canvas.drawRoundRect(rect, radius, radius, paint)
                    canvas.drawRoundRect(rect, radius, radius, maskPaint)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.drawPath(path, paint)
                canvas.drawPath(path, maskPaint)
            }
        }

        canvas.translate(-dx, -dy)
    }

    override fun ContentDrawScope.draw() {
        if (!isCacheValid) {
            observeReads { cacheDrawBlock() }
            isCacheValid = true
        }

        val shadow = _shadow
        if (shadow != null) {
            val canvas = drawContext.canvas.nativeCanvas
            val elevation = shadow.elevation.toPx()
            val dx = -(elevation - shadow.offset.x.toPx())
            val dy = -(elevation - shadow.offset.y.toPx())
            canvas.translate(dx, dy)
            bitmap?.let { canvas.drawBitmap(it, 0f, 0f, drawPaint) }
            canvas.translate(-dx, -dy)
        }

        drawContent()
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        isCacheValid = false
        invalidateDraw()
    }
}
