package com.kyant.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.LightingColorFilter
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.BackdropEffectScopeImpl
import com.kyant.backdrop.internal.ColorFilterEffect
import com.kyant.backdrop.internal.LightingShaderString
import com.kyant.backdrop.internal.TintShaderString
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported

/**
 * Applies [colorFilter]. The matrix filter stays on the platform, so how it treats
 * extended-range values follows the platform and OS release. The platform's
 * intermediate buffers and output surface must also support HDR. Tint supports the
 * Porter-Duff modes, Plus, Modulate, Multiply and Screen without packing
 * colors into ARGB8. Lighting is also unbounded; other blend modes retain
 * their platform semantics.
 */
fun BackdropEffectScope.colorFilter(colorFilter: ColorFilter) {
    if (!isRenderEffectSupported()) return

    if (isRuntimeShaderSupported() && colorFilter is BlendModeColorFilter) {
        val mode = tintMode(colorFilter.blendMode)
        if (mode != null) {
            runtimeShaderEffect(
                key = (this as BackdropEffectScopeImpl).nextTintEffectKey(),
                shaderString = TintShaderString, uniformShaderName = "content"
            ) {
                setColorUniform("color", colorFilter.color)
                setIntUniform("mode", mode)
            }
            return
        }
    }
    if (isRuntimeShaderSupported() && colorFilter is LightingColorFilter) {
        runtimeShaderEffect(
            key = (this as BackdropEffectScopeImpl).nextLightingEffectKey(),
            shaderString = LightingShaderString, uniformShaderName = "content"
        ) {
            setColorUniform("multiplyColor", colorFilter.multiply)
            setColorUniform("addColor", colorFilter.add)
        }
        return
    }

    // Color matrices and every other filter without a runtime-shader path keep their platform
    // semantics here. Android 12/12L has RenderEffect but no RuntimeShader.
    renderEffect = ColorFilterEffect(renderEffect, colorFilter)
}

fun BackdropEffectScope.opacity(@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
    val colorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, alpha, 0f
        )
    )
    colorFilter(ColorMatrixColorFilter(colorMatrix))
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

private val VibrantColorFilter = colorControlsColorFilter(saturation = 1.5f)

fun BackdropEffectScope.vibrancy() {
    colorFilter(VibrantColorFilter)
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
        floatArrayOf(
            cr + cs, cg, cb, 0f, t,
            cr, cg + cs, cb, 0f, t,
            cr, cg, cb + cs, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorMatrixColorFilter(colorMatrix)
}

private fun tintMode(mode: BlendMode): Int? = when (mode) {
    BlendMode.Clear -> 0
    BlendMode.Src -> 1
    BlendMode.Dst -> 2
    BlendMode.SrcOver -> 3
    BlendMode.DstOver -> 4
    BlendMode.SrcIn -> 5
    BlendMode.DstIn -> 6
    BlendMode.SrcOut -> 7
    BlendMode.DstOut -> 8
    BlendMode.SrcAtop -> 9
    BlendMode.DstAtop -> 10
    BlendMode.Xor -> 11
    BlendMode.Plus -> 12
    BlendMode.Modulate -> 13
    BlendMode.Multiply -> 14
    BlendMode.Screen -> 15
    else -> null
}
