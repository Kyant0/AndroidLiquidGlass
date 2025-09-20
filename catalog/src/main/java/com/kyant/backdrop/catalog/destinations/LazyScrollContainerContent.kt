package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.rememberBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun LazyScrollContainerContent() {
    val backdrop = rememberBackdrop()

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.wallpaper_light),
            null,
            Modifier
                .backdrop(backdrop)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16f.dp),
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            item {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
            }
            items(20) {
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop,
                            { ContinuousRoundedRectangle(32f.dp) }
                        ) {
                            saturation()
                            refraction(16f.dp.toPx(), 32f.dp.toPx())
                        }
                        .height(160f.dp)
                        .fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
            }
        }
    }
}
