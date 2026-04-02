package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
