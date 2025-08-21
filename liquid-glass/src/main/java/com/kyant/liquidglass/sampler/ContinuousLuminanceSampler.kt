package com.kyant.liquidglass.sampler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastCoerceIn

@ExperimentalLuminanceSamplerApi
@Stable
class ContinuousLuminanceSampler(
    durationMillis: Long = 300L,
    easing: Easing = LinearEasing,
    val precision: Float = 0.25f,
    val scaledSize: IntSize = IntSize(5, 5),
    val onAnimateToLuminance: ((animationSpec: FiniteAnimationSpec<Float>, luminance: Float) -> Unit)? = null
) : LuminanceSampler {

    override val sampleIntervalMillis: Long = 0L

    private val impulseLuminanceSampler =
        ImpulseLuminanceSampler(
            sampleIntervalMillis = sampleIntervalMillis,
            precision = precision,
            scaledSize = scaledSize
        )

    private val luminanceAnimation = Animatable(0f)

    override val luminance: Float
        get() = luminanceAnimation.value.fastCoerceIn(0f, 1f)

    private var hasValidValue = false

    private val animationSpec =
        tween<Float>(durationMillis.toInt(), 0, easing)

    override suspend fun sample(graphicsLayer: GraphicsLayer) {
        impulseLuminanceSampler.sample(graphicsLayer)
        val sampledLuminance = impulseLuminanceSampler.luminance
        if (!sampledLuminance.isNaN()) {
            if (hasValidValue) {
                onAnimateToLuminance?.invoke(animationSpec, sampledLuminance)
                luminanceAnimation.animateTo(sampledLuminance, animationSpec)
            } else {
                onAnimateToLuminance?.invoke(snap(), sampledLuminance)
                luminanceAnimation.snapTo(sampledLuminance)
                hasValidValue = true
            }
        }
    }
}
