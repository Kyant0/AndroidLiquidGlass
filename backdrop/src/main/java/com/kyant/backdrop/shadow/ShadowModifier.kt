package com.kyant.backdrop.shadow

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.ShapeProvider
import kotlin.math.ceil

internal class ShadowElement(
    val shapeProvider: ShapeProvider,
    val shadow: () -> Shadow?
) : ModifierNodeElement<ShadowNode>() {

    override fun create(): ShadowNode {
        return ShadowNode(shapeProvider, shadow)
    }

    override fun update(node: ShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
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
    var shapeProvider: ShapeProvider,
    var shadow: () -> Shadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()

    override fun ContentDrawScope.draw() {
        val shadow = shadow() ?: return drawContent()

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()
            val offsetX = shadow.offset.x.toPx()
            val offsetY = shadow.offset.y.toPx()
            val shadowSize = IntSize(
                ceil(size.width + radius * 4f + offsetX).toInt(),
                ceil(size.height + radius * 4f + offsetY).toInt()
            )
            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)

            configurePaint(shadow)

            shadowLayer.alpha = shadow.alpha
            shadowLayer.blendMode = shadow.blendMode
            shadowLayer.record(shadowSize) {
                translate(radius * 2f + offsetX, radius * 2f + offsetY) {
                    drawShadow(outline, offsetX, offsetY)
                }
            }

            translate(-radius * 2f, -radius * 2f) {
                drawLayer(shadowLayer)
            }
        }

        drawContent()
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

    private fun DrawScope.configurePaint(shadow: Shadow) {
        paint.color = shadow.color.toArgb()
        val blurRadius = shadow.radius.toPx()
        paint.maskFilter =
            if (blurRadius > 0f) {
                BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            } else {
                null
            }
    }

    private fun DrawScope.drawShadow(outline: Outline, offsetX: Float, offsetY: Float) {
        val canvas = drawContext.canvas.nativeCanvas

        when (outline) {
            is Outline.Rectangle -> {
                val left = outline.rect.left
                val top = outline.rect.top
                val right = outline.rect.right
                val bottom = outline.rect.bottom
                canvas.drawRect(left, top, right, bottom, paint)
                canvas.translate(-offsetX, -offsetY)
                canvas.drawRect(left, top, right, bottom, ShadowMaskPaint)
                canvas.translate(offsetX, offsetY)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
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
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, ShadowMaskPaint)
                    canvas.translate(offsetX, offsetY)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.drawPath(path, paint)
                canvas.translate(-offsetX, -offsetY)
                canvas.drawPath(path, ShadowMaskPaint)
                canvas.translate(offsetX, offsetY)
            }
        }
    }
}

private val ShadowMaskPaint = Paint().apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        blendMode = android.graphics.BlendMode.CLEAR
    } else {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}
