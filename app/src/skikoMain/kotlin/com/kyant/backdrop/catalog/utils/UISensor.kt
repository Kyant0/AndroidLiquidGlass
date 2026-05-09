package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Composable
actual fun rememberUISensor(): UISensor {
    val uiSensor = remember { UISensorImpl() }

    DisposableEffect(Unit) {
        uiSensor.start()
        onDispose { uiSensor.stop() }
    }

    return uiSensor
}

private class UISensorImpl : UISensor {

    override var gravityAngle: Float by mutableFloatStateOf(45f)
        private set

    override var gravity: Offset by mutableStateOf(Offset.Zero)
        private set

    override fun start() = Unit

    override fun stop() = Unit
}
