package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.CatalogDestination

@Composable
fun HomeContent(onNavigate: (CatalogDestination) -> Unit) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .displayCutoutPadding()
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16f.dp)
    ) {
        BasicText(
            "Backdrop Catalog",
            Modifier.padding(16f.dp, 40f.dp, 16f.dp, 16f.dp),
            style = TextStyle(contentColor, 28f.sp, FontWeight.Medium)
        )

        Column {
            Subtitle("Liquid glass components")
            ListItem({ onNavigate(CatalogDestination.Buttons) }, "Buttons")
            ListItem({ onNavigate(CatalogDestination.Toggle) }, "Toggle")
            ListItem({ onNavigate(CatalogDestination.Slider) }, "Slider")
            ListItem({ onNavigate(CatalogDestination.BottomTabs) }, "Bottom tabs")
            ListItem({ onNavigate(CatalogDestination.Dialog) }, "Dialog")

            Subtitle("System UIs")
            ListItem({ onNavigate(CatalogDestination.LockScreen) }, "Lock screen (SDF texture)")
            ListItem({ onNavigate(CatalogDestination.ControlCenter) }, "Control center")
            ListItem({ onNavigate(CatalogDestination.Magnifier) }, "Magnifier")

            Subtitle("Experiments")
            ListItem({ onNavigate(CatalogDestination.GlassPlayground) }, "Glass playground")
            ListItem({ onNavigate(CatalogDestination.AdaptiveLuminanceGlass) }, "Adaptive luminance glass")
            ListItem({ onNavigate(CatalogDestination.ProgressiveBlur) }, "Progressive blur")
            ListItem({ onNavigate(CatalogDestination.ScrollContainer) }, "Scroll container")
            ListItem({ onNavigate(CatalogDestination.LazyScrollContainer) }, "Lazy scroll container")
        }
    }
}

@Composable
private fun Subtitle(label: String) {
    BasicText(
        label,
        Modifier
            .padding(16f.dp, 24f.dp, 16f.dp, 8f.dp)
            .fillMaxWidth(),
        style = TextStyle(Color(0xFF0088FF), 15f.sp, FontWeight.Medium)
    )
}

@Composable
private fun ListItem(
    onClick: () -> Unit,
    label: String
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White

    BasicText(
        label,
        Modifier
            .clickable(onClick = onClick)
            .padding(16f.dp)
            .fillMaxWidth(),
        style = TextStyle(contentColor, 17f.sp)
    )
}
