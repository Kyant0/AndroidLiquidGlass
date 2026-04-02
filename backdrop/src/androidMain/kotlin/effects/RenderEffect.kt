package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.isPlatformEffectsSupported

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isPlatformEffectsSupported) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createChainEffect(effect, currentEffect)
        } else {
            effect
        }
}

fun BackdropEffectScope.effect(effect: androidx.compose.ui.graphics.RenderEffect) {
    if (!isPlatformEffectsSupported) return

    effect(effect.asAndroidRenderEffect())
}
