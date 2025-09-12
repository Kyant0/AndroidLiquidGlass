package com.kyant.liquidglass.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The highlight effect applied to the liquid glass.
 *
 * @param width
 * The width of the highlight.
 *
 * @param color
 * The color of the highlight.
 *
 * @param blendMode
 * The blend mode of the highlight.
 *
 * @param style
 * The style of the highlight.
 */
@Immutable
data class GlassHighlight(
    val width: Dp = 1f.dp,
    val color: Color = Color.White.copy(alpha = 0.3f),
    val blendMode: BlendMode = BlendMode.Plus,
    val style: GlassHighlightStyle = GlassHighlightStyle.Default
) {

    companion object {

        @Stable
        val Default: GlassHighlight = GlassHighlight()
    }
}
