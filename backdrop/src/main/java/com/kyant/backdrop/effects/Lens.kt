package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.RoundedRectDispersionShaderString
import com.kyant.backdrop.RoundedRectRefractionShaderString

fun BackdropEffectScope.refraction(
    height: Float,
    amount: Float = height,
    hasDepthEffect: Boolean = false
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader = obtainRuntimeShader("Refraction", RoundedRectRefractionShaderString).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", height)
                setFloatUniform("refractionAmount", -amount)
                setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
            }
            RenderEffect.createRuntimeShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

fun BackdropEffectScope.dispersion(
    height: Float,
    amount: Float = height
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader = obtainRuntimeShader("Dispersion", RoundedRectDispersionShaderString).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("dispersionHeight", height)
                setFloatUniform("dispersionAmount", amount)
            }
            RenderEffect.createRuntimeShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

fun BackdropEffectScope.refractionWithDispersion(
    height: Float,
    amount: Float = height,
    hasDepthEffect: Boolean = false,
    dispersionIntensity: Float = 1f
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val refractionShader = obtainRuntimeShader("Refraction", RoundedRectRefractionShaderString).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", height)
                setFloatUniform("refractionAmount", -amount)
                setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
            }
            val dispersionShader = obtainRuntimeShader("Dispersion", RoundedRectDispersionShaderString).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("dispersionHeight", height * dispersionIntensity)
                setFloatUniform("dispersionAmount", amount * dispersionIntensity)
            }
            RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(dispersionShader, "content"),
                RenderEffect.createRuntimeShaderEffect(refractionShader, "content")
            )
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

private val BackdropEffectScope.cornerRadii: FloatArray?
    get() {
        val shape = shape as? CornerBasedShape ?: return null
        val size = size
        val maxRadius = size.minDimension / 2f
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft =
            if (isLtr) shape.topStart.toPx(size, this)
            else shape.topEnd.toPx(size, this)
        val topRight =
            if (isLtr) shape.topEnd.toPx(size, this)
            else shape.topStart.toPx(size, this)
        val bottomRight =
            if (isLtr) shape.bottomEnd.toPx(size, this)
            else shape.bottomStart.toPx(size, this)
        val bottomLeft =
            if (isLtr) shape.bottomStart.toPx(size, this)
            else shape.bottomEnd.toPx(size, this)
        return floatArrayOf(
            topLeft.fastCoerceAtMost(maxRadius),
            topRight.fastCoerceAtMost(maxRadius),
            bottomRight.fastCoerceAtMost(maxRadius),
            bottomLeft.fastCoerceAtMost(maxRadius)
        )
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException("Only CornerBasedShape is supported in lens effects.")
}
