package com.kyant.backdrop.shadow

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isUnspecified
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.ceil

fun Modifier.backdropShadow(
    shape: Shape,
    elevation: Dp = 24f.dp,
    color: Color = Color.Black.copy(alpha = 0.15f),
    offset: DpOffset = DpOffset(0f.dp, 4f.dp),
    alpha: Float = 1f,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
): Modifier =
    this then BackdropShadowElement {
        Shadow(
            shape = shape,
            elevation = elevation,
            color = color,
            offset = offset,
            alpha = alpha,
            blendMode = blendMode
        )
    }

fun Modifier.backdropShadow(
    shadow: () -> Shadow?
): Modifier =
    this then BackdropShadowElement(shadow)

private class BackdropShadowElement(
    val shadow: () -> Shadow?
) : ModifierNodeElement<BackdropShadowNode>() {

    override fun create(): BackdropShadowNode {
        return BackdropShadowNode(shadow)
    }

    override fun update(node: BackdropShadowNode) {
        node.update(shadow)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "backdropShadow"
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackdropShadowElement) return false

        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        return shadow.hashCode()
    }
}

private class BackdropShadowNode(
    var shadow: () -> Shadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private val shapeDrawable = ShapeDrawable()

    override fun ContentDrawScope.draw() {
        val shadow = shadow()

        if (shadow == null || shadow.elevation.value <= 0f || shadow.color.isUnspecified) {
            return drawContent()
        }

        val size = size
        val outline = shadow.shape.createOutline(size, layoutDirection, this)
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

        val graphicsLayer = graphicsLayer
        if (graphicsLayer != null) {
            graphicsLayer.record(
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

            translate(-(elevation - offset.x), -(elevation - offset.y)) {
                drawLayer(graphicsLayer)
            }
        }

        drawContent()
    }

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
            this.graphicsLayer = null
        }
    }

    fun update(
        shadow: () -> Shadow?
    ) {
        if (this.shadow != shadow) {
            this.shadow = shadow
            invalidateDraw()
        }
    }
}
