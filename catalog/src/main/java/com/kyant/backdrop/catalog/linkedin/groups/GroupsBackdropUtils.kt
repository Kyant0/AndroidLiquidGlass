package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.shapes.RoundedRectangle

/**
 * Simplified drawBackdrop extension for Groups/Circles screens with standard glass effect.
 * Uses a rounded rectangle shape and applies blur + vibrancy effects.
 */
fun Modifier.glassBackground(
    backdrop: LayerBackdrop,
    blurRadius: Float = 20f,
    vibrancyAlpha: Float = 0.1f,
    cornerRadius: Float = 16f,
    surfaceAlpha: Float = 0.1f
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = maxOf(0.22f, surfaceAlpha + 0.10f)),
                Color.White.copy(alpha = maxOf(0.12f, surfaceAlpha))
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = maxOf(0.24f, vibrancyAlpha + 0.14f)),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

/**
 * Glass background with no rounded corners (for full-screen headers etc)
 */
fun Modifier.glassBackgroundFlat(
    backdrop: LayerBackdrop,
    blurRadius: Float = 20f,
    surfaceAlpha: Float = 0.1f
): Modifier = this
    .background(Color.White.copy(alpha = maxOf(0.16f, surfaceAlpha + 0.08f)))
