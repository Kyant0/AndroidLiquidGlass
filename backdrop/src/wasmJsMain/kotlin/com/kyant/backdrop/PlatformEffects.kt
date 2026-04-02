package com.kyant.backdrop

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

internal actual fun PlatformRenderEffect.toComposeRenderEffect(): RenderEffect {
    return this.asComposeRenderEffect()
}

internal actual val isPlatformEffectsSupported: Boolean = true
