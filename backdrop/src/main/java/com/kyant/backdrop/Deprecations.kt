package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private val DefaultDrawLayer: ContentDrawScope.() -> Unit = { drawContent() }

@Deprecated(
    message = "Use rememberLayerBackdrop instead",
    replaceWith = ReplaceWith(
        "rememberLayerBackdrop(graphicsLayer = graphicsLayer)",
        "com.kyant.backdrop.backdrops.rememberLayerBackdrop",
        "androidx.compose.ui.graphics.rememberGraphicsLayer"
    )
)
@Composable
fun rememberBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    drawLayer: ContentDrawScope.() -> Unit = DefaultDrawLayer
): LayerBackdrop {
    return rememberLayerBackdrop(graphicsLayer, drawLayer)
}

@Deprecated(
    message = "Use layerBackdrop instead",
    replaceWith = ReplaceWith(
        "this.layerBackdrop(backdrop)",
        "com.kyant.backdrop.backdrops.layerBackdrop"
    )
)
fun Modifier.backdrop(backdrop: LayerBackdrop): Modifier =
    this.layerBackdrop(backdrop)
