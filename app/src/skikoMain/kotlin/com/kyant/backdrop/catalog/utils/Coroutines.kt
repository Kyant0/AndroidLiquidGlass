package com.kyant.backdrop.catalog.utils

import kotlinx.coroutines.delay

actual suspend fun awaitFrame() {
    delay(1000L / 60L)
}
