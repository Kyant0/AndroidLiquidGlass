package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

enum class AgentAuraMode {
    Idle,
    Listening,
    Thinking,
    Speaking
}

fun AgentUiState.agentAuraMode(): AgentAuraMode {
    return when {
        isPlayingAudio -> AgentAuraMode.Speaking
        isVoiceListening || isRecordingVoice -> AgentAuraMode.Listening
        isVoiceThinking || isVoiceSessionConnecting -> AgentAuraMode.Thinking
        else -> AgentAuraMode.Idle
    }
}

@Composable
fun AgentAuraSurface(
    modifier: Modifier = Modifier,
    mode: AgentAuraMode,
    contentColor: Color,
    accentColor: Color,
    surfaceColor: Color,
    reduceAnimations: Boolean = false,
    cornerRadiusDp: Float = 28f,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "agent_aura_surface")
    val rotationDuration = when (mode) {
        AgentAuraMode.Idle -> 9800
        AgentAuraMode.Listening -> 2800
        AgentAuraMode.Thinking -> 2200
        AgentAuraMode.Speaking -> 1600
    }
    val pulseDuration = when (mode) {
        AgentAuraMode.Idle -> 3200
        AgentAuraMode.Listening -> 1700
        AgentAuraMode.Thinking -> 2100
        AgentAuraMode.Speaking -> 1300
    }
    val rotation = if (reduceAnimations) {
        0f
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "agent_aura_rotation"
        ).value
    }
    val pulse = if (reduceAnimations) {
        1f
    } else {
        transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = pulseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "agent_aura_pulse"
        ).value
    }
    val traceAlpha = when (mode) {
        AgentAuraMode.Idle -> 0.18f
        AgentAuraMode.Listening -> 0.34f
        AgentAuraMode.Thinking -> 0.42f
        AgentAuraMode.Speaking -> 0.54f
    }
    val haloAlpha = when (mode) {
        AgentAuraMode.Idle -> 0.06f
        AgentAuraMode.Listening -> 0.10f
        AgentAuraMode.Thinking -> 0.11f
        AgentAuraMode.Speaking -> 0.15f
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(surfaceColor)
            .drawWithCache {
                val strokeWidth = 1.35.dp.toPx()
                val glowWidth = 2.8.dp.toPx()
                val cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx(), cornerRadiusDp.dp.toPx())
                val center = Offset(size.width / 2f, size.height / 2f)
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = 0.04f * pulse),
                        accentColor.copy(alpha = traceAlpha * 0.65f * pulse),
                        contentColor.copy(alpha = traceAlpha * pulse),
                        accentColor.copy(alpha = traceAlpha * 0.9f * pulse),
                        Color.Transparent
                    ),
                    center = center
                )
                val haloBrush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = haloAlpha * pulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.78f
                )

                onDrawWithContent {
                    drawRoundRect(
                        brush = haloBrush,
                        cornerRadius = cornerRadius,
                        blendMode = BlendMode.SrcOver
                    )
                    drawContent()
                    drawRoundRect(
                        color = contentColor.copy(alpha = 0.08f),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokeWidth)
                    )
                    rotate(rotation, center) {
                        drawRoundRect(
                            brush = sweepBrush,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = glowWidth)
                        )
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun AgentAuraOrb(
    modifier: Modifier = Modifier,
    mode: AgentAuraMode,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "agent_orb_aura")
    val duration = when (mode) {
        AgentAuraMode.Idle -> 3000
        AgentAuraMode.Listening -> 1600
        AgentAuraMode.Thinking -> 2100
        AgentAuraMode.Speaking -> 1150
    }
    val pulse = if (reduceAnimations) {
        1f
    } else {
        transition.animateFloat(
            initialValue = 0.86f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "agent_orb_pulse"
        ).value
    }
    val haloAlpha = when (mode) {
        AgentAuraMode.Idle -> 0.10f
        AgentAuraMode.Listening -> 0.16f
        AgentAuraMode.Thinking -> 0.18f
        AgentAuraMode.Speaking -> 0.24f
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Transparent)
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height / 2f)
                val haloBrush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = haloAlpha * pulse),
                        accentColor.copy(alpha = haloAlpha * 0.38f * pulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.maxDimension * 0.62f * pulse
                )

                onDrawBehind {
                    drawCircle(
                        brush = haloBrush,
                        radius = size.minDimension * 0.54f * pulse,
                        center = center
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.22f * pulse),
                        radius = size.minDimension * 0.42f,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}
