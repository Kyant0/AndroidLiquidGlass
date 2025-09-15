package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Composable
fun rememberLayerBackdrop(backgroundColor: Color?): LayerBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer, backgroundColor) {
        LayerBackdrop(
            graphicsLayer = graphicsLayer,
            backgroundColor = backgroundColor
        )
    }
}

@Stable
class LayerBackdrop(
    val graphicsLayer: GraphicsLayer,
    val backgroundColor: Color?
) : Backdrop {

    override val element: Modifier.Element?
        get() = LayerBackdropElement(this)

    var backdropPosition: Offset by mutableStateOf(Offset.Zero)

    override fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow() - backdropPosition
        translate(-position.x, -position.y) {
            if (backgroundColor != null && backgroundColor.isSpecified) {
                drawContext.canvas.nativeCanvas.drawColor(backgroundColor.toArgb())
            }
            drawLayer(graphicsLayer)
        }
    }
}

private class LayerBackdropElement(
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

private class LayerBackdropNode(
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
            backdrop.backdropPosition = coordinates.positionInWindow()
        }
    }

    fun update(backdrop: LayerBackdrop) {
        this.backdrop = backdrop
    }
}
