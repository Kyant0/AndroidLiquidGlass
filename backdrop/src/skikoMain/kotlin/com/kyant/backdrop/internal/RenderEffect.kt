package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaColorFilter
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asSkikoRuntimeShader
import org.jetbrains.skia.ImageFilter

internal actual fun RenderEffect?.chain(other: RenderEffect): RenderEffect {
    return if (this != null) {
        ImageFilter.makeCompose(
            other.asSkiaImageFilter(),
            this.asSkiaImageFilter()
        ).asComposeRenderEffect()
    } else {
        other
    }
}

internal actual fun RuntimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String
): RenderEffect {
    return ImageFilter.makeRuntimeShader(
        runtimeShader.asSkikoRuntimeShader(),
        uniformShaderName,
        null
    ).asComposeRenderEffect()
}

internal actual fun ColorFilterEffect(
    renderEffect: RenderEffect?,
    colorFilter: ColorFilter
): RenderEffect {
    return ImageFilter.makeColorFilter(
        colorFilter.asSkiaColorFilter(),
        renderEffect?.asSkiaImageFilter(),
        null
    ).asComposeRenderEffect()
}
