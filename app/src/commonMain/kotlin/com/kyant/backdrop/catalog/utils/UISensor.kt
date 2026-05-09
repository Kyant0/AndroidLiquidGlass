package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

@Composable
expect fun rememberUISensor(): UISensor

interface UISensor {

    val gravityAngle: Float

    val gravity: Offset

    fun start()

    fun stop()
}
