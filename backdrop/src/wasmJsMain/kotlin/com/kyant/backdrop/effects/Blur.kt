package com.kyant.backdrop.effects

import androidx.compose.ui.graphics.TileMode
import com.kyant.backdrop.BackdropEffectScope
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter

fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode = TileMode.Clamp
) {
    if (radius <= 0f) return

    if (edgeTreatment != TileMode.Clamp || renderEffect != null) {
        if (radius > padding) {
            padding = radius
        }
    }

    val tileMode = when (edgeTreatment) {
        TileMode.Clamp -> FilterTileMode.CLAMP
        TileMode.Repeated -> FilterTileMode.REPEAT
        TileMode.Mirror -> FilterTileMode.MIRROR
        TileMode.Decal -> FilterTileMode.DECAL
        else -> FilterTileMode.CLAMP
    }

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            ImageFilter.makeBlur(radius, radius, tileMode, currentEffect)
        } else {
            ImageFilter.makeBlur(radius, radius, tileMode)
        }
}
