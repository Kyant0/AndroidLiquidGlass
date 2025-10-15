package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import com.kyant.backdrop.BackdropEffectScope

fun BackdropEffectScope.blur(@FloatRange(from = 0.0) radius: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (radius <= 0f) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createBlurEffect(
                radius,
                radius,
                currentEffect,
                Shader.TileMode.CLAMP
            )
        } else {
            RenderEffect.createBlurEffect(
                radius,
                radius,
                Shader.TileMode.CLAMP
            )
        }
}
