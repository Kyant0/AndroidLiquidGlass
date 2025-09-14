package com.kyant.backdrop.catalog.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalContentColor: ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { Color.Black }
