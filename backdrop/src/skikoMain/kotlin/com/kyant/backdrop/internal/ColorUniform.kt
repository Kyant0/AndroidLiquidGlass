package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.connect
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

// RuntimeShaderBuilder accepts float uniforms, so do not constrain the conversion
// to the [0, 1] range of ColorSpaces.Srgb (or pack it back into a Compose Color).
private val UniformSrgb = Rgb(
    name = "Backdrop shader sRGB",
    primaries = floatArrayOf(0.64f, 0.33f, 0.30f, 0.60f, 0.15f, 0.06f),
    whitePoint = ColorSpaces.Srgb.whitePoint,
    oetf = { value ->
        val magnitude = abs(value)
        sign(value) * if (magnitude <= 0.0031308) magnitude * 12.92
        else 1.055 * magnitude.pow(1.0 / 2.4) - 0.055
    },
    eotf = { value ->
        val magnitude = abs(value)
        sign(value) * if (magnitude <= 0.04045) magnitude / 12.92
        else ((magnitude + 0.055) / 1.055).pow(2.4)
    },
    min = -Float.MAX_VALUE,
    max = Float.MAX_VALUE
)

internal fun colorUniformComponents(color: Color): FloatArray {
    val components = floatArrayOf(color.red, color.green, color.blue)
    return if (color.colorSpace == ColorSpaces.Srgb) components
    else color.colorSpace.connect(UniformSrgb).transform(components)
}
