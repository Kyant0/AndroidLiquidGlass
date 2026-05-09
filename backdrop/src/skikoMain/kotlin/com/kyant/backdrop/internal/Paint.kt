package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Paint
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asSkikoRuntimeShader
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal actual fun Paint.blur(radius: Float) {
    this.asFrameworkPaint().maskFilter =
        if (radius > 0f) MaskFilter.makeBlur(FilterBlurMode.NORMAL, radius)
        else null
}

internal actual fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.asFrameworkPaint().shader = runtimeShader?.asSkikoRuntimeShader()?.makeShader()
}
