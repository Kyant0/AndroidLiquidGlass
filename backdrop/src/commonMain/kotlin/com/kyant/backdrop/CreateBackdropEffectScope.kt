package com.kyant.backdrop

import androidx.compose.ui.graphics.Shape

internal expect fun createBackdropEffectScope(shapeProvider: () -> Shape): BackdropEffectScopeImpl
