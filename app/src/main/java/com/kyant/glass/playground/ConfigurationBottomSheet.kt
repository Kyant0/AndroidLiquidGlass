package com.kyant.glass.playground

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.expressa.components.button.Button
import com.kyant.expressa.components.button.ButtonColors
import com.kyant.expressa.components.iconbutton.IconButton
import com.kyant.expressa.components.iconbutton.IconButtonColors
import com.kyant.expressa.m3.motion.MotionScheme
import com.kyant.expressa.m3.shape.CornerShape
import com.kyant.expressa.prelude.*
import com.kyant.expressa.ui.Icon
import com.kyant.expressa.ui.Text
import com.kyant.glass.R
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.LiquidGlassProviderState
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight

@Composable
fun ConfigurationBottomSheet(
    state: PreviewState,
    providerState: LiquidGlassProviderState,
    modifier: Modifier = Modifier
) {
    // val providerState = rememberLiquidGlassProviderState()
    CompositionLocalProvider(
        // LocalLiquidGlassProviderState provides providerState
    ) {
        Column(
            modifier
                // .liquidGlassProvider(providerState)
                .liquidGlass(
                    providerState,
                    GlassStyle(
                        shape = CornerShape.extraLarge,
                        innerRefraction = InnerRefraction(
                            height = RefractionHeight(24.dp),
                            amount = RefractionAmount((-64).dp)
                        ),
                        material = GlassMaterial(
                            blurRadius = 8.dp,
                            brush = SolidColor(surface.copy(alpha = 0.3f))
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {}
                }
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (state.configurationMode) {
                        ConfigurationMode.Colors -> stringResource(R.string.colors)
                        ConfigurationMode.Advanced -> stringResource(R.string.advanced_settings)
                        null -> ""
                    },
                    titleMedium,
                    Modifier.weight(1f)
                )

                IconButton(
                    { state.configurationMode = null },
                    Modifier.liquidGlass(providerState, GlassStyle(CornerShape.full)),
                    colors = IconButtonColors.tonal(
                        containerColor = primaryContainer.copy(alpha = 0.85f)
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.close_24px),
                        stringResource(R.string.close)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val retainedConfigurationMode = remember {
                    Snapshot.withoutReadObservation { state.configurationMode }
                }
                when (retainedConfigurationMode) {
                    ConfigurationMode.Colors -> {
                        SliderChip(
                            state.blurRadius,
                            stringResource(R.string.blur_radius),
                            { Icon(painterResource(R.drawable.lens_blur_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                        SliderChip(
                            state.contrast,
                            stringResource(R.string.contrast),
                            { Icon(painterResource(R.drawable.contrast_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                        SliderChip(
                            state.whitePoint,
                            stringResource(R.string.white_point),
                            { Icon(painterResource(R.drawable.wb_sunny_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                        SliderChip(
                            state.chromaMultiplier,
                            stringResource(R.string.chroma_multiplier),
                            { Icon(painterResource(R.drawable.palette_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                    }

                    ConfigurationMode.Advanced -> {
                        Button(
                            { state.unsafeMode = !state.unsafeMode },
                            Modifier.liquidGlass(providerState, GlassStyle(CornerShape.full)),
                            colors = ButtonColors.filled(
                                containerColor = primary.copy(alpha = 0.85f)
                            )
                        ) {
                            Text(
                                if (state.unsafeMode) stringResource(R.string.disable_unsafe_mode)
                                else stringResource(R.string.enable_unsafe_mode),
                                Modifier.animateContentSize(MotionScheme.fastSpatial())
                            )
                        }
                        SliderChip(
                            state.bleedBlurRadius,
                            stringResource(R.string.bleed_blur_radius),
                            { Icon(painterResource(R.drawable.lens_blur_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                        SliderChip(
                            state.eccentricFactor,
                            stringResource(R.string.eccentric_factor),
                            { Icon(painterResource(R.drawable.all_out_24px)) },
                            providerState,
                            Modifier.fillMaxWidth()
                        )
                        Button(
                            { state.isDispersionEnabled = !state.isDispersionEnabled },
                            Modifier.liquidGlass(providerState, GlassStyle(CornerShape.full)),
                            colors = ButtonColors.filled(
                                containerColor = primary.copy(alpha = 0.85f)
                            )
                        ) {
                            Text(
                                if (state.isDispersionEnabled) stringResource(R.string.disable_dispersion)
                                else stringResource(R.string.enable_dispersion),
                                Modifier.animateContentSize(MotionScheme.fastSpatial())
                            )
                        }
                    }

                    null -> {}
                }
            }
        }
    }
}
