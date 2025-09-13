package com.kyant.liquidglass.material

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.liquidglass.GlassStyle

internal class SimpleGlassBrushElement(
    val style: GlassStyle
) : ModifierNodeElement<SimpleGlassBrushNode>() {

    override fun create(): SimpleGlassBrushNode {
        return SimpleGlassBrushNode(style)
    }

    override fun update(node: SimpleGlassBrushNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassBrush"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleGlassBrushElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class GlassBrushElement(
    val style: () -> GlassStyle
) : ModifierNodeElement<GlassBrushNode>() {

    override fun create(): GlassBrushNode {
        return GlassBrushNode(style)
    }

    override fun update(node: GlassBrushNode) {
        node.update(style)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassBrush"
        properties["style"] = style
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassBrushElement) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }
}

internal class SimpleGlassBrushNode(
    var style: GlassStyle
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var material: GlassMaterial = style.material
        set(value) {
            if (field.brush != value.brush ||
                field.alpha != value.alpha ||
                field.blendMode != value.blendMode
            ) {
                field = value
                invalidateDraw()
            }
        }

    override fun ContentDrawScope.draw() {
        material.brush?.let { brush ->
            drawRect(
                brush = brush,
                alpha = material.alpha,
                blendMode = material.blendMode
            )
        }

        drawContent()
    }

    fun update(
        style: GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            material = style.material
        }
    }
}

internal class GlassBrushNode(
    var style: () -> GlassStyle
) : DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var material: GlassMaterial = style().material
        set(value) {
            if (field.brush != value.brush ||
                field.alpha != value.alpha ||
                field.blendMode != value.blendMode
            ) {
                field = value
                invalidateDraw()
            }
        }

    override fun ContentDrawScope.draw() {
        material.brush?.let { brush ->
            drawRect(
                brush = brush,
                alpha = material.alpha,
                blendMode = material.blendMode
            )
        }

        drawContent()
    }

    override fun onObservedReadsChanged() {
        updateBrush()
    }

    override fun onAttach() {
        updateBrush()
    }

    fun update(
        style: () -> GlassStyle
    ) {
        if (this.style != style) {
            this.style = style
            updateBrush()
        }
    }

    private fun updateBrush() {
        observeReads { material = style().material }
    }
}
