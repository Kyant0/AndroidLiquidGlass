package com.kyant.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

@Deprecated("[Backdrop API] Use `rememberLayerBackdrop` instead")
@Composable
fun rememberLiquidGlassProviderState(
    backgroundColor: Color?
): LiquidGlassProviderState {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(backgroundColor, graphicsLayer) {
        LiquidGlassProviderState(
            backgroundColor = backgroundColor,
            graphicsLayer = graphicsLayer
        )
    }
}

@Deprecated("[Backdrop API] Use `LayerBackdrop` instead")
@Stable
class LiquidGlassProviderState internal constructor(
    val backgroundColor: Color?,
    internal val graphicsLayer: GraphicsLayer
) {

    internal val backdrop = LayerBackdrop(graphicsLayer, backgroundColor)

    internal var position: Offset
        get() = backdrop.backdropPosition
        set(value) {
            backdrop.backdropPosition = value
        }
}
