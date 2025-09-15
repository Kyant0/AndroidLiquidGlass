package com.kyant.backdrop.highlight

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.DynamicHighlightStyleShaderString
import kotlin.math.PI

@Immutable
interface HighlightStyle {

    fun createRenderEffect(size: Size, width: Float): RenderEffect? {
        return null
    }

    @Immutable
    data object Solid : HighlightStyle

    @Immutable
    data object Soft : HighlightStyle {

        override fun createRenderEffect(size: Size, width: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = width / 2f
                if (blurRadiusPx > 0f) {
                    RenderEffect.createBlurEffect(
                        blurRadiusPx,
                        blurRadiusPx,
                        Shader.TileMode.DECAL
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : HighlightStyle {

        override fun createRenderEffect(size: Size, width: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = width / 2f
                val blurRenderEffect =
                    if (blurRadiusPx > 0f) {
                        RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.DECAL
                        )
                    } else {
                        null
                    }

                val highlightRenderEffect =
                    RenderEffect.createRuntimeShaderEffect(
                        RuntimeShader(DynamicHighlightStyleShaderString).apply {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("angle", angle * (PI / 180f).toFloat())
                            setFloatUniform("falloff", falloff)
                        },
                        "image"
                    )

                if (blurRenderEffect != null) {
                    RenderEffect.createChainEffect(
                        highlightRenderEffect,
                        blurRenderEffect
                    )
                } else {
                    highlightRenderEffect
                }
            } else {
                null
            }
        }
    }

    companion object {

        @Stable
        val Default: Dynamic = Dynamic()
    }
}
