package com.kyant.backdrop

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint

internal actual fun Paint.applyBlurMaskFilter(radius: Float) {
    asFrameworkPaint().maskFilter =
        if (radius > 0f) {
            BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
}
