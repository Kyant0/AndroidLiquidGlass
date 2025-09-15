package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LoremIpsum
import com.kyant.backdrop.catalog.components.Text
import com.kyant.backdrop.catalog.theme.LocalContentColor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.rememberBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun DialogContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)
    val dimColor =
        if (isLightTheme) Color(0xFF29293A).copy(0.23f)
        else Color(0xFF121212).copy(0.56f)

    val backdrop = rememberBackdrop()

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.system_home_screen_light),
            null,
            Modifier
                .backdrop(backdrop)
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier
                .padding(40f.dp)
                .drawBackdrop(
                    backdrop,
                    { ContinuousRoundedRectangle(48f.dp) },
                    highlight = { Highlight { HighlightStyle.Soft } },
                    onDrawSurface = { drawRect(containerColor) }
                ) {
                    colorFilter(
                        brightness = if (isLightTheme) 0.2f else 0f,
                        saturation = 1.5f
                    )
                    blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                    refraction(24f.dp.toPx(), 48f.dp.toPx(), true)
                }
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Text(
                "Dialog Title",
                TextStyle(fontSize = 24f.sp, fontWeight = FontWeight.Medium),
                Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp)
            )

            Text(
                LoremIpsum,
                TextStyle(fontSize = 15f.sp),
                Modifier
                    .then(
                        if (isLightTheme) {
                            // plus darker
                            Modifier
                                .drawWithCache {
                                    val bottomLayer = obtainGraphicsLayer().apply {
                                        blendMode = BlendMode.Overlay
                                    }
                                    val topLayer = obtainGraphicsLayer().apply {
                                        blendMode = BlendMode.Luminosity
                                    }

                                    onDrawWithContent {
                                        bottomLayer.record {
                                            this@onDrawWithContent.drawContent()
                                        }
                                        topLayer.record {
                                            this@onDrawWithContent.drawContent()
                                        }

                                        drawLayer(bottomLayer)
                                        drawLayer(topLayer)
                                    }
                                }
                        } else {
                            // plus lighter
                            Modifier.graphicsLayer(blendMode = BlendMode.Plus)
                        }
                    )
                    .padding(24f.dp, 12f.dp, 24f.dp, 12f.dp),
                color = LocalContentColor.current.copy(0.68f),
                maxLines = 5
            )

            Row(
                Modifier
                    .padding(24f.dp, 12f.dp, 24f.dp, 24f.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16f.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(containerColor.copy(0.2f))
                        .clickable {}
                        .height(48f.dp)
                        .weight(1f)
                        .padding(horizontal = 16f.dp),
                    horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cancel",
                        TextStyle(fontSize = 16f.sp)
                    )
                }
                Row(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(accentColor)
                        .clickable {}
                        .height(48f.dp)
                        .weight(1f)
                        .padding(horizontal = 16f.dp),
                    horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Okay",
                        TextStyle(fontSize = 16f.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}
