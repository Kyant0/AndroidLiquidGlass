package com.kyant.backdrop.effects

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.RoundedRectRefractionShaderString
import com.kyant.backdrop.RoundedRectRefractionWithDispersionShaderString
import com.kyant.shapes.RoundedRectangularShape
import org.jetbrains.skia.ImageFilter

fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) {
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).coerceAtLeast(0f)
    }

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val builder =
                if (!chromaticAberration) {
                    obtainRuntimeShaderBuilder(
                        "Refraction",
                        RoundedRectRefractionShaderString
                    )
                } else {
                    obtainRuntimeShaderBuilder(
                        "RefractionWithDispersion",
                        RoundedRectRefractionWithDispersionShaderString
                    )
                }
            builder.apply {
                uniform("size", size.width, size.height)
                uniform("offset", -padding, -padding)
                uniform("cornerRadii", cornerRadii[0], cornerRadii[1], cornerRadii[2], cornerRadii[3])
                uniform("refractionHeight", refractionHeight)
                uniform("refractionAmount", -refractionAmount)
                uniform("depthEffect", if (depthEffect) 1f else 0f)
                if (chromaticAberration) {
                    uniform("chromaticAberration", 1f)
                }
            }
            ImageFilter.makeRuntimeShader(builder, "content", null)
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
                topLeft.coerceAtMost(maxRadius),
                topRight.coerceAtMost(maxRadius),
                bottomRight.coerceAtMost(maxRadius),
                bottomLeft.coerceAtMost(maxRadius)
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
                topLeft.coerceAtMost(maxRadius),
                topRight.coerceAtMost(maxRadius),
                bottomRight.coerceAtMost(maxRadius),
                bottomLeft.coerceAtMost(maxRadius)
            )
        }

        else -> null
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException(
        "Only RoundedRectangularShape or CornerBasedShape is supported in lens effects."
    )
}
