package com.kyant.backdrop.highlight

import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.AmbientHighlightShaderString
import com.kyant.backdrop.DynamicHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCacheScope
import kotlin.math.PI

@Immutable
interface HighlightStyle {

    val color: Color

    val blendMode: BlendMode

    @RequiresApi(Build.VERSION_CODES.S)
    fun DrawScope.createRenderEffect(
        shape: Shape,
        shaderCache: RuntimeShaderCacheScope
    ): RenderEffect?

    @Immutable
    data object Solid : HighlightStyle {

        override val color: Color = DefaultHighlightColor

        override val blendMode: BlendMode = DrawScope.DefaultBlendMode

        @RequiresApi(Build.VERSION_CODES.S)
        override fun DrawScope.createRenderEffect(
            shape: Shape,
            shaderCache: RuntimeShaderCacheScope
        ): RenderEffect? = null
    }

    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f,
        val isAmbient: Boolean = false
    ) : HighlightStyle {

        override val color: Color =
            if (isAmbient) Color.Black.copy(alpha = 0.38f)
            else DefaultHighlightColor

        override val blendMode: BlendMode =
            if (isAmbient) DrawScope.DefaultBlendMode else BlendMode.Plus

        @RequiresApi(Build.VERSION_CODES.S)
        override fun DrawScope.createRenderEffect(
            shape: Shape,
            shaderCache: RuntimeShaderCacheScope
        ): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader =
                    if (isAmbient) {
                        shaderCache.obtainRuntimeShader("Ambient", AmbientHighlightShaderString)
                    } else {
                        shaderCache.obtainRuntimeShader("Dynamic", DynamicHighlightShaderString)
                    }
                shader.apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
                android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            } else {
                null
            }
        }

        companion object {

            @Stable
            val Default: Dynamic = Dynamic()
        }
    }
}

@Stable
fun lerp(start: HighlightStyle, stop: HighlightStyle, fraction: Float): HighlightStyle {
    if (start is HighlightStyle.Solid && stop is HighlightStyle.Solid) {
        return HighlightStyle.Solid
    }
    if (start is HighlightStyle.Dynamic && stop is HighlightStyle.Dynamic) {
        return HighlightStyle.Dynamic(
            angle = lerp(start.angle, stop.angle, fraction),
            falloff = lerp(start.falloff, stop.falloff, fraction),
            isAmbient = if (fraction < 0.5f) start.isAmbient else stop.isAmbient
        )
    }
    return if (fraction < 0.5f) start else stop
}

private fun DrawScope.getCornerRadii(shape: Shape): FloatArray {
    val shape = shape as? CornerBasedShape ?: return FloatArray(4) { size.minDimension / 2f }
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

private val DefaultHighlightColor = Color.White.copy(alpha = 0.5f)
