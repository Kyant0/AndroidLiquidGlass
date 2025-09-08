package com.kyant.liquidglass.highlight

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.liquidglass.utils.GlassShaders
import kotlin.math.PI

/**
 * The highlight effect applied to the liquid glass.
 */
@Immutable
sealed interface GlassHighlight {

    val width: Dp

    val color: Color

    val blendMode: BlendMode

    @Stable
    fun createRenderEffect(size: Size, density: Density, cornerRadius: Float): RenderEffect? {
        return null
    }

    /**
     * A no-op highlight effect.
     */
    @Immutable
    data object None : GlassHighlight {

        override val width: Dp = Dp.Unspecified

        override val color: Color = Color.Unspecified

        override val blendMode: BlendMode = DrawScope.DefaultBlendMode
    }

    /**
     * A solid highlight effect.
     *
     * @param width
     * The width of the highlight.
     *
     * @param color
     * The color of the highlight.
     *
     * @param blendMode
     * The blend mode of the highlight.
     */
    @Immutable
    data class Solid(
        override val width: Dp = 1f.dp,
        override val color: Color = Color.White.copy(alpha = 0.4f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : GlassHighlight

    /**
     * A dynamic highlight effect that creates a shimmering effect.
     *
     * @param width
     * The width of the highlight.
     *
     * @param color
     * The color of the highlight.
     *
     * @param blendMode
     * The blend mode of the highlight.
     *
     * @param angle
     * The angle of the highlight in degrees.
     *
     * @param decay
     * The decay factor for the highlight, controlling how quickly it fades out.
     */
    @Immutable
    data class Dynamic(
        override val width: Dp = 1f.dp,
        override val color: Color = Color.White.copy(alpha = 0.4f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val decay: Float = 1.5f
    ) : GlassHighlight {

        private var highlightShaderCache: RuntimeShader? = null

        override fun createRenderEffect(size: Size, density: Density, cornerRadius: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRenderEffect =
                    cachedBlurRenderEffect
                        ?: RenderEffect.createBlurEffect(
                            with(density) { 0.5f.dp.toPx() },
                            with(density) { 0.5f.dp.toPx() },
                            Shader.TileMode.DECAL
                        ).also { cachedBlurRenderEffect = it }

                val highlightShader = highlightShaderCache
                    ?: RuntimeShader(GlassShaders.highlightShaderString)
                        .also { highlightShaderCache = it }

                val highlightRenderEffect =
                    RenderEffect.createRuntimeShaderEffect(
                        highlightShader.apply {
                            setFloatUniform("size", size.width, size.height)
                            setFloatUniform("cornerRadius", cornerRadius)
                            setFloatUniform("angle", angle * PI.toFloat() / 180f)
                            setFloatUniform("decay", decay)
                        },
                        "image"
                    )

                RenderEffect.createChainEffect(
                    highlightRenderEffect,
                    blurRenderEffect
                )
            } else {
                null
            }
        }

        private companion object {

            var cachedBlurRenderEffect: RenderEffect? = null
        }
    }

    companion object {

        @Stable
        val Default: Dynamic = Dynamic()
    }
}
