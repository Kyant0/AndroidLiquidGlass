package com.kyant.liquidglass.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * The shadow effect applied to the liquid glass.
 */
@Immutable
data class GlassShadow(
    val elevation: Dp = 24f.dp,
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val offset: DpOffset = DpOffset(0f.dp, 4f.dp),
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        val Default: GlassShadow = GlassShadow()
    }
}
