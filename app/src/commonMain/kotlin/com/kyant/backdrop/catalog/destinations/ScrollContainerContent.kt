package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun ScrollContainerContent() {
    BackdropDemoScaffold { backdrop ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16f.dp)
                .systemBarsPadding()
                .displayCutoutPadding()
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            repeat(20) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(32f.dp) },
                            effects = {
                                vibrancy()
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            }
                        )
                        .height(160f.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
