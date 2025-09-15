package com.kyant.backdrop.shadow

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
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
        node.drawNode.invalidateDrawCache()
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
) : DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private val shadowPaint = Paint()
    private val maskPaint =
        Paint().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = android.graphics.BlendMode.DST_OUT
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
        }

    val drawNode = delegate(CacheDrawModifierNode {
        val shadow = shadow()
        val graphicsLayer = graphicsLayer
        if (shadow == null || graphicsLayer == null ||
            shadow.elevation.value <= 0f || shadow.color.isUnspecified
        ) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }

        val size = size
        val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)
        val elevation = shadow.elevation.toPx()
        val offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())

        shadowPaint.setShadowLayer(elevation, offset.x, offset.y, shadow.color.toArgb())
        shadowPaint.asComposePaint().blendMode = shadow.blendMode

        graphicsLayer.record(
            density = this,
            layoutDirection = layoutDirection,
            size = IntSize(
                ceil(size.width + elevation * 2).toInt(),
                ceil(size.height + elevation * 2).toInt()
            )
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            canvas.translate(elevation - offset.x, elevation - offset.y)

            when (outline) {
                is Outline.Rectangle -> {
                    val rect = RectF(0f, 0f, size.width, size.height)
                    canvas.drawRect(rect, shadowPaint)
                    canvas.drawRect(rect, maskPaint)
                }

                is Outline.Rounded -> {
                    val path = Path().apply { addRoundRect(outline.roundRect) }.asAndroidPath()
                    canvas.drawPath(path, shadowPaint)
                    canvas.drawPath(path, maskPaint)
                }

                is Outline.Generic -> {
                    val path = outline.path.asAndroidPath()
                    canvas.drawPath(path, shadowPaint)
                    canvas.drawPath(path, maskPaint)
                }
            }
        }

        onDrawWithContent {
            translate(-(elevation - offset.x), -(elevation - offset.y)) {
                drawLayer(graphicsLayer)
            }
            drawContent()
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
}
