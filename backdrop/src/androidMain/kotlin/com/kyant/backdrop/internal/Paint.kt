package com.kyant.backdrop.internal

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asAndroidRuntimeShader

internal actual fun Paint.blur(radius: Float) {
    this.asFrameworkPaint().maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal actual fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.asFrameworkPaint().shader = runtimeShader?.asAndroidRuntimeShader()
}
