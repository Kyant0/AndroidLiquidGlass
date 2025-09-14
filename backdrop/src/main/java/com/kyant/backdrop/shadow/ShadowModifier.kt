package com.kyant.backdrop.shadow

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawOutline
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
import androidx.compose.ui.util.fastCoerceIn
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
    private val shapeDrawable = ShapeDrawable()

    val drawNode = delegate(CacheDrawModifierNode {
        val shadow = shadow()
        if (shadow == null || shadow.elevation.value <= 0f || shadow.color.isUnspecified) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }

        val size = size
        val outline = shapeProvider.shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }

        val elevation = shadow.elevation.toPx()
        val offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())

        shapeDrawable.apply {
            this.shape =
                PathShape(
                    path.asAndroidPath(),
                    size.width,
                    size.height
                )
            setBounds(0, 0, ceil(size.width).toInt(), ceil(size.height).toInt())
            paint.setShadowLayer(
                elevation,
                offset.x,
                offset.y,
                shadow.color.toArgb()
            )
            paint.alpha = (shadow.alpha.fastCoerceIn(0f, 1f) * 255f + 0.5f).toInt()
            paint.asComposePaint().blendMode = shadow.blendMode
        }

        graphicsLayer?.record(
            density = this,
            layoutDirection = layoutDirection,
            size = IntSize(
                ceil(size.width + elevation * 2).toInt(),
                ceil(size.height + elevation * 2).toInt()
            )
        ) {
            translate(elevation - offset.x, elevation - offset.y) {
                shapeDrawable.draw(drawContext.canvas.nativeCanvas)

                drawOutline(
                    outline = outline,
                    color = Color.Black,
                    blendMode = BlendMode.DstOut
                )
            }
        }

        onDrawWithContent {
            graphicsLayer?.let { layer ->
                translate(-(elevation - offset.x), -(elevation - offset.y)) {
                    drawLayer(layer)
                }
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
