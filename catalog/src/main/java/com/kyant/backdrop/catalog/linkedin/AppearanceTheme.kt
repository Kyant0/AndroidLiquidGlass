package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

const val DefaultThemeModeKey = "glass"

enum class VormexThemeMode(val key: String) {
    Glass("glass"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromKey(key: String): VormexThemeMode =
            entries.firstOrNull { it.key == key } ?: Glass
    }
}

enum class VormexSurfaceTone {
    Card,
    Subtle,
    Sheet,
    Input,
    Overlay
}

data class VormexAppearance(
    val mode: VormexThemeMode,
    val backgroundColor: Color,
    val contentColor: Color,
    val cardColor: Color,
    val cardBorderColor: Color,
    val subtleColor: Color,
    val subtleBorderColor: Color,
    val sheetColor: Color,
    val sheetBorderColor: Color,
    val inputColor: Color,
    val inputBorderColor: Color,
    val overlayColor: Color,
    val overlayBorderColor: Color,
    val dividerColor: Color
) {
    val isGlassTheme: Boolean
        get() = mode == VormexThemeMode.Glass

    val isLightTheme: Boolean
        get() = mode != VormexThemeMode.Dark

    val isDarkTheme: Boolean
        get() = mode == VormexThemeMode.Dark

    fun surfaceColor(tone: VormexSurfaceTone): Color = when (tone) {
        VormexSurfaceTone.Card -> cardColor
        VormexSurfaceTone.Subtle -> subtleColor
        VormexSurfaceTone.Sheet -> sheetColor
        VormexSurfaceTone.Input -> inputColor
        VormexSurfaceTone.Overlay -> overlayColor
    }

    fun borderColor(tone: VormexSurfaceTone): Color = when (tone) {
        VormexSurfaceTone.Card -> cardBorderColor
        VormexSurfaceTone.Subtle -> subtleBorderColor
        VormexSurfaceTone.Sheet -> sheetBorderColor
        VormexSurfaceTone.Input -> inputBorderColor
        VormexSurfaceTone.Overlay -> overlayBorderColor
    }
}

val LocalVormexAppearance = compositionLocalOf<VormexAppearance?> { null }

@Composable
fun rememberVormexAppearance(themeMode: String): VormexAppearance =
    remember(themeMode) { appearanceForTheme(themeMode) }

@Composable
fun currentVormexAppearance(
    fallbackThemeMode: String = DefaultThemeModeKey
): VormexAppearance =
    LocalVormexAppearance.current ?: rememberVormexAppearance(fallbackThemeMode)

@Composable
fun ProvideVormexAppearance(
    themeMode: String,
    content: @Composable () -> Unit
) {
    val appearance = rememberVormexAppearance(themeMode)
    CompositionLocalProvider(LocalVormexAppearance provides appearance) {
        content()
    }
}

private fun appearanceForTheme(themeMode: String): VormexAppearance =
    when (VormexThemeMode.fromKey(themeMode)) {
        VormexThemeMode.Glass -> VormexAppearance(
            mode = VormexThemeMode.Glass,
            backgroundColor = Color.Transparent,
            contentColor = Color.Black,
            cardColor = Color.White.copy(alpha = 0.12f),
            cardBorderColor = Color.White.copy(alpha = 0.20f),
            subtleColor = Color.White.copy(alpha = 0.08f),
            subtleBorderColor = Color.White.copy(alpha = 0.14f),
            sheetColor = Color.White.copy(alpha = 0.72f),
            sheetBorderColor = Color.White.copy(alpha = 0.20f),
            inputColor = Color.White.copy(alpha = 0.24f),
            inputBorderColor = Color.White.copy(alpha = 0.16f),
            overlayColor = Color.White.copy(alpha = 0.18f),
            overlayBorderColor = Color.White.copy(alpha = 0.22f),
            dividerColor = Color.Black.copy(alpha = 0.08f)
        )
        VormexThemeMode.Light -> VormexAppearance(
            mode = VormexThemeMode.Light,
            backgroundColor = Color(0xFFF5F7FB),
            contentColor = Color(0xFF0F1720),
            cardColor = Color.White.copy(alpha = 0.98f),
            cardBorderColor = Color(0xFFDCE4EF),
            subtleColor = Color(0xFFF0F4F9),
            subtleBorderColor = Color(0xFFE2E9F2),
            sheetColor = Color(0xFFF8FAFD),
            sheetBorderColor = Color(0xFFDAE3EE),
            inputColor = Color.White,
            inputBorderColor = Color(0xFFD6E0EB),
            overlayColor = Color.White.copy(alpha = 0.96f),
            overlayBorderColor = Color(0xFFDCE4EF),
            dividerColor = Color(0xFFE7EDF5)
        )
        VormexThemeMode.Dark -> VormexAppearance(
            mode = VormexThemeMode.Dark,
            backgroundColor = Color(0xFF090C11),
            contentColor = Color(0xFFF8FAFC),
            cardColor = Color(0xFF121922),
            cardBorderColor = Color.White.copy(alpha = 0.08f),
            subtleColor = Color(0xFF17212D),
            subtleBorderColor = Color.White.copy(alpha = 0.06f),
            sheetColor = Color(0xFF0F1724),
            sheetBorderColor = Color.White.copy(alpha = 0.08f),
            inputColor = Color(0xFF101827),
            inputBorderColor = Color.White.copy(alpha = 0.08f),
            overlayColor = Color(0xFF141C28),
            overlayBorderColor = Color.White.copy(alpha = 0.08f),
            dividerColor = Color.White.copy(alpha = 0.06f)
        )
    }

fun Modifier.vormexSurface(
    backdrop: LayerBackdrop? = null,
    tone: VormexSurfaceTone = VormexSurfaceTone.Card,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 20.dp,
    lensRadius: Dp = 6.dp,
    lensDepth: Dp = 12.dp,
    useBackdropEffects: Boolean = true,
    surfaceColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(cornerRadius)
    val resolvedSurface = surfaceColor ?: appearance.surfaceColor(tone)
    val resolvedBorder = borderColor ?: appearance.borderColor(tone)

    if (appearance.isGlassTheme && backdrop != null && useBackdropEffects) {
        var modifier = this.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(cornerRadius) },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                if (lensRadius > 0.dp || lensDepth > 0.dp) {
                    lens(lensRadius.toPx(), lensDepth.toPx())
                }
            },
            onDrawSurface = {
                drawRect(resolvedSurface)
            }
        )

        modifier = modifier.clip(shape)

        if (resolvedBorder.alpha > 0f) {
            modifier = modifier.border(1.dp, resolvedBorder, shape)
        }

        modifier
    } else {
        var modifier = this
            .clip(shape)
            .background(resolvedSurface, shape)

        if (resolvedBorder.alpha > 0f) {
            modifier = modifier.border(1.dp, resolvedBorder, shape)
        }

        modifier
    }
}
