package com.kyant.liquidglass.material

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter

@Deprecated(
    "Use simpleColorFilter instead. This function will be removed in a future release.",
    ReplaceWith("simpleColorFilter(saturation = saturation)")
)
fun saturationColorFilter(saturation: Float): ColorFilter {
    return ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
}

@Stable
fun simpleColorFilter(
    brightness: Float = 1f,
    contrast: Float = 1f,
    saturation: Float = 1f
): ColorFilter {
    val invSat = 1f - saturation
    val r = 0.213f * invSat
    val g = 0.715f * invSat
    val b = 0.072f * invSat

    val c = contrast
    val t = (0.5f - c * 0.5f + (brightness - 1f)) * 255f
    val s = saturation

    val cr = c * r
    val cg = c * g
    val cb = c * b
    val cs = c * s

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            cr + cs, cg, cb, 0f, t,
            cr, cg + cs, cb, 0f, t,
            cr, cg, cb + cs, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorMatrixColorFilter(colorMatrix)
}
