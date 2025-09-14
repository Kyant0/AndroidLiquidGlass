package com.kyant.glassmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.rememberLayerBackdrop

@Composable
fun MainContent() {
    val background = Color.White
    val backdrop = rememberLayerBackdrop(background)

    Box(
        Modifier.fillMaxSize()
    ) {
        // content
        Box(
            Modifier
                .backdrop(backdrop)
                .fillMaxSize()
        ) {
            SongsContent()
        }

        // top bar
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .safeDrawingPadding()
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .drawBackdrop(
                        backdrop,
                        { CircleShape },
                        onDrawSurface = { drawRect(background.copy(alpha = 0.5f)) }
                    ) {
                        saturation()
                        blur(2f.dp.toPx())
                        refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                    }
                    .clickable {}
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.arrow_back_24px),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                )
            }

            Box(
                Modifier
                    .drawBackdrop(
                        backdrop,
                        { CircleShape },
                        onDrawSurface = { drawRect(background.copy(alpha = 0.5f)) }
                    ) {
                        saturation()
                        blur(2f.dp.toPx())
                        refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                    }
                    .clickable {}
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.more_vert_24px),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                )
            }
        }

        // bottom bars
        Column(
            Modifier
                .padding(32.dp, 8.dp)
                .safeDrawingPadding()
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .drawBackdrop(backdrop, { CircleShape }) {
                        saturation()
                        blur(2f.dp.toPx())
                        refraction(height = size.minDimension / 4f, amount = size.minDimension / 2f)
                    }
                    .height(56.dp)
                    .fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedTab = remember { mutableStateOf(MainNavTab.Songs) }
                BottomTabs(
                    tabs = MainNavTab.entries,
                    selectedTabState = selectedTab,
                    backdrop = backdrop,
                    background = background,
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        MainNavTab.Songs -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.home_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Songs", color = color)
                            }
                        )

                        MainNavTab.Library -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.library_music_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Library", color = color)
                            }
                        )

                        MainNavTab.Settings -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.settings_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Settings", color = color)
                            }
                        )
                    }
                }

                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop,
                            { CircleShape },
                            highlight = {
                                Highlight(
                                    width = 2f.dp,
                                    color = Color(0xFFFFF59D),
                                    blendMode = BlendMode.Overlay
                                )
                            },
                            onDrawSurface = { drawRect(Color(0xFFFDD835).copy(alpha = 0.8f)) }
                        ) {
                            saturation()
                            blur(2f.dp.toPx())
                            refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                        }
                        .clickable {}
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.paint(
                            painterResource(R.drawable.search_24px),
                            colorFilter = ColorFilter.tint(Color.Black)
                        )
                    )
                }
            }
        }
    }
}
