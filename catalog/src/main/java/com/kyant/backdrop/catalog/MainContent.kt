package com.kyant.backdrop.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kyant.backdrop.catalog.destinations.ControlCenterContent
import com.kyant.backdrop.catalog.destinations.DialogContent
import com.kyant.backdrop.catalog.destinations.HomeContent

@Composable
fun MainContent() {
    var destination by rememberSaveable { mutableStateOf(CatalogDestination.Home) }

    BackHandler(destination != CatalogDestination.Home) {
        destination = CatalogDestination.Home
    }

    Crossfade(
        destination,
        Modifier.fillMaxSize(),
        animationSpec = spring()
    ) { dest ->
        when (dest) {
            CatalogDestination.Home -> HomeContent(onNavigate = { destination = it })
            CatalogDestination.ControlCenter -> ControlCenterContent()
            CatalogDestination.Dialog -> DialogContent()
        }
    }
}
