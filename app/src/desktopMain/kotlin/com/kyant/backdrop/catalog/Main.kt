package com.kyant.backdrop.catalog

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center)),
        title = "Backdrop Catalog"
    ) {
        MainContent()
    }
}
