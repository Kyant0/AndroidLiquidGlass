package com.kyant.liquidglass.shadow

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.liquidglass.GlassStyle

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

internal class GlassShadowNode(
    var style: () -> GlassStyle
) : DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var _shadowPainter: DropShadowPainter? = null

    private var shape: CornerBasedShape = style().shape
        set(value) {
            if (field != value) {
                field = value
                _shadowPainter = null
                invalidateDraw()
            }
        }

    private var shadow: GlassShadow? = style().shadow
        set(value) {
            if (field != value) {
                field = value
                _shadowPainter = null
                invalidateDraw()
            }
        }

    override fun ContentDrawScope.draw() {
        val shadow = shadow
        if (shadow != null && _shadowPainter == null) {
            _shadowPainter =
                requireGraphicsContext().shadowContext.createDropShadowPainter(
                    shape = shape,
                    shadow = Shadow(
                        radius = shadow.elevation,
                        brush = shadow.brush,
                        spread = shadow.spread,
                        offset = shadow.offset,
                        alpha = shadow.alpha,
                        blendMode = shadow.blendMode
                    )
                )
        }

        val shadowPainter = _shadowPainter
        if (shadowPainter != null) {
            with(shadowPainter) { draw(size) }
        }

        drawContent()
    }

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
            val style = style()
            shape = style.shape
            shadow = style.shadow
        }
    }
}
