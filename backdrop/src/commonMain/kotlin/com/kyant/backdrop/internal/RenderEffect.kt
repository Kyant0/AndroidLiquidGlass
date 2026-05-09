package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import com.kyant.backdrop.RuntimeShader

internal expect fun RenderEffect?.chain(other: RenderEffect): RenderEffect

internal expect fun RuntimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String
): RenderEffect

internal expect fun ColorFilterEffect(
    renderEffect: RenderEffect? = null,
    colorFilter: ColorFilter
): RenderEffect
