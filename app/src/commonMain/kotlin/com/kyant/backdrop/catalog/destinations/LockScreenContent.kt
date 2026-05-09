package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.utils.rememberSdfShader
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.shapes.Rectangle
import glass.app.generated.resources.Res
import glass.app.generated.resources.clock_sdf

@Composable
fun LockScreenContent() {
    BackdropDemoScaffold { backdrop ->
        var offset by remember { mutableStateOf(Offset.Zero) }
        val sdfShader = rememberSdfShader(Res.drawable.clock_sdf)

        Column(Modifier.background(Color.Black.copy(alpha = 0.3f)).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.padding(horizontal = 48f.dp)
                        .graphicsLayer {
                            val offset = offset
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .draggable2D(rememberDraggable2DState { delta -> offset += delta })
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { Rectangle },
                            effects = {
                                colorControls(
                                    brightness = -0.1f,
                                    contrast = 0.75f,
                                    saturation = 1.5f,
                                )
                                blur(2f.dp.toPx())
                                with(sdfShader) { apply() }
                            },
                            onDrawBackdrop = { drawBackdrop ->
                                drawBackdrop()
                                drawRect(Color.White.copy(alpha = 0.25f))
                            },
                        )
                        .widthIn(max = 400f.dp)
                        .aspectRatio(sdfShader.width.toFloat() / sdfShader.height.toFloat())
                        .fillMaxWidth()
                )
            }
            Box(Modifier.weight(1f))
        }
    }
}
