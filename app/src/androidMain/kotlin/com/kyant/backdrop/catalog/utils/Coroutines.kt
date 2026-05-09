package com.kyant.backdrop.catalog.utils

import kotlinx.coroutines.android.awaitFrame

actual suspend fun awaitFrame() {
    awaitFrame()
}
