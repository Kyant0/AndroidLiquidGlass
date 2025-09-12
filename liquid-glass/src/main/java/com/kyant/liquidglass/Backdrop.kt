package com.kyant.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen

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

interface Backdrop {

    val element: Modifier.Element?

    fun DrawScope.drawChild(coordinates: LayoutCoordinates)
}

@Stable
class LayerBackdrop(
    val graphicsLayer: GraphicsLayer,
    val backgroundColor: Color?
) : Backdrop {

    override val element: Modifier.Element?
        get() = LayerBackdropElement(this)

    var backdropPosition: Offset by mutableStateOf(Offset.Zero)

    override fun DrawScope.drawChild(coordinates: LayoutCoordinates) {
        val position = coordinates.positionOnScreen() - backdropPosition
        translate(-position.x, -position.y) {
            if (backgroundColor != null && backgroundColor.isSpecified) {
                drawContext.canvas.nativeCanvas.drawColor(backgroundColor.toArgb())
            }
            drawLayer(graphicsLayer)
        }
    }
}
