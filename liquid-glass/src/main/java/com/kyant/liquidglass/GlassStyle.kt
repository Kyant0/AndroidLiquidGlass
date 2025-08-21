package com.kyant.liquidglass

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import com.kyant.liquidglass.highlight.GlassHighlight
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.shadow.GlassShadow

/**
 * The style of the liquid glass.
 */
@Immutable
data class GlassStyle(
    val shape: CornerBasedShape,
    val innerRefraction: InnerRefraction = InnerRefraction.Default,
    val material: GlassMaterial = GlassMaterial.Default,
    val highlight: GlassHighlight = GlassHighlight.Default,
    val shadow: GlassShadow? = GlassShadow.Default
)
