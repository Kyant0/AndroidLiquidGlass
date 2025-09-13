package com.kyant.blur

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Highlight(
    val shape: Shape,
    val width: Dp = 1f.dp,
    val color: Color = Color.White.copy(alpha = 0.3f),
    val blendMode: BlendMode = BlendMode.Plus,
    val style: HighlightStyle = HighlightStyle.Default
)
