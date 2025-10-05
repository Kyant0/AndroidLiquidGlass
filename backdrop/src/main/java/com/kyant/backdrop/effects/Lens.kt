package com.kyant.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.RoundedRectRefractionShaderString
import com.kyant.backdrop.RoundedRectRefractionWithDispersionShaderString

@Deprecated(
    message = "Use lens() instead",
    replaceWith = ReplaceWith("lens(height, amount, hasDepthEffect)")
)
fun BackdropEffectScope.refraction(
    height: Float,
    amount: Float = height,
    hasDepthEffect: Boolean = false
) {
    lens(
        refractionHeight = height,
        refractionAmount = amount,
        hasDepthEffect = hasDepthEffect
    )
}

@Deprecated(
    message = "Use lens() with chromaticAberration parameter instead",
    replaceWith =
        ReplaceWith("lens(height, amount, hasDepthEffect)")
)
fun BackdropEffectScope.refractionWithDispersion(
    height: Float,
    amount: Float = height,
    hasDepthEffect: Boolean = false,
    dispersionIntensity: Float = 1f
) {
    lens(
        refractionHeight = height,
        refractionAmount = amount,
        hasDepthEffect = hasDepthEffect,
        chromaticAberration = DefaultChromaticAberration * dispersionIntensity
    )
}

val DefaultChromaticAberration: Offset = Offset(1f / 2f, 1f / 6f)

fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float = refractionHeight,
    hasDepthEffect: Boolean = false,
    chromaticAberration: Offset = Offset.Zero
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader =
                if (chromaticAberration == Offset.Zero) {
                    obtainRuntimeShader("Refraction", RoundedRectRefractionShaderString).apply {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("cornerRadii", cornerRadii)
                        setFloatUniform("refractionHeight", refractionHeight)
                        setFloatUniform("refractionAmount", -refractionAmount)
                        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
                    }
                } else {
                    obtainRuntimeShader(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    ).apply {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("cornerRadii", cornerRadii)
                        setFloatUniform("refractionHeight", refractionHeight)
                        setFloatUniform("refractionAmount", -refractionAmount)
                        setFloatUniform("depthEffect", if (hasDepthEffect) 1f else 0f)
                        setFloatUniform("chromaticAberration", chromaticAberration.x, chromaticAberration.y)
                    }
                }
            RenderEffect.createRuntimeShaderEffect(shader, "content")
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
