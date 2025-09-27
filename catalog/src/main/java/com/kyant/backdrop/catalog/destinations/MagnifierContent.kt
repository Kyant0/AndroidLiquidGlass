package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.utils.LoremIpsum
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.refractionWithDispersion
import com.kyant.capsule.ContinuousCapsule

@Composable
fun MagnifierContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)

    BackdropDemoScaffold { backdrop ->
        val contentBackdrop = rememberLayerBackdrop()
        val cursorBackdrop = rememberLayerBackdrop()
        var offset by remember { mutableStateOf(Offset.Zero) }

        BasicText(
            LoremIpsum,
            Modifier
                .layerBackdrop(contentBackdrop)
                .padding(48f.dp),
            style = TextStyle(contentColor, 15f.sp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val offset = offset
                    translationX = offset.x
                    translationY = offset.y
                }
                .draggable2D(rememberDraggable2DState { delta -> offset += delta })
                .layerBackdrop(cursorBackdrop)
                .background(accentColor, ContinuousCapsule)
                .size(4f.dp, 24f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val offset = offset
                    translationX = offset.x
                    translationY = offset.y - size.height
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, contentBackdrop, cursorBackdrop),
                    shape = { ContinuousCapsule },
                    effects = {
                        refractionWithDispersion(
                            height = 12f.dp.toPx(),
                            amount = 12f.dp.toPx(),
                            hasDepthEffect = true,
                            dispersionIntensity = 1.5f
                        )
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        scale(1.5f, 1.5f) {
                            translate(top = -size.height) {
                                drawBackdrop()
                            }
                        }
                    }
                )
                .size(128f.dp, 80f.dp)
        )
    }
}
