package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.Paint
import com.kyant.backdrop.RuntimeShader

internal expect fun Paint.blur(radius: Float)

internal expect fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?)
