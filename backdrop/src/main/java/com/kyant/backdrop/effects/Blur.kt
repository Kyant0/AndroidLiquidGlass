package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import com.kyant.backdrop.BackdropEffectScope

fun BackdropEffectScope.blur(
    blurRadius: Float,
    tileMode: TileMode = TileMode.Clamp
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (blurRadius <= 0f) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                currentEffect,
                tileMode.toAndroidTileMode()
            )
        } else {
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                tileMode.toAndroidTileMode()
            )
        }
}
