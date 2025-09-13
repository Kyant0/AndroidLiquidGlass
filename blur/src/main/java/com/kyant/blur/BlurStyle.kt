package com.kyant.blur

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp

@Immutable
data class BlurStyle(
    val shape: Shape,
    val blurRadius: Dp,
    val tileMode: TileMode = TileMode.Clamp,
    val material: BlurMaterial = BlurMaterial.Default
)
