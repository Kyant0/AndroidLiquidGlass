package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.shapes.RoundedRectangle

@Composable
fun SliderContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val backgroundColor =
        if (isLightTheme) Color(0xFFFFFFFF)
        else Color(0xFF121212)

    BackdropDemoScaffold { backdrop ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            var value by rememberSaveable { mutableFloatStateOf(50f) }

            LiquidSlider(
                value = { value },
                onValueChange = { value = it },
                valueRange = 0f..100f,
                visibilityThreshold = 0.01f,
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 32f.dp)
            )

            Box(
                Modifier
                    .padding(24f.dp)
                    .clip(RoundedRectangle(32f.dp))
                    .background(backgroundColor)
                    .padding(24f.dp)
            ) {
                LiquidSlider(
                    value = { value },
                    onValueChange = { value = it },
                    valueRange = 0f..100f,
                    visibilityThreshold = 0.01f,
                    backdrop = rememberCanvasBackdrop { drawRect(backgroundColor) },
                    modifier = Modifier.padding(horizontal = 32f.dp)
                )
            }
        }
    }
}
