package com.kyant.backdrop.effects

import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.GammaAdjustmentShaderString
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.ImageFilter
import kotlin.math.pow

fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            ImageFilter.makeColorFilter(colorFilter, currentEffect, null)
        } else {
            ImageFilter.makeColorFilter(colorFilter, null, null)
        }
}

fun BackdropEffectScope.opacity(alpha: Float) {
    val colorMatrix = ColorMatrix(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, alpha, 0f
    )
    colorFilter(ColorFilter.makeMatrix(colorMatrix))
}

fun BackdropEffectScope.colorControls(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
) {
    if (brightness == 0f && contrast == 1f && saturation == 1f) {
        return
    }

    colorFilter(colorControlsColorFilter(brightness, contrast, saturation))
}

fun BackdropEffectScope.vibrancy() {
    colorFilter(VibrantColorFilter)
}

private val VibrantColorFilter = colorControlsColorFilter(saturation = 1.5f)

fun BackdropEffectScope.exposureAdjustment(ev: Float) {
    val scale = 2f.pow(ev / 2.2f)
    val colorMatrix = ColorMatrix(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    colorFilter(ColorFilter.makeMatrix(colorMatrix))
}

fun BackdropEffectScope.gammaAdjustment(power: Float) {
    val builder = obtainRuntimeShaderBuilder("GammaAdjustment", GammaAdjustmentShaderString).apply {
        uniform("power", power)
    }
    effect(ImageFilter.makeRuntimeShader(builder, "content", null))
}

private fun colorControlsColorFilter(
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
        cr + cs, cg, cb, 0f, t,
        cr, cg + cs, cb, 0f, t,
        cr, cg, cb + cs, 0f, t,
        0f, 0f, 0f, 1f, 0f
    )
    return ColorFilter.makeMatrix(colorMatrix)
}
