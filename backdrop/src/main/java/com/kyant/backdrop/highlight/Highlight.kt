package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Highlight(
    val width: Dp = (2f / 3f).dp,
    val color: Color = Color.White.copy(alpha = 0.38f),
    val blendMode: BlendMode = BlendMode.Plus,
    val blurRadius: Dp = width / 2f,
    val style: () -> HighlightStyle = DefaultHighlightStyle
)

private val DefaultHighlightStyle = { HighlightStyle.Default }
