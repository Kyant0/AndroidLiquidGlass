package com.kyant.backdrop

import androidx.compose.runtime.Composable
import kotlin.time.Duration

@Composable
internal actual fun rememberBackdropHdrState(interval: Duration): BackdropHdrState = BackdropHdrState()
