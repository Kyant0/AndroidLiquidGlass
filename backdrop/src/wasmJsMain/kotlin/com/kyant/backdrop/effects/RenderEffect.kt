package com.kyant.backdrop.effects

import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.PlatformRenderEffect
import org.jetbrains.skia.ImageFilter

fun BackdropEffectScope.effect(effect: PlatformRenderEffect) {
    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            ImageFilter.makeCompose(effect, currentEffect)
        } else {
            effect
        }
}
