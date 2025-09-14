package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.DispersionShaderString
import com.kyant.backdrop.RefractionShaderString

fun BackdropEffectScope.refraction(
    height: Float,
    amount: Float,
    hasDepthEffect: Boolean = false,
    cornerRadius: Float = Float.NaN
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (height <= 0f || amount <= 0f) return

    val cornerRadius =
        if (cornerRadius.isNaN()) {
            val shape = shape
            require(shape is CornerBasedShape) {
                "When cornerRadius is not specified, shape must be a CornerBasedShape"
            }
            maxOf(
                shape.topStart.toPx(size, this),
                shape.topEnd.toPx(size, this),
                shape.bottomEnd.toPx(size, this),
                shape.bottomStart.toPx(size, this)
            )
        } else {
            cornerRadius
        }

    val shader = obtainRuntimeShader("Refraction", RefractionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadius", cornerRadius)

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
    cornerRadius: Float = Float.NaN,
    dispersionIntensity: Float = 1f
) {
    refraction(
        height = height,
        amount = amount,
        hasDepthEffect = hasDepthEffect,
        cornerRadius = cornerRadius
    )

    dispersion(
        height = height * dispersionIntensity,
        amount = amount * dispersionIntensity
    )
}
