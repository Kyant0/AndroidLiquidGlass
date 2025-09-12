package com.kyant.liquidglass

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

fun Modifier.backdrop(backdrop: Backdrop): Modifier {
    val element = backdrop.element
    return if (element != null) {
        this then element
    } else {
        this
    }
}

internal class LayerBackdropElement(
    val backdrop: LayerBackdrop
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop = backdrop)
    }

    override fun update(node: LayerBackdropNode) {
        node.update(backdrop = backdrop)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

internal class LayerBackdropNode(
    var backdrop: LayerBackdrop
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    override fun ContentDrawScope.draw() {
        drawContent()

        backdrop.graphicsLayer.record {
            backdrop.backgroundColor?.let { drawRect(it) }
            this@draw.drawContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.backdropPosition = coordinates.positionOnScreen()
        }
    }

    fun update(backdrop: LayerBackdrop) {
        this.backdrop = backdrop
    }
}
