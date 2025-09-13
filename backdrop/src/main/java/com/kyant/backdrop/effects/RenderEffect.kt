package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import com.kyant.backdrop.BackdropDrawScope

@RequiresApi(Build.VERSION_CODES.S)
fun BackdropDrawScope.effect(effect: RenderEffect) {
    val renderEffect = renderEffect
    this.renderEffect =
        if (renderEffect != null) {
            RenderEffect.createChainEffect(
                effect,
                renderEffect
            )
        } else {
            effect
        }
}
