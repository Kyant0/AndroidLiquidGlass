package com.kyant.backdrop.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

@Immutable
data class Shadow(
    val radius: Dp = 24f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius / 6f),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Stable
        val Default: Shadow = Shadow()
    }
}

@Stable
fun lerp(start: Shadow, stop: Shadow, fraction: Float): Shadow {
    return Shadow(
        radius = lerp(start.radius, stop.radius, fraction),
        offset = lerp(start.offset, stop.offset, fraction),
        color = lerp(start.color, stop.color, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction),
        blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode
    )
}
