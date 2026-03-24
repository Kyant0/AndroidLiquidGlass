package com.kyant.backdrop.catalog.linkedin

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.R

const val DefaultGlassBackgroundPresetKey = "wallpaper"
const val DefaultAccentPaletteKey = "linkedin"
const val DefaultGlassMotionStyleKey = "float"

data class GlassBackgroundPreset(
    val key: String,
    val name: String,
    val description: String,
    @param:DrawableRes val imageResId: Int? = null,
    val baseColors: List<Color>,
    val orbColors: List<Color>
)

data class GlassAccentPalette(
    val key: String,
    val name: String,
    val color: Color
)

data class GlassMotionStyle(
    val key: String,
    val name: String,
    val description: String,
    val travel: Float,
    val durationMillis: Int,
    val pulseAmount: Float
)

private data class GlassMotionState(
    val primaryX: Float,
    val primaryY: Float,
    val secondaryX: Float,
    val secondaryY: Float,
    val tertiaryX: Float,
    val tertiaryY: Float,
    val pulse: Float
)

val GlassBackgroundPresets = listOf(
    GlassBackgroundPreset(
        key = "wallpaper",
        name = "Blue Bloom",
        description = "The current cool blue glass background.",
        baseColors = listOf(
            Color(0xFFF9FBFE),
            Color(0xFFF2F6FB),
            Color(0xFFE7EEF7)
        ),
        orbColors = listOf(
            Color(0xFF7DB7FF),
            Color(0xFF2F76E8),
            Color(0xFFB9E9FF)
        )
    ),
    GlassBackgroundPreset(
        key = "crystal",
        name = "Crystal Wallpaper",
        description = "The original image wallpaper with soft glass glow.",
        imageResId = R.drawable.wallpaper_light,
        baseColors = listOf(
            Color(0xFFE9F8FF),
            Color(0xFFD6F0FF),
            Color(0xFFC7E7FF)
        ),
        orbColors = listOf(
            Color(0xFF4AB9FF),
            Color(0xFF0E6EEA),
            Color(0xFF8DE9FF)
        )
    ),
    GlassBackgroundPreset(
        key = "lagoon",
        name = "Lagoon Drift",
        description = "Teal and blue layers with airy highlights.",
        baseColors = listOf(
            Color(0xFFE7FBFF),
            Color(0xFFD5F6F7),
            Color(0xFFEAF4FF)
        ),
        orbColors = listOf(
            Color(0xFF33C9DD),
            Color(0xFF0094D7),
            Color(0xFF93FFF2)
        )
    ),
    GlassBackgroundPreset(
        key = "aurora",
        name = "Aurora Mint",
        description = "Minty glass gradients with cool cyan glow.",
        baseColors = listOf(
            Color(0xFFEFFFF7),
            Color(0xFFD9FBF0),
            Color(0xFFE6F7FF)
        ),
        orbColors = listOf(
            Color(0xFF35D6B4),
            Color(0xFF29B7D8),
            Color(0xFFA7FFF0)
        )
    ),
    GlassBackgroundPreset(
        key = "peach",
        name = "Peach Prism",
        description = "Warm sunrise glass with coral and lilac.",
        baseColors = listOf(
            Color(0xFFFFF1EB),
            Color(0xFFFFE4F3),
            Color(0xFFEFE8FF)
        ),
        orbColors = listOf(
            Color(0xFFFF9C7B),
            Color(0xFFFF5DA2),
            Color(0xFFC59BFF)
        )
    ),
    GlassBackgroundPreset(
        key = "violet",
        name = "Violet Haze",
        description = "Lavender glass tones with cool blue depth.",
        baseColors = listOf(
            Color(0xFFF6F1FF),
            Color(0xFFE6EEFF),
            Color(0xFFE8F5FF)
        ),
        orbColors = listOf(
            Color(0xFF8677FF),
            Color(0xFF59A8FF),
            Color(0xFFCDB8FF)
        )
    ),
    GlassBackgroundPreset(
        key = "sunburst",
        name = "Pure Yellow",
        description = "A bold yellow wash with warm amber glow.",
        baseColors = listOf(
            Color(0xFFFFE100),
            Color(0xFFFFE100),
            Color(0xFFFFC400)
        ),
        orbColors = listOf(
            Color(0xFFFFF176),
            Color(0xFFFFB300),
            Color(0xFFFF8F00)
        )
    ),
    GlassBackgroundPreset(
        key = "evergreen",
        name = "Pure Green",
        description = "Fresh emerald color with clean glass depth.",
        baseColors = listOf(
            Color(0xFF22C55E),
            Color(0xFF22C55E),
            Color(0xFF16A34A)
        ),
        orbColors = listOf(
            Color(0xFF86EFAC),
            Color(0xFF16A34A),
            Color(0xFF14532D)
        )
    ),
    GlassBackgroundPreset(
        key = "obsidian",
        name = "Black Glass",
        description = "A deep black backdrop with subtle cool shine.",
        baseColors = listOf(
            Color(0xFF08090D),
            Color(0xFF08090D),
            Color(0xFF111827)
        ),
        orbColors = listOf(
            Color(0xFF334155),
            Color(0xFF0F172A),
            Color(0xFF38BDF8)
        )
    ),
    GlassBackgroundPreset(
        key = "cobalt",
        name = "Pure Blue",
        description = "Strong blue color with bright electric bloom.",
        baseColors = listOf(
            Color(0xFF2563EB),
            Color(0xFF2563EB),
            Color(0xFF1D4ED8)
        ),
        orbColors = listOf(
            Color(0xFF93C5FD),
            Color(0xFF1D4ED8),
            Color(0xFF0F172A)
        )
    ),
    GlassBackgroundPreset(
        key = "ember",
        name = "Pure Red",
        description = "Confident red tones with a soft ruby glow.",
        baseColors = listOf(
            Color(0xFFEF4444),
            Color(0xFFEF4444),
            Color(0xFFDC2626)
        ),
        orbColors = listOf(
            Color(0xFFFCA5A5),
            Color(0xFFDC2626),
            Color(0xFF7F1D1D)
        )
    )
)

val GlassAccentPalettes = listOf(
    GlassAccentPalette("linkedin", "LinkedIn Blue", Color(0xFF0A66C2)),
    GlassAccentPalette("aqua", "Aqua", Color(0xFF0EA5E9)),
    GlassAccentPalette("mint", "Mint", Color(0xFF14B8A6)),
    GlassAccentPalette("rose", "Rose", Color(0xFFF43F5E)),
    GlassAccentPalette("violet", "Violet", Color(0xFF8B5CF6))
)

val GlassMotionStyles = listOf(
    GlassMotionStyle(
        key = "still",
        name = "Still",
        description = "No ambient drift.",
        travel = 0f,
        durationMillis = 1,
        pulseAmount = 0f
    ),
    GlassMotionStyle(
        key = "float",
        name = "Float",
        description = "Slow, calm glass motion.",
        travel = 26f,
        durationMillis = 18000,
        pulseAmount = 0.04f
    ),
    GlassMotionStyle(
        key = "bloom",
        name = "Bloom",
        description = "A little more shimmer and movement.",
        travel = 42f,
        durationMillis = 13000,
        pulseAmount = 0.07f
    )
)

fun glassBackgroundPreset(key: String): GlassBackgroundPreset =
    GlassBackgroundPresets.firstOrNull { it.key == key } ?: GlassBackgroundPresets.first()

fun glassAccentPalette(key: String): GlassAccentPalette =
    GlassAccentPalettes.firstOrNull { it.key == key } ?: GlassAccentPalettes.first()

fun glassMotionStyle(key: String): GlassMotionStyle =
    GlassMotionStyles.firstOrNull { it.key == key } ?: GlassMotionStyles[1]

@Composable
fun GlassBackgroundLayer(
    modifier: Modifier = Modifier,
    backgroundKey: String,
    accentColor: Color,
    motionStyleKey: String,
    reduceAnimations: Boolean
) {
    val motionStyle = glassMotionStyle(motionStyleKey)
    val effectiveMotion = if (reduceAnimations) GlassMotionStyles.first() else motionStyle

    Box(modifier = modifier) {
        Crossfade(
            targetState = backgroundKey,
            animationSpec = tween(
                durationMillis = if (reduceAnimations) 250 else 700
            ),
            label = "glassBackgroundCrossfade"
        ) { key ->
            GlassBackgroundVisual(
                preset = glassBackgroundPreset(key),
                accentColor = accentColor,
                motionStyle = effectiveMotion,
                animate = !reduceAnimations && effectiveMotion.key != "still"
            )
        }
    }
}

@Composable
fun GlassBackgroundPreview(
    modifier: Modifier = Modifier,
    presetKey: String,
    accentColor: Color = GlassAccentPalettes.first().color
) {
    GlassBackgroundVisual(
        modifier = modifier,
        preset = glassBackgroundPreset(presetKey),
        accentColor = accentColor,
        motionStyle = GlassMotionStyles.first(),
        animate = false
    )
}

@Composable
private fun GlassBackgroundVisual(
    preset: GlassBackgroundPreset,
    accentColor: Color,
    motionStyle: GlassMotionStyle,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val motion = rememberGlassMotionState(
        motionStyle = motionStyle,
        animate = animate
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(preset.baseColors))
    ) {
        if (preset.imageResId != null) {
            Image(
                painter = painterResource(preset.imageResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        AmbientOrb(
            alignment = Alignment.TopStart,
            sizeDp = 320f,
            offsetXDp = -118f + motion.primaryX,
            offsetYDp = -40f + motion.primaryY,
            scale = motion.pulse,
            colors = listOf(
                preset.orbColors[0].copy(alpha = 0.72f),
                preset.orbColors[1].copy(alpha = 0.24f),
                Color.Transparent
            )
        )

        AmbientOrb(
            alignment = Alignment.TopEnd,
            sizeDp = 228f,
            offsetXDp = 42f - motion.secondaryX,
            offsetYDp = 30f + motion.secondaryY,
            scale = 1f + (motion.pulse - 1f) * 0.6f,
            colors = listOf(
                accentColor.copy(alpha = 0.22f),
                accentColor.copy(alpha = 0.08f),
                Color.Transparent
            )
        )

        AmbientOrb(
            alignment = Alignment.CenterEnd,
            sizeDp = 360f,
            offsetXDp = 120f + motion.secondaryX,
            offsetYDp = 8f - motion.primaryY,
            scale = 1f,
            colors = listOf(
                preset.orbColors[1].copy(alpha = 0.82f),
                preset.orbColors[2].copy(alpha = 0.20f),
                Color.Transparent
            )
        )

        AmbientOrb(
            alignment = Alignment.BottomStart,
            sizeDp = 280f,
            offsetXDp = -58f + motion.tertiaryX,
            offsetYDp = 112f - motion.secondaryY,
            scale = 1f + (motion.pulse - 1f) * 0.35f,
            colors = listOf(
                preset.orbColors[2].copy(alpha = 0.52f),
                preset.orbColors[0].copy(alpha = 0.10f),
                Color.Transparent
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.06f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun rememberGlassMotionState(
    motionStyle: GlassMotionStyle,
    animate: Boolean
): GlassMotionState {
    if (!animate || motionStyle.key == "still") {
        return GlassMotionState(
            primaryX = 0f,
            primaryY = 0f,
            secondaryX = 0f,
            secondaryY = 0f,
            tertiaryX = 0f,
            tertiaryY = 0f,
            pulse = 1f
        )
    }

    val transition = rememberInfiniteTransition(label = "glassBackgroundMotion")

    val primaryX = transition.animateFloat(
        initialValue = -motionStyle.travel,
        targetValue = motionStyle.travel,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motionStyle.durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryX"
    )

    val primaryY = transition.animateFloat(
        initialValue = motionStyle.travel * 0.35f,
        targetValue = -motionStyle.travel * 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 0.88f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primaryY"
    )

    val secondaryX = transition.animateFloat(
        initialValue = motionStyle.travel * 0.2f,
        targetValue = -motionStyle.travel * 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 1.12f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryX"
    )

    val secondaryY = transition.animateFloat(
        initialValue = -motionStyle.travel * 0.55f,
        targetValue = motionStyle.travel * 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 0.94f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryY"
    )

    val tertiaryX = transition.animateFloat(
        initialValue = -motionStyle.travel * 0.16f,
        targetValue = motionStyle.travel * 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 1.35f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tertiaryX"
    )

    val tertiaryY = transition.animateFloat(
        initialValue = motionStyle.travel * 0.24f,
        targetValue = -motionStyle.travel * 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 1.22f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tertiaryY"
    )

    val pulse = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + motionStyle.pulseAmount,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (motionStyle.durationMillis * 0.72f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    return GlassMotionState(
        primaryX = primaryX.value,
        primaryY = primaryY.value,
        secondaryX = secondaryX.value,
        secondaryY = secondaryY.value,
        tertiaryX = tertiaryX.value,
        tertiaryY = tertiaryY.value,
        pulse = pulse.value
    )
}

@Composable
private fun BoxScope.AmbientOrb(
    alignment: Alignment,
    sizeDp: Float,
    offsetXDp: Float,
    offsetYDp: Float,
    scale: Float,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(
                x = offsetXDp.dp,
                y = offsetYDp.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(sizeDp.dp)
            .background(
                brush = Brush.radialGradient(colors = colors),
                shape = CircleShape
            )
    )
}
