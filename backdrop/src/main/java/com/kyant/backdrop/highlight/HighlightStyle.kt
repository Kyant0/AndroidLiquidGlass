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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.AmbientHighlightShaderString
import com.kyant.backdrop.DefaultHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCache
import kotlin.math.PI

@Immutable
interface HighlightStyle {

    val color: Color

    val blendMode: BlendMode

    @RequiresApi(Build.VERSION_CODES.S)
    fun DrawScope.createRenderEffect(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): RenderEffect?

    @Immutable
    data class Plain(
        override val color: Color = Color.White.copy(alpha = 0.38f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : HighlightStyle {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun DrawScope.createRenderEffect(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RenderEffect? = null
    }

    @Immutable
    data class Default(
        override val color: Color = Color.White.copy(alpha = 0.5f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : HighlightStyle {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun DrawScope.createRenderEffect(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = runtimeShaderCache.obtainRuntimeShader("Default", DefaultHighlightShaderString)
                shader.apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
                android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            } else {
                null
            }
        }
    }

    @Immutable
    data class Ambient(
        override val color: Color = Color.White.copy(alpha = 0.5f),
        override val blendMode: BlendMode = DrawScope.DefaultBlendMode
    ) : HighlightStyle {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun DrawScope.createRenderEffect(
            shape: Shape,
            runtimeShaderCache: RuntimeShaderCache
        ): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = runtimeShaderCache.obtainRuntimeShader("Ambient", AmbientHighlightShaderString)
                shader.apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                    setFloatUniform("falloff", 1f)
                }
                android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            } else {
                null
            }
        }
    }

    companion object {

        @Stable
        val Default: Default = Default()

        @Stable
        val Ambient: Ambient = Ambient()

        @Stable
        val Plain: Plain = Plain()
    }
}

@Stable
fun lerp(start: HighlightStyle, stop: HighlightStyle, fraction: Float): HighlightStyle {
    if (start is HighlightStyle.Plain && stop is HighlightStyle.Plain) {
        return HighlightStyle.Plain(
            color = lerp(start.color, stop.color, fraction),
            blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode
        )
    }
    if (start is HighlightStyle.Default && stop is HighlightStyle.Default) {
        return HighlightStyle.Default(
            color = lerp(start.color, stop.color, fraction),
            blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode,
            angle = lerp(start.angle, stop.angle, fraction),
            falloff = lerp(start.falloff, stop.falloff, fraction)
        )
    }
    if (start is HighlightStyle.Ambient && stop is HighlightStyle.Ambient) {
        return HighlightStyle.Ambient(
            color = lerp(start.color, stop.color, fraction),
            blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode
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
