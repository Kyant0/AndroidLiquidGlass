package com.kyant.backdrop

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

@Immutable
object EmptyBackdropDrawer : BackdropDrawer {

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
    }
}
