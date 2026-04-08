package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.backdrops.LayerBackdrop

/**
 * Glass-themed dropdown menu that matches the app's glass aesthetic.
 *
 * @param useGlassBackdropEffects When false, uses a flat surface (no blur/lens/vibrancy) — avoids glow on dense UIs like post overflow menus.
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier,
    useGlassBackdropEffects: Boolean = true,
    content: @Composable () -> Unit
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )

    if (expandedState.currentState || expandedState.targetState) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrim/backdrop that dismisses on click
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismissRequest
                        )
                )
                
                // The actual menu positioned at top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                ) {
                    AnimatedVisibility(
                        visibleState = expandedState,
                        enter = fadeIn(tween(150)) + scaleIn(
                            tween(150),
                            transformOrigin = TransformOrigin(1f, 0f)
                        ),
                        exit = fadeOut(tween(100)) + scaleOut(
                            tween(100),
                            transformOrigin = TransformOrigin(1f, 0f)
                        )
                    ) {
                        if (useGlassBackdropEffects) {
                            Box(
                                modifier = modifier
                                    .width(200.dp)
                                    .vormexSurface(
                                        backdrop = backdrop,
                                        tone = VormexSurfaceTone.Overlay,
                                        cornerRadius = 16.dp,
                                        blurRadius = 24.dp,
                                        lensRadius = 6.dp,
                                        lensDepth = 12.dp,
                                        surfaceColor = appearance.overlayColor,
                                        borderColor = appearance.overlayBorderColor
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    content()
                                }
                            }
                        } else {
                            Box(
                                modifier = modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(appearance.overlayColor)
                                    .border(1.dp, appearance.overlayBorderColor, RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menu item for GlassDropdownMenu
 */
@Composable
fun GlassMenuItem(
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    text: String,
    textColor: Color = contentColor,
    enabled: Boolean = true
) {
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(appearance.subtleColor)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(12.dp))
        }
        BasicText(
            text = text,
            style = TextStyle(
                color = textColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Divider for GlassDropdownMenu
 */
@Composable
fun GlassMenuDivider(
    contentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .height(1.dp)
            .background(contentColor.copy(alpha = 0.1f))
    )
}
