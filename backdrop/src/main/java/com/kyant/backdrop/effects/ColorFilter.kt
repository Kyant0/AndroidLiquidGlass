package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import com.kyant.backdrop.BackdropEffectScope

fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val filter = colorFilter.asAndroidColorFilter()
    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createColorFilterEffect(filter, currentEffect)
        } else {
            RenderEffect.createColorFilterEffect(filter)
        }
}

fun BackdropEffectScope.colorFilter(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
) {
    if (brightness == 0f && contrast == 1f && saturation == 1f) {
        return
    }

    val colorFilter = simpleColorFilter(brightness, contrast, saturation)
    colorFilter(colorFilter)
}

fun BackdropEffectScope.saturation() {
    colorFilter(DefaultSaturationColorFilter)
}

private val DefaultSaturationColorFilter = simpleColorFilter(saturation = 1.5f)

private fun simpleColorFilter(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
): ColorFilter {
    val invSat = 1f - saturation
    val r = 0.213f * invSat
    val g = 0.715f * invSat
    val b = 0.072f * invSat

    val c = contrast
    val t = (0.5f - c * 0.5f + brightness) * 255f
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
