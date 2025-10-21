package com.kyant.backdrop

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize

context(node: DelegatableNode)
internal fun DrawScope.recordLayer(
    layer: GraphicsLayer,
    size: IntSize = this.size.toIntSize(),
    block: DrawScope.() -> Unit
) {
    layer.record(
        density = node.requireDensity(),
        layoutDirection = layoutDirection,
        size = size
    ) {
        this@recordLayer.draw(
            density = drawContext.density,
            layoutDirection = drawContext.layoutDirection,
            canvas = drawContext.canvas,
            size = drawContext.size,
            graphicsLayer = drawContext.graphicsLayer,
            block = block
        )
    }
}
