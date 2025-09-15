package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.DispersionShaderString
import com.kyant.backdrop.RefractionShaderString

fun BackdropEffectScope.refraction(
    height: Float,
    amount: Float = height,
    hasDepthEffect: Boolean = false
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val shader = obtainRuntimeShader("Refraction", RefractionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("refractionHeight", height)
        setFloatUniform("refractionAmount", -amount)
        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    effect(effect)
}

fun BackdropEffectScope.dispersion(
    height: Float,
    amount: Float = height
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val shader = obtainRuntimeShader("Dispersion", DispersionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("dispersionHeight", height)
        setFloatUniform("dispersionAmount", amount)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
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

    val refractionShader = obtainRuntimeShader("Refraction", RefractionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("refractionHeight", height)
        setFloatUniform("refractionAmount", -amount)
        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
    }
    val dispersionShader = obtainRuntimeShader("Dispersion", DispersionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("dispersionHeight", height * dispersionIntensity)
        setFloatUniform("dispersionAmount", amount * dispersionIntensity)
    }

    val effect = RenderEffect.createChainEffect(
        RenderEffect.createRuntimeShaderEffect(dispersionShader, "image"),
        RenderEffect.createRuntimeShaderEffect(refractionShader, "image")
    )
    effect(effect)
}
