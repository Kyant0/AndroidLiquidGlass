package com.kyant.backdrop.highlight

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropDrawScope
import com.kyant.backdrop.BackdropSurfaceDrawScope
import com.kyant.backdrop.NoOpBackdropSurfaceDrawScope
import kotlin.math.ceil

fun BackdropDrawScope.onDrawSurfaceWithHighlight(
    width: Float = 1f.dp.toPx(),
    color: Color = Color.White.copy(alpha = 0.3f),
    blendMode: BlendMode = BlendMode.Plus,
    style: HighlightStyle = HighlightStyle.Default,
    block: DrawScope.() -> Unit
) {
    val highlight =
        highlight(
            width = width,
            color = color,
            blendMode = blendMode,
            style = style
        )

    onDrawSurface {
        block()
        with(highlight) { draw() }
    }
}

fun BackdropDrawScope.drawHighlight(
    width: Float = 1f.dp.toPx(),
    color: Color = Color.White.copy(alpha = 0.3f),
    blendMode: BlendMode = BlendMode.Plus,
    style: HighlightStyle = HighlightStyle.Default
) {
    val highlight =
        highlight(
            width = width,
            color = color,
            blendMode = blendMode,
            style = style
        )

    onDrawSurface { with(highlight) { draw() } }
}

fun BackdropDrawScope.highlight(
    width: Float = 1f.dp.toPx(),
    color: Color = Color.White.copy(alpha = 0.3f),
    blendMode: BlendMode = BlendMode.Plus,
    style: HighlightStyle = HighlightStyle.Default
): BackdropSurfaceDrawScope {
    val shape = shape
    if (shape == null || width <= 0f || color.isUnspecified) {
        return NoOpBackdropSurfaceDrawScope
    }

    val graphicsLayer = obtainGraphicsLayer("Highlight").apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }

    val size = size
    val strokeWidth = ceil(width.fastCoerceAtMost(size.minDimension / 2f))
    val borderSize = Size(size.width - strokeWidth, size.height - strokeWidth)
    val outline = shape.createOutline(borderSize, layoutDirection, this)

    val borderRenderEffect = style.createRenderEffect(size, strokeWidth)
    graphicsLayer.renderEffect = borderRenderEffect?.asComposeRenderEffect()
    graphicsLayer.blendMode = blendMode

    graphicsLayer.record(this, layoutDirection, size.toIntSize()) {
        draw(
            this@highlight,
            layoutDirection,
            drawContext.canvas,
            drawContext.size,
            drawContext.graphicsLayer
        ) {
            translate(strokeWidth / 2, strokeWidth / 2) {
                drawOutline(
                    outline = outline,
                    brush = SolidColor(color),
                    style = Stroke(strokeWidth)
                )
            }
        }
    }

    return BackdropSurfaceDrawScope {
        drawLayer(graphicsLayer)
    }
}
