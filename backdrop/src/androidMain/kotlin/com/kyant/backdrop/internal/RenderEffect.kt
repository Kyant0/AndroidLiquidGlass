package com.kyant.backdrop.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asAndroidRuntimeShader

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun RenderEffect?.chain(other: RenderEffect): RenderEffect {
    return if (this != null) {
        android.graphics.RenderEffect.createChainEffect(
            other.asAndroidRenderEffect(),
            this.asAndroidRenderEffect()
        ).asComposeRenderEffect()
    } else {
        other
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun RuntimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String
): RenderEffect {
    return android.graphics.RenderEffect.createRuntimeShaderEffect(
        runtimeShader.asAndroidRuntimeShader(),
        uniformShaderName
    ).asComposeRenderEffect()
}

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun ColorFilterEffect(
    renderEffect: RenderEffect?,
    colorFilter: ColorFilter
): RenderEffect {
    return if (renderEffect != null) {
        android.graphics.RenderEffect.createColorFilterEffect(
            colorFilter.asAndroidColorFilter(),
            renderEffect.asAndroidRenderEffect()
        ).asComposeRenderEffect()
    } else {
        android.graphics.RenderEffect.createColorFilterEffect(
            colorFilter.asAndroidColorFilter(),
        ).asComposeRenderEffect()
    }
}
