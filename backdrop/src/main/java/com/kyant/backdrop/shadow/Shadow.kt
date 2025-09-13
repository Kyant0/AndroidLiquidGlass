package com.kyant.backdrop.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Immutable
data class Shadow(
    val shape: Shape,
    val elevation: Dp = 24f.dp,
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val offset: DpOffset = DpOffset(0f.dp, 4f.dp),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
)
