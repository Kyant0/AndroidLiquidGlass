package com.kyant.backdrop

import androidx.compose.ui.graphics.Paint
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal actual fun Paint.applyBlurMaskFilter(radius: Float) {
    asFrameworkPaint().maskFilter =
        if (radius > 0f) {
            MaskFilter.makeBlur(FilterBlurMode.NORMAL, radius / 2f)
        } else {
            null
        }
}
