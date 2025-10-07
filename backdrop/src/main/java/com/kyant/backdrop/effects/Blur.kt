package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import com.kyant.backdrop.BackdropEffectScope

fun BackdropEffectScope.blur(@FloatRange(from = 0.0) blurRadius: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (blurRadius <= 0f) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                currentEffect,
                Shader.TileMode.CLAMP
            )
        } else {
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.CLAMP
            )
        }
}
