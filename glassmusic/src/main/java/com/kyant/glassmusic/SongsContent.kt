package com.kyant.glassmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun SongsContent() {
    LazyVerticalGrid(
        GridCells.Fixed(3),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
        }

        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            BasicText(
                "Songs",
                Modifier
                    .padding(8.dp, 64.dp, 8.dp, 16.dp)
                    .fillMaxWidth(),
                style = TextStyle(fontSize = 32.sp)
            )
        }

        items(100) { index ->
            val color = remember { Color(Random.nextLong(0xFF000000, 0xFFFFFFFF)) }

            Box(
                Modifier
                    .background(color, RoundedCornerShape(16.dp))
                    .fillMaxSize()
                    .aspectRatio(1f)
            )
        }

        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
