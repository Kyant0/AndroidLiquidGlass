package com.kyant.backdrop

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo

fun Modifier.backdrop(backdrop: Backdrop): Modifier =
    this then BackdropElement(backdrop)

private class BackdropElement(val backdrop: Backdrop) : ModifierNodeElement<BackdropNode>() {

    override fun create(): BackdropNode {
        return BackdropNode(backdrop = backdrop)
    }

    override fun update(node: BackdropNode) {
        node.backdrop = backdrop
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackdropElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class BackdropNode(var backdrop: Backdrop) :
    DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    override fun ContentDrawScope.draw() {
        drawContent()
        backdrop.graphicsLayer.record { backdrop.drawLayer.invoke(this@draw) }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.backdropPosition = coordinates.positionInWindow()
        }
    }
}
