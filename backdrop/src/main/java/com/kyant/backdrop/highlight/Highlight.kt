package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

@Immutable
data class Highlight(
    val width: Dp = 0.5f.dp,
    val blurRadius: Dp = width / 2f,
    val alpha: Float = 1f,
    val style: HighlightStyle = HighlightStyle.Dynamic.Default
) {

    @Deprecated(message = "Use the non-lambda version of style parameter")
    constructor(
        width: Dp = 0.5f.dp,
        blurRadius: Dp = width / 2f,
        color: Color = Color.White.copy(alpha = 0.5f),
        alpha: Float = 1f,
        blendMode: BlendMode = BlendMode.Plus,
        style: () -> HighlightStyle
    ) : this(
        width = width,
        blurRadius = blurRadius,
        alpha = alpha,
        style = style()
    )

    companion object {

        @Stable
        val Default: Highlight = Highlight()

        @Stable
        val AmbientDefault: Highlight = Highlight(style = HighlightStyle.Dynamic(isAmbient = true))

        @Stable
        val SolidDefault: Highlight = Highlight(style = HighlightStyle.Solid)
    }
}

@Stable
fun lerp(start: Highlight, stop: Highlight, fraction: Float): Highlight {
    return Highlight(
        width = lerp(start.width, stop.width, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction),
        blurRadius = lerp(start.blurRadius, stop.blurRadius, fraction),
        style = lerp(start.style, stop.style, fraction)
    )
}
