package com.kyant.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.internal.RoundedRectRefractionShaderString
import com.kyant.backdrop.internal.RoundedRectRefractionWithDispersionShaderString
import com.kyant.backdrop.internal.RuntimeShaderEffect
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.shapes.RoundedRectangularShape

fun BackdropEffectScope.lens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val shader =
                if (!chromaticAberration) {
                    obtainRuntimeShader(
                        "Refraction",
                        RoundedRectRefractionShaderString
                    )
                } else {
                    obtainRuntimeShader(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    )
                }
            shader.apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("offset", -padding, -padding)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("refractionAmount", -refractionAmount)
                setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
                if (chromaticAberration) {
                    setFloatUniform("chromaticAberration", 1f)
                }
            }
            RuntimeShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

private val BackdropEffectScope.cornerRadii: FloatArray?
    get() = when (val shape = shape) {
        is RoundedRectangularShape -> {
            val corners = shape.corners(size, layoutDirection, this)
            floatArrayOf(
                corners.topLeft,
                corners.topRight,
                corners.bottomRight,
                corners.bottomLeft
            )
        }

        is AbsoluteRoundedCornerShape -> {
            val size = size
            val maxRadius = size.minDimension / 2f
            val topLeft = shape.topStart.toPx(size, this)
            val topRight = shape.topEnd.toPx(size, this)
            val bottomRight = shape.bottomEnd.toPx(size, this)
            val bottomLeft = shape.bottomStart.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }

        is CornerBasedShape -> {
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
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }

        else -> null
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException(
        "Only RoundedRectangularShape or CornerBasedShape is supported in lens effects."
    )
}
