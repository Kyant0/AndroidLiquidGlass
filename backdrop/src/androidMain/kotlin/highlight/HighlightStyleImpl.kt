package com.kyant.backdrop.highlight

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import com.kyant.backdrop.AmbientHighlightShaderString
import com.kyant.backdrop.DefaultHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCache
import kotlin.math.PI

@Immutable
data class DefaultHighlightStyle(
    override val color: Color = Color.White.copy(alpha = 0.5f),
    override val blendMode: BlendMode = BlendMode.Plus,
    val angle: Float = 45f,
    val falloff: Float = 1f
) : HighlightStyle {

    @Deprecated(
        "Use the constructor with color parameter instead.",
        ReplaceWith("DefaultHighlightStyle(color = Color.White.copy(alpha = intensity), angle = angle, falloff = falloff)")
    )
    constructor(
        intensity: Float,
        angle: Float = 45f,
        falloff: Float = 1f
    ) : this(
        color = Color.White.copy(alpha = intensity),
        angle = angle,
        falloff = falloff
    )

    @RequiresApi(Build.VERSION_CODES.S)
    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): Shader? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeShaderCache.obtainRuntimeShader(
                "Default",
                DefaultHighlightShaderString
            ).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", getCornerRadii(shape))
                setColorUniform("color", color.copy(alpha = 1f).toArgb())
                setFloatUniform("angle", angle * (PI / 180f).toFloat())
                setFloatUniform("falloff", falloff)
            }
        } else {
            null
        }
    }
}

@Immutable
data class AmbientHighlightStyle(
    val intensity: Float = 0.38f
) : HighlightStyle {

    override val color: Color = Color.White.copy(alpha = intensity)

    override val blendMode: BlendMode = DrawScope.DefaultBlendMode

    @RequiresApi(Build.VERSION_CODES.S)
    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): Shader? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeShaderCache.obtainRuntimeShader(
                "Ambient",
                AmbientHighlightShaderString
            ).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", getCornerRadii(shape))
                setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                setFloatUniform("falloff", 1f)
            }
        } else {
            null
        }
    }
}

internal actual fun expect_DefaultHighlightStyle(): HighlightStyle = DefaultHighlightStyle()
internal actual fun expect_AmbientHighlightStyle(): HighlightStyle = AmbientHighlightStyle()
