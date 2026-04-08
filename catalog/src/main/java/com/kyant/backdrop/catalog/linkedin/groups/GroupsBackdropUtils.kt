package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.vormexSurface

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
    .vormexSurface(
        backdrop = backdrop,
        tone = VormexSurfaceTone.Card,
        cornerRadius = cornerRadius.dp,
        blurRadius = blurRadius.dp,
        lensRadius = 0.dp,
        lensDepth = 0.dp
    )

/**
 * Glass background with no rounded corners (for full-screen headers etc)
 */
fun Modifier.glassBackgroundFlat(
    backdrop: LayerBackdrop,
    blurRadius: Float = 20f,
    surfaceAlpha: Float = 0.1f
): Modifier = this
    .vormexSurface(
        backdrop = backdrop,
        tone = VormexSurfaceTone.Subtle,
        cornerRadius = 0.dp,
        blurRadius = blurRadius.dp,
        lensRadius = 0.dp,
        lensDepth = 0.dp
    )
