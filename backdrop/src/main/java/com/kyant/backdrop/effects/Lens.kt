package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.DispersionShaderString
import com.kyant.backdrop.RefractionShaderString

fun BackdropEffectScope.refraction(
    height: Float,
    amount: Float,
    hasDepthEffect: Boolean = false
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val shader = obtainRuntimeShader("Refraction", RefractionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadius", cornerRadiusArray)
        setFloatUniform("refractionHeight", height)
        setFloatUniform("refractionAmount", -amount)
        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    effect(effect)
}

fun BackdropEffectScope.dispersion(
    height: Float,
    amount: Float
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val shader = obtainRuntimeShader("Dispersion", DispersionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadius", cornerRadiusArray)
        setFloatUniform("dispersionHeight", height)
        setFloatUniform("dispersionAmount", amount)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    effect(effect)
}

fun BackdropEffectScope.refractionWithDispersion(
    height: Float,
    amount: Float,
    hasDepthEffect: Boolean = false,
    dispersionIntensity: Float = 1f
) {
    refraction(
        height = height,
        amount = amount,
        hasDepthEffect = hasDepthEffect
    )

    dispersion(
        height = height * dispersionIntensity,
        amount = amount * dispersionIntensity
    )
}
