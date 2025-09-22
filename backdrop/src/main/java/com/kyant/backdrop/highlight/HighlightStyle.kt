package com.kyant.backdrop.highlight

import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.AmbientHighlightShaderString
import com.kyant.backdrop.DynamicHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCacheScope
import kotlin.math.PI

@Immutable
sealed interface HighlightStyle {

    fun RuntimeShaderCacheScope.createShader(
        size: Size,
        bitmapShader: BitmapShader
    ): Shader?

    @Immutable
    data object Solid : HighlightStyle {

        override fun RuntimeShaderCacheScope.createShader(
            size: Size,
            bitmapShader: BitmapShader
        ): Shader? = null
    }

    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f,
        val isAmbient: Boolean = false
    ) : HighlightStyle {

        override fun RuntimeShaderCacheScope.createShader(
            size: Size,
            bitmapShader: BitmapShader
        ): Shader? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader =
                    if (isAmbient) {
                        obtainRuntimeShader("Ambient", AmbientHighlightShaderString)
                    } else {
                        obtainRuntimeShader("Dynamic", DynamicHighlightShaderString)
                    }
                shader.apply {
                    setInputBuffer("image", bitmapShader)
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
            } else {
                null
            }
        }

        companion object {

            @Stable
            val Default: Dynamic = Dynamic()

            @Stable
            val AmbientDefault: Dynamic = Dynamic(isAmbient = true)
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
