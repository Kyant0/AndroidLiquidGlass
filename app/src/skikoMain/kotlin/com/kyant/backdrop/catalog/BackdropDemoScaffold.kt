package com.kyant.backdrop.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import glass.app.generated.resources.Res
import glass.app.generated.resources.wallpaper_light
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun BackdropDemoScaffold(
    modifier: Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var painter: Painter? by remember { mutableStateOf(null) }

        val backdrop = rememberLayerBackdrop()

        Image(
            painter ?: painterResource(Res.drawable.wallpaper_light),
            null,
            Modifier
                .layerBackdrop(backdrop)
                .then(modifier)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        content(backdrop)
    }
}
