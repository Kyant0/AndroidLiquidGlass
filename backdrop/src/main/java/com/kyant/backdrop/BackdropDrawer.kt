package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

@Composable
fun rememberBackdropDrawer(
    backdrop: BackdropDrawer,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop
): BackdropDrawer {
    return remember(backdrop, onDrawBackdrop) {
        object : BackdropDrawer {

            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates,
                layerBlock: (GraphicsLayerScope.() -> Unit)?
            ) {
                onDrawBackdrop {
                    with(backdrop) { drawBackdrop(density, coordinates, layerBlock) }
                }
            }
        }
    }
}

interface BackdropDrawer {

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
