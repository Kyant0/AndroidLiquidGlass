package com.kyant.backdrop.catalog

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/**
 * Catalog-only radiance control; unsupported and SDR windows use SDR
 * white.
 */
internal val LocalCatalogHdrStrength = compositionLocalOf { 1f }

/**
 * Catalog-only switch; false reproduces SDR component output in the
 * comparison.
 */
internal val LocalCatalogComponentHdr = compositionLocalOf { true }

/**
 * Linear extended sRGB white for a catalog radiance strength. Values above
 * 1 stay above SDR white through the library's shader and paint paths.
 */
internal fun catalogHdrWhite(strength: Float): Color =
    if (strength > 1f) Color(strength, strength, strength, colorSpace = ColorSpaces.LinearExtendedSrgb)
    else Color.White
