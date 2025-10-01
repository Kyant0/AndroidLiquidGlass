package com.kyant.backdrop.shadow

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.TileMode
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
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.ShapeProvider

internal class InnerShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> InnerShadow?
) : ModifierNodeElement<InnerShadowNode>() {

    override fun create(): InnerShadowNode {
        return InnerShadowNode(shapeProvider, shadow)
    }

    override fun update(node: InnerShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false

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

internal class InnerShadowNode(
    var shapeProvider: ShapeProvider,
    var shadow: () -> InnerShadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private var clipPath: Path? = null

    private var prevRadius = Float.NaN

    override fun ContentDrawScope.draw() {
        drawContent()

        val shadow = shadow() ?: return

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()
            val offsetX = shadow.offset.x.toPx()
            val offsetY = shadow.offset.y.toPx()

            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)

            configurePaint(shadow)

            shadowLayer.alpha = shadow.alpha
            shadowLayer.blendMode = shadow.blendMode
            if (prevRadius != radius) {
                shadowLayer.renderEffect =
                    if (radius > 0f) {
                        BlurEffect(radius, radius, TileMode.Decal)
                    } else {
                        null
                    }
                prevRadius = radius
            }
            shadowLayer.record { drawShadow(outline, -offsetX, -offsetY) }

            drawMaskedShadow(outline, shadowLayer)
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            shadowLayer = null
        }
    }

    private fun DrawScope.configurePaint(shadow: InnerShadow) {
        paint.color = shadow.color.toArgb()
    }

    private fun DrawScope.drawShadow(outline: Outline, offsetX: Float, offsetY: Float) {
        val canvas = drawContext.canvas.nativeCanvas

        @Suppress("UseKtx")
        canvas.save()

        when (outline) {
            is Outline.Rectangle -> {
                val left = outline.rect.left
                val top = outline.rect.top
                val right = outline.rect.right
                val bottom = outline.rect.bottom
                canvas.clipRect(left, top, right, bottom)
                canvas.drawRect(left, top, right, bottom, paint)
                canvas.translate(-offsetX, -offsetY)
                canvas.drawRect(left, top, right, bottom, ShadowMaskPaint)
                canvas.translate(offsetX, offsetY)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.clipPath(path)
                    canvas.drawPath(path, paint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.drawPath(path, ShadowMaskPaint)
                    canvas.translate(offsetX, offsetY)
                } else {
                    val left = outline.roundRect.left
                    val top = outline.roundRect.top
                    val right = outline.roundRect.right
                    val bottom = outline.roundRect.bottom
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    val clipPath = clipPath?.apply { rewind() } ?: Path().also { clipPath = it }
                    clipPath.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
                    canvas.clipPath(clipPath)
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, ShadowMaskPaint)
                    canvas.translate(offsetX, offsetY)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.clipPath(path)
                canvas.drawPath(path, paint)
                canvas.translate(-offsetX, -offsetY)
                canvas.drawPath(path, ShadowMaskPaint)
                canvas.translate(offsetX, offsetY)
            }
        }

        canvas.restore()
    }

    private fun DrawScope.drawMaskedShadow(outline: Outline, layer: GraphicsLayer) {
        val canvas = drawContext.canvas.nativeCanvas

        @Suppress("UseKtx")
        canvas.save()

        when (outline) {
            is Outline.Rectangle -> {
                canvas.clipRect(0f, 0f, size.width, size.height)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.clipPath(path)
                } else {
                    val rr = outline.roundRect
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    val clipPath = clipPath?.apply { rewind() } ?: Path().also { clipPath = it }
                    clipPath.addRoundRect(rr.left, rr.top, rr.right, rr.bottom, radius, radius, Path.Direction.CW)
                    canvas.clipPath(clipPath)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.clipPath(path)
            }
        }

        drawLayer(layer)

        canvas.restore()
    }
}

private val ShadowMaskPaint = Paint().apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        blendMode = android.graphics.BlendMode.CLEAR
    } else {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}
