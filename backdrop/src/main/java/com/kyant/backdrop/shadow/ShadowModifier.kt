package com.kyant.backdrop.shadow

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.TileMode
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
    private var maskLayer: GraphicsLayer? = null

    private val shadowPaint = Paint()

    private var prevRadius = Float.NaN

    override fun ContentDrawScope.draw() {
        val shadow = shadow() ?: return drawContent()

        val shadowLayer = shadowLayer
        val maskLayer = maskLayer
        if (shadowLayer != null && maskLayer != null) {
            val size = size
            val radius = shadow.radius.toPx()
            val offsetX = shadow.offset.x.toPx()
            val offsetY = shadow.offset.y.toPx()
            val shadowSize = IntSize(
                ceil(size.width + radius * 2 + offsetX).toInt(),
                ceil(size.height + radius * 2 + offsetY).toInt()
            )
            val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)

            configurePaint(shadow)

            if (prevRadius != radius) {
                shadowLayer.renderEffect =
                    if (radius > 0f) {
                        BlurEffect(radius, radius, TileMode.Decal)
                    } else {
                        null
                    }
                prevRadius = radius
            }
            shadowLayer.record { drawOutline(outline, shadowPaint) }

            maskLayer.blendMode = shadow.blendMode
            maskLayer.record(size = shadowSize) {
                translate(radius + offsetX, radius + offsetY) {
                    drawLayer(shadowLayer)
                    translate(-offsetX, -offsetY) {
                        drawOutline(outline, ShadowMaskPaint)
                    }
                }
            }

            translate(-radius, -radius) {
                drawLayer(maskLayer)
            }
        }

        drawContent()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer = graphicsContext.createGraphicsLayer()
        maskLayer =
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
        maskLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            maskLayer = null
        }
        prevRadius = Float.NaN
    }

    private fun DrawScope.configurePaint(shadow: Shadow) {
        shadowPaint.color = shadow.color.modulate(shadow.alpha).toArgb()
    }

    private fun DrawScope.drawOutline(outline: Outline, paint: Paint) {
        val canvas = drawContext.canvas.nativeCanvas

        when (outline) {
            is Outline.Rectangle -> {
                canvas.drawRect(0f, 0f, size.width, size.height, paint)
            }

            is Outline.Rounded -> {
                @Suppress("INVISIBLE_REFERENCE")
                val path = outline.roundRectPath?.asAndroidPath()
                if (path != null) {
                    canvas.drawPath(path, paint)
                } else {
                    val rr = outline.roundRect
                    val radius = outline.roundRect.topLeftCornerRadius.x
                    canvas.drawRoundRect(rr.left, rr.top, rr.right, rr.bottom, radius, radius, paint)
                }
            }

            is Outline.Generic -> {
                val path = outline.path.asAndroidPath()
                canvas.drawPath(path, paint)
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

private fun Color.modulate(alpha: Float): Color =
    if (alpha != 1.0f) {
        copy(alpha = this.alpha * alpha)
    } else {
        this
    }
