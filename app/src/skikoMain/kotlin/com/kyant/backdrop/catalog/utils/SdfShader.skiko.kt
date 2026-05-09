package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.asSkikoRuntimeShader
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported

actual fun SdfShader(imageBitmap: ImageBitmap): SdfShader {
    return SdfShaderImpl(imageBitmap)
}

@Immutable
private class SdfShaderImpl(val sdfBitmap: ImageBitmap) : SdfShader {

    private val sdfTexture = sdfBitmap.asSkiaBitmap().makeShader()

    override val width: Int
        get() = sdfBitmap.width

    override val height: Int
        get() = sdfBitmap.height

    override fun BackdropEffectScope.apply(
        refractionHeight: Float,
        lightAngle: Float,
    ) {
        if (isRuntimeShaderSupported()) {
            runtimeShaderEffect("SdfShader", SdfShaderString, "content") {
                asSkikoRuntimeShader().child("sdfTex", sdfTexture)
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("sdfTexSize", sdfBitmap.width.toFloat(), sdfBitmap.height.toFloat())
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("lightAngle", lightAngle)
            }
        }
    }
}
