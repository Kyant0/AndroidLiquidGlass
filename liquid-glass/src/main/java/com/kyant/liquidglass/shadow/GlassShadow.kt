package com.kyant.liquidglass.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shadow effect applied to the liquid glass.
 *
 * @param elevation
 * The elevation of the shadow.
 *
 * @param color
 * The color of the shadow.
 */
@Immutable
data class GlassShadow(
    val elevation: Dp = 16.dp,
    val color: Color = Color.Black.copy(alpha = 0.25f)
) {

    companion object {

        val None: GlassShadow = GlassShadow(
            elevation = 0.dp,
            color = Color.Transparent
        )

        val Default: GlassShadow = GlassShadow()
    }
}
