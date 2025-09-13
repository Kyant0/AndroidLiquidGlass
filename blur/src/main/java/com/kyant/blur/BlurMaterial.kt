package com.kyant.blur

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.simpleColorFilter

@Immutable
data class BlurMaterial(
    val colorFilter: ColorFilter? = DefaultColorFilter,
    val brush: Brush? = null,
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Stable
        val DefaultColorFilter: ColorFilter = simpleColorFilter(saturation = 1.5f)

        @Stable
        val Default: BlurMaterial = BlurMaterial()

        @Stable
        val None: BlurMaterial = BlurMaterial(colorFilter = null)
    }
}
