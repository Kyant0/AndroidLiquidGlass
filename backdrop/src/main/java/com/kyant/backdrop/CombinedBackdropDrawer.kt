package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

@Composable
fun rememberCombinedBackdropDrawer(backdrop1: Backdrop, backdrop2: Backdrop): BackdropDrawer {
    return remember(backdrop1, backdrop2) {
        CombinedBackdropDrawer(backdrop1, backdrop2)
    }
}

@Composable
fun rememberCombinedBackdropDrawer(vararg backdrops: Backdrop): BackdropDrawer {
    return remember(*backdrops) {
        CombinedBackdropsDrawer(*backdrops)
    }
}

@Stable
private class CombinedBackdropDrawer(
    val backdrop1: Backdrop,
    val backdrop2: Backdrop
) : BackdropDrawer {

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        with(backdrop1) { drawBackdrop(density, coordinates, layerBlock) }
        with(backdrop2) { drawBackdrop(density, coordinates, layerBlock) }
    }
}

@Stable
private class CombinedBackdropsDrawer(
    vararg val backdrops: Backdrop
) : BackdropDrawer {

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        backdrops.forEach {
            with(it) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}
