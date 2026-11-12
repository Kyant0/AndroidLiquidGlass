package com.kyant.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Minimum interval between HDR layer refreshes, 200 milliseconds
 * by default. When events keep arriving the most recent display
 * ratio is still delivered once they stop, so no change is dropped.
 *
 * The value must be finite and non-negative; [BackdropHdrScope] throws
 * [IllegalArgumentException] otherwise. Set it once: changing a static
 * composition local recomposes the whole subtree it provides to.
 */
val LocalBackdropHdrRefreshInterval: ProvidableCompositionLocal<Duration> =
    staticCompositionLocalOf { 200.milliseconds }

/**
 * The display HDR/SDR ratio sampled by this scope, and 1 while the
 * display reports no headroom. The scope updates it at most once per
 * [LocalBackdropHdrRefreshInterval]; the system animates the per-window
 * ratio separately.
 */
val LocalBackdropHdrHeadroom: ProvidableCompositionLocal<Float> = compositionLocalOf { 1f }

/**
 * Refreshes Backdrop-owned rendering resources when Android's display
 * HDR headroom changes. The composition and interaction state remain
 * intact. Enable HDR on the Android Window separately. Platforms without
 * the display callback retain their usual rendering behavior. Externally
 * supplied GraphicsLayers and ancestor effects remain owned by their
 * callers.
 */
@Composable
fun BackdropHdrScope(content: @Composable () -> Unit) {
    val interval = LocalBackdropHdrRefreshInterval.current
    require(interval.isFinite() && interval >= Duration.ZERO) { "HDR refresh interval must be finite and nonnegative" }
    val state = rememberBackdropHdrState(interval)
    CompositionLocalProvider(
        LocalBackdropRenderEpoch provides state.epoch,
        LocalBackdropHdrHeadroom provides state.headroom,
        content = content
    )
}

internal data class BackdropHdrState(val epoch: Int = 0, val headroom: Float = 1f)

@Composable
internal expect fun rememberBackdropHdrState(interval: Duration): BackdropHdrState
