package com.kyant.liquidglass.highlight

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
interface GlassHighlightStyle {

    /**
     * Solid highlight effect.
     */
    @Immutable
    data object Solid : GlassHighlightStyle

    /**
     * Soft highlight effect with blur.
     */
    @Immutable
    data object Soft : GlassHighlightStyle

    /**
     * Dynamic highlight effect with shimmering effect.
     *
     * @param angle
     * The angle of the highlight in degrees.
     *
     * @param falloff
     * The falloff of the highlight. Higher value results in a sharper highlight.
     */
    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : GlassHighlightStyle

    companion object {

        @Stable
        val Default: Dynamic = Dynamic()
    }
}
