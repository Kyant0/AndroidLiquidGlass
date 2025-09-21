package com.kyant.backdrop

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface BackdropDrawer {

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
