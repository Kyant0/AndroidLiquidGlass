package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

@Immutable
data class Highlight(
    val width: Dp = (2f / 3f).dp,
    val blurRadius: Dp = width / 2f,
    val color: Color = Color.White.copy(alpha = 0.38f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = BlendMode.Plus,
    val style: () -> HighlightStyle = DefaultHighlightStyle
) {

    companion object {

        @Stable
        val Default: Highlight = Highlight()

        @Stable
        val AmbientDefault: Highlight =
            Highlight(
                width = (1f / 3f).dp,
                blurRadius = (1f / 3f).dp,
                color = Color.Transparent.copy(0.5f),
                blendMode = BlendMode.SrcOver,
                style = { HighlightStyle.Dynamic.AmbientDefault }
            )

        @Stable
        val SolidDefault: Highlight = Highlight { HighlightStyle.Solid }
    }
}

@Stable
fun lerp(start: Highlight, stop: Highlight, fraction: Float): Highlight {
    return Highlight(
        width = lerp(start.width, stop.width, fraction),
        color = lerp(start.color, stop.color, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction),
        blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode,
        blurRadius = lerp(start.blurRadius, stop.blurRadius, fraction),
        style = { lerp(start.style(), stop.style(), fraction) }
    )
}

private val DefaultHighlightStyle = { HighlightStyle.Dynamic.Default }
