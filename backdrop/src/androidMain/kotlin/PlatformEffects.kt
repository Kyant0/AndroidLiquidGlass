package com.kyant.backdrop

import android.os.Build
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

internal actual fun PlatformRenderEffect.toComposeRenderEffect(): RenderEffect {
    return this.asComposeRenderEffect()
}

internal actual val isPlatformEffectsSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
