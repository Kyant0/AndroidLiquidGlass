package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.BackdropDrawScope

@RequiresApi(Build.VERSION_CODES.S)
fun BackdropDrawScope.blur(
    blurRadius: Dp,
    tileMode: TileMode = TileMode.Clamp
) {
    blur(
        blurRadius = blurRadius.toPx(),
        tileMode = tileMode
    )
}

@RequiresApi(Build.VERSION_CODES.S)
fun BackdropDrawScope.blur(
    blurRadius: Float,
    tileMode: TileMode = TileMode.Clamp
) {
    if (blurRadius <= 0f) {
        return
    }

    val renderEffect = renderEffect
    this.renderEffect =
        if (renderEffect != null) {
            RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                renderEffect,
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
