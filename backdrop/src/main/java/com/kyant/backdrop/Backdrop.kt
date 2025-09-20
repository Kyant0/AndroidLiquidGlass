package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates

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
    val graphicsLayer: GraphicsLayer,
    internal val drawLayer: ContentDrawScope.() -> Unit
) {

    internal var backdropCoordinates: LayoutCoordinates? by mutableStateOf(null)

    private var layerScope: SimpleGraphicsLayerScope? = null

    internal fun DrawScope.drawBackdrop(
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val backdropCoordinates = backdropCoordinates ?: return
        val offset = backdropCoordinates.localPositionOf(coordinates)
        val layerBlock = layerBlock
        if (layerBlock != null) {
            withTransform({
                val layerScope = layerScope ?: SimpleGraphicsLayerScope().also { layerScope = it }
                with(layerScope) { inverseTransform(this@drawBackdrop, layerBlock) }
                translate(-offset.x, -offset.y)
            }) {
                drawLayer(graphicsLayer)
            }
        } else {
            translate(-offset.x, -offset.y) {
                drawLayer(graphicsLayer)
            }
        }
    }
}
