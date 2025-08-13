package com.kyant.liquidglass.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Deprecated(
    "Use GlassShadow with brush instead. This function will be removed in a future release.",
    ReplaceWith(
        "GlassShadow(elevation, brush = SolidColor(color))",
        "androidx.compose.ui.graphics.SolidColor"
    )
)
@Stable
fun GlassShadow(
    elevation: Dp = 24.dp,
    color: Color = Color.Black.copy(alpha = 0.15f)
): GlassShadow =
    GlassShadow(
        elevation = elevation,
        brush = SolidColor(color)
    )

/**
 * The shadow effect applied to the liquid glass.
 */
@Immutable
data class GlassShadow(
    val elevation: Dp = 24.dp,
    val brush: Brush = SolidColor(Color.Black.copy(alpha = 0.15f)),
    val spread: Dp = 0.dp,
    val offset: DpOffset = DpOffset(0.dp, 4.dp),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Deprecated("Use null instead", ReplaceWith("null"))
        val None: GlassShadow? = null

        val Default: GlassShadow = GlassShadow()
    }
}
