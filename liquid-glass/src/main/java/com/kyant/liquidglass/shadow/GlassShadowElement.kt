package com.kyant.liquidglass.shadow

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import com.kyant.liquidglass.GlassStyle
import kotlin.math.ceil

internal class SimpleGlassShadowElement(
    val style: GlassStyle
) : ModifierNodeElement<SimpleGlassShadowNode>() {

    override fun create(): SimpleGlassShadowNode {
        return SimpleGlassShadowNode(style)
    }

    override fun update(node: SimpleGlassShadowNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassShadow"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassShadowElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class GlassShadowElement(
    val style: () -> GlassStyle
) : ModifierNodeElement<GlassShadowNode>() {

    override fun create(): GlassShadowNode {
        return GlassShadowNode(style)
    }

    override fun update(node: GlassShadowNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassShadow"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassShadowElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class SimpleGlassShadowNode(
    var style: GlassStyle
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null

    private var shadow = style.shadow
        set(value) {
            if (field != value) {
                field = value
                invalidateDraw()
            }
        }

    private var shape = style.shape
        set(value) {
            if (field != value) {
                field = value
                invalidateDraw()
            }
        }

    override fun ContentDrawScope.draw() {
        val shadow = shadow
        val elevation = shadow?.elevation?.toPx() ?: 0f
        val offsetX = shadow?.offset?.x?.toPx() ?: 0f
        val offsetY = shadow?.offset?.y?.toPx() ?: 0f

        if (shadow != null && elevation > 0f) {
            val size = size
            val outline = shape.createOutline(size, layoutDirection, this)

            val shapeDrawable = ShapeDrawable().apply {
                this.shape =
                    PathShape(
                        Path().apply { addOutline(outline) }.asAndroidPath(),
                        size.width,
                        size.height
                    )
                setBounds(0, 0, ceil(size.width).toInt(), ceil(size.height).toInt())
                paint.setShadowLayer(
                    elevation,
                    offsetX,
                    offsetY,
                    shadow.color.toArgb()
                )
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
                translate(elevation - offsetX, elevation - offsetY) {
                    shapeDrawable.draw(drawContext.canvas.nativeCanvas)

                    drawOutline(
                        outline = outline,
                        color = Color.Black,
                        blendMode = BlendMode.DstOut
                    )
                }
            }
        }

        graphicsLayer?.let { layer ->
            translate(-(elevation - offsetX), -(elevation - offsetY)) {
                drawLayer(layer)
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
        style: GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            this.shadow = style.shadow
            this.shape = style.shape
        }
    }
}

internal class GlassShadowNode(
    var style: () -> GlassStyle
) : ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private val shadowNode = delegate(
        SimpleGlassShadowNode(style())
    )

    override fun onObservedReadsChanged() {
        updateShadow()
    }

    override fun onAttach() {
        updateShadow()
    }

    fun update(
        style: () -> GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            updateShadow()
        }
    }

    private fun updateShadow() {
        observeReads {
            shadowNode.update(style())
        }
    }
}
