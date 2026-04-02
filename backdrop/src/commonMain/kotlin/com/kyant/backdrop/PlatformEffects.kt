package com.kyant.backdrop

import androidx.compose.ui.graphics.RenderEffect

internal expect fun PlatformRenderEffect.toComposeRenderEffect(): RenderEffect

internal expect val isPlatformEffectsSupported: Boolean
