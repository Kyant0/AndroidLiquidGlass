package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

@Immutable
data class Highlight(
    val width: Dp = 0.5f.dp,
    val blurRadius: Dp = width / 2f,
    val alpha: Float = 1f,
    val style: HighlightStyle = HighlightStyle.Default
) {

    companion object {

        @Stable
        val Default: Highlight = Highlight()

        @Stable
        val Ambient: Highlight = Highlight(style = HighlightStyle.Ambient)

        @Stable
        val Plain: Highlight = Highlight(style = HighlightStyle.Plain)
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
