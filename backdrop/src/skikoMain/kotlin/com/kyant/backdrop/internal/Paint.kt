package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asSkikoRuntimeShader
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal actual fun Paint.blur(radius: Float) {
    this.skiaPaint.maskFilter =
        if (radius > 0f) MaskFilter.makeBlur(FilterBlurMode.NORMAL, radius)
        else null
}

internal actual fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.skiaPaint.shader = runtimeShader?.asSkikoRuntimeShader()?.makeShader()
}
