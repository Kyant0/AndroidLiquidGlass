package com.kyant.liquidglass.material

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ColorFilter
import com.kyant.backdrop.simpleColorFilter

@Deprecated(
    message = "Use the new package com.kyant.backdrop",
    replaceWith = ReplaceWith(
        "simpleColorFilter(brightness, contrast, saturation)",
        "com.kyant.backdrop.simpleColorFilter"
    )
)
@Stable
fun simpleColorFilter(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
): ColorFilter = simpleColorFilter(
    brightness = brightness,
    contrast = contrast,
    saturation = saturation
)
