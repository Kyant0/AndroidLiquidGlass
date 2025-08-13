package com.kyant.liquidglass.material

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The material properties of the liquid glass.
 *
 * @param blurRadius
 * The radius of the blur effect applied to the content behind the glass.
 *
 * @param colorFilter
 * The color filter applied to the content behind the glass.
 * The default is a saturation color filter with a factor of 1.5.
 *
 * @param brush
 * The brush applied to the glass material.
 * It can be used to create a tinted overlay to meet the contrast requirements.
 *
 * @param alpha
 * The alpha value of the brush.
 *
 * @param blendMode
 * The blend mode of the brush.
 */
@Immutable
data class GlassMaterial(
    val blurRadius: Dp = 2.dp,
    val colorFilter: ColorFilter? = DefaultColorFilter,
    val brush: Brush? = null,
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Stable
        val DefaultColorFilter: ColorFilter = simpleColorFilter(contrast = 1.5f, saturation = 1.5f)

        @Stable
        val Default: GlassMaterial = GlassMaterial()

        @Stable
        val None: GlassMaterial =
            GlassMaterial(
                blurRadius = 0.dp,
                colorFilter = null
            )
    }
}
