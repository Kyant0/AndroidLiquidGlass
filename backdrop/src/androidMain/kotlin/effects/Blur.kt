package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.isPlatformEffectsSupported

fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode = TileMode.Clamp
) {
    if (!isPlatformEffectsSupported) return
    if (radius <= 0f) return

    if (edgeTreatment != TileMode.Clamp || renderEffect != null) {
        if (radius > padding) {
            padding = radius
        }
    }

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createBlurEffect(
                radius,
                radius,
                currentEffect,
                edgeTreatment.toAndroidTileMode()
            )
        } else {
            RenderEffect.createBlurEffect(
                radius,
                radius,
                edgeTreatment.toAndroidTileMode()
            )
        }
}
