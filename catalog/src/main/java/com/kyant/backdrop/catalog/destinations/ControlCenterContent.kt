package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.rememberUISensor
import com.kyant.backdrop.catalog.theme.LocalContentColor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun ControlCenterContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor = Color.White.copy(0.1f)
    val dimColor = Color.Black.copy(0.4f)

    val itemSpacing = 16f.dp
    val itemSize = 68f.dp
    val itemTwoSpanSize = itemSize * 2 + itemSpacing
    val itemShape = ContinuousRoundedRectangle(itemSize / 2f)

    val innerItemSize = 56f.dp
    val innerItemShape = ContinuousCapsule
    val innerItemIconScale = 0.8f

    val inactiveItemColor = Color.White.copy(0.2f)
    val activeItemColor = accentColor

    val airplaneModeIcon = painterResource(R.drawable.flight_40px)
    val iconColorFilter = ColorFilter.tint(Color.White)

    val backdrop = rememberLayerBackdrop(null)
    val uiSensor = rememberUISensor()

    fun Modifier.glass() =
        this
            .drawBackdrop(
                backdrop,
                { itemShape },
                highlight = { Highlight { HighlightStyle.Dynamic(angle = uiSensor.gravityAngle) } },
                onDrawSurface = { drawRect(containerColor) }
            ) {
                saturation()
                blur(16f.dp.toPx())
                refraction(24f.dp.toPx(), 48f.dp.toPx(), true)
            }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
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
                    .blur(8f.dp)
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                Modifier
                    .padding(top = 80f.dp)
                    .systemBarsPadding()
                    .displayCutoutPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .glass()
                            .size(itemTwoSpanSize)
                            .padding(itemSpacing)
                    ) {
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(inactiveItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.TopStart)
                        )
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(activeItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.TopEnd)
                        )
                        Box(
                            Modifier
                                .clip(innerItemShape)
                                .background(activeItemColor)
                                .clickable {}
                                .scale(innerItemIconScale)
                                .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                .size(innerItemSize)
                                .align(Alignment.BottomStart)
                        )
                    }
                    Box(
                        Modifier
                            .glass()
                            .size(itemTwoSpanSize)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .glass()
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                            Box(
                                Modifier
                                    .glass()
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                        Box(
                            Modifier
                                .glass()
                                .size(itemTwoSpanSize, itemSize)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .glass()
                                .size(itemSize, itemTwoSpanSize)
                        )
                        Box(
                            Modifier
                                .glass()
                                .size(itemSize, itemTwoSpanSize)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .glass()
                            .size(itemTwoSpanSize)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .glass()
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                            Box(
                                Modifier
                                    .glass()
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .glass()
                                    .clickable {}
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                                    .size(itemSize)
                            )
                        }
                    }
                }
            }
        }
    }
}
