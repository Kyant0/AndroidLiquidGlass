package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CornerBasedShape
import com.kyant.backdrop.BackdropDrawScope
import com.kyant.backdrop.DispersionShaderString
import com.kyant.backdrop.RefractionShaderString

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun BackdropDrawScope.refraction(
    height: Float,
    amount: Float,
    hasDepthEffect: Boolean = false,
    cornerRadius: Float = Float.NaN
) {
    if (height <= 0f || amount <= 0f) {
        return
    }

    val cornerRadius =
        if (cornerRadius.isNaN()) {
            require(shape is CornerBasedShape) {
                "When cornerRadius is not specified, shape must be a CornerBasedShape"
            }
            (shape as CornerBasedShape).topStart.toPx(size, this)
        } else {
            cornerRadius
        }

    val shader = RuntimeShader(RefractionShaderString).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadius", cornerRadius)

        setFloatUniform("refractionHeight", height)
        setFloatUniform("refractionAmount", -amount)
        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    effect(effect)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun BackdropDrawScope.dispersion(
    height: Float,
    amount: Float
) {
    if (height <= 0f || amount <= 0f) {
        return
    }

    val shader = RuntimeShader(DispersionShaderString).apply {
        setFloatUniform("size", size.width, size.height)

        setFloatUniform("dispersionHeight", height)
        setFloatUniform("dispersionAmount", amount)
    }

    val effect = RenderEffect.createRuntimeShaderEffect(shader, "image")
    effect(effect)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun BackdropDrawScope.refractionWithDispersion(
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
