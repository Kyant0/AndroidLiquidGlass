package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun LockScreenContent() {
    BackdropDemoScaffold(
        Modifier.drawWithContent {
            drawContent()
            drawRect(Color.Black.copy(0.5f))
        }
    ) { backdrop ->
        Column(
            Modifier.padding(16f.dp),
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            Row(
                Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24f.dp) },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(0.05f)
                            )
                        },
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            refraction(16f.dp.toPx(), size.minDimension / 2f, true)
                        },
                        onDrawSurface = { drawRect(Color.White.copy(0.1f)) },
                        contentEffects = {
                            blur(4f.dp.toPx())
                            refraction(8f.dp.toPx(), 24f.dp.toPx())
                        }
                    )
                    .fillMaxWidth()
                    .padding(12f.dp),
                horizontalArrangement = Arrangement.spacedBy(12f.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.instagram_logo),
                    null,
                    Modifier.size(40f.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2f.dp)) {
                    BasicText(
                        "Instagram",
                        style = TextStyle(Color.White, 15f.sp)
                    )
                    BasicText(
                        "Kyant shared a photo.",
                        Modifier.graphicsLayer(blendMode = BlendMode.Plus),
                        style = TextStyle(Color.White.copy(0.5f), 15f.sp)
                    )
                }
            }
        }
    }
}
