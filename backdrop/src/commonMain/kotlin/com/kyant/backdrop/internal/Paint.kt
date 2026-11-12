package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Paint
import com.kyant.backdrop.RuntimeShader

internal expect fun Paint.blur(radius: Float)

internal expect fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?)

/** Sets an extended-range paint color without packing RGB into ARGB8. */
internal expect fun Paint.setHdrColor(color: androidx.compose.ui.graphics.Color)
