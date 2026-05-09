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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

@Composable
fun MagnifierContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val backgroundColor =
        if (isLightTheme) Color(0xFFFFFFFF)
        else Color(0xFF121212)

    BackdropDemoScaffold { backdrop ->
        val contentBackdrop = rememberLayerBackdrop()
        val cursorBackdrop = rememberLayerBackdrop()
        var offset by remember { mutableStateOf(Offset.Zero) }

        BasicText(
            LoremIpsum,
            Modifier
                .layerBackdrop(contentBackdrop)
                .padding(24f.dp)
                .clip(RoundedRectangle(32f.dp))
                .background(backgroundColor.copy(alpha = 0.9f))
                .padding(24f.dp),
            style = TextStyle(contentColor, 16f.sp)
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
                .background(accentColor, Capsule())
                .size(4f.dp, 24f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val offset = offset
                    translationX = offset.x
                    translationY = offset.y - 80f.dp.toPx()
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, contentBackdrop, cursorBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(
                            8f.dp.toPx(),
                            24f.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    innerShadow = { InnerShadow(radius = 16f.dp) },
                    onDrawBackdrop = { drawBackdrop ->
                        withTransform(
                            {
                                scale(1.5f, 1.5f)
                                translate(top = with(this@drawBackdrop) { -80f.dp.toPx() })
                            },
                            drawBackdrop
                        )
                    }
                )
                .size(128f.dp, 96f.dp)
        )
    }
}
