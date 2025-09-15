package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow

private val DefaultDrawLayer: ContentDrawScope.() -> Unit = { drawContent() }

@Composable
fun rememberBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    drawLayer: ContentDrawScope.() -> Unit = DefaultDrawLayer
): Backdrop {
    return remember(graphicsLayer, drawLayer) {
        Backdrop(
            graphicsLayer = graphicsLayer,
            drawLayer = drawLayer
        )
    }
}

@Stable
class Backdrop internal constructor(
    internal val graphicsLayer: GraphicsLayer,
    internal val drawLayer: ContentDrawScope.() -> Unit
) {

    internal var backdropPosition: Offset by mutableStateOf(Offset.Zero)

    internal fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow() - backdropPosition
        translate(-position.x, -position.y) {
            drawLayer(graphicsLayer)
        }
    }
}
