package com.kyant.backdrop

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.kyant.backdrop.internal.EventThrottle
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.time.Duration

@Composable
internal actual fun rememberBackdropHdrState(interval: Duration): BackdropHdrState {
    if (Build.VERSION.SDK_INT < 34) return BackdropHdrState()
    return rememberAndroidHdrState(interval)
}

@RequiresApi(34)
@Composable
private fun rememberAndroidHdrState(interval: Duration): BackdropHdrState {
    val view = LocalView.current
    val state = remember(view) { mutableStateOf(BackdropHdrState()) }
    DisposableEffect(view, interval) {
        val handler = Handler(Looper.getMainLooper())
        val manager = view.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        var active = true
        var observed: Display? = null
        var observedId: Int? = null
        var pendingTask: Runnable? = null
        val throttle = EventThrottle<Pair<Int?, Float>>(
            interval.inWholeMilliseconds, SystemClock::uptimeMillis,
            schedule = { delay, block ->
                val runnable = Runnable(block)
                pendingTask = runnable
                handler.postDelayed(runnable, delay)
            },
            cancelScheduled = {
                pendingTask?.let(handler::removeCallbacks)
                pendingTask = null
            },
        ) { (displayId, ratio) ->
            if (active && observedId == displayId) {
                val previous = state.value
                if (previous.headroom != ratio) state.value = BackdropHdrState(previous.epoch + 1, ratio)
            }
        }
        val listener = Consumer<Display> { display ->
            if (active && display.displayId == observedId) {
                val ratio = if (display.isHdrSdrRatioAvailable) display.hdrSdrRatio else 1f
                if (ratio.isFinite() && ratio >= 1f) throttle.offer(display.displayId to ratio)
            }
        }
        val executor = Executor { command -> handler.post { if (active) command.run() } }
        fun detachDisplay() {
            observed?.unregisterHdrSdrRatioChangedListener(listener)
            observed = null
            observedId = null
            throttle.cancel()
        }

        fun attachDisplay() {
            val display = if (view.isAttachedToWindow) view.display else null
            if (display != null && display.displayId == observedId && observed != null &&
                display.isValid && display.isHdrSdrRatioAvailable
            ) return
            detachDisplay()
            observedId = display?.displayId
            if (display != null && display.isValid && display.isHdrSdrRatioAvailable) {
                observed = display
                try {
                    display.registerHdrSdrRatioChangedListener(executor, listener)
                    listener.accept(display)
                } catch (exception: IllegalStateException) {
                    // Availability can change between checking the display and registering.
                    if (display.isValid && display.isHdrSdrRatioAvailable) throw exception
                    observed = null
                    throttle.offer(observedId to 1f)
                }
            } else throttle.offer(observedId to 1f)
        }

        val attachment = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = attachDisplay()
            override fun onViewDetachedFromWindow(v: View) {
                detachDisplay()
            }
        }
        val displays = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = attachDisplay()
            override fun onDisplayRemoved(displayId: Int) = attachDisplay()
            override fun onDisplayChanged(displayId: Int) = attachDisplay()
        }
        view.addOnAttachStateChangeListener(attachment)
        manager.registerDisplayListener(displays, handler)
        attachDisplay()
        onDispose {
            active = false
            view.removeOnAttachStateChangeListener(attachment)
            manager.unregisterDisplayListener(displays)
            detachDisplay()
            handler.removeCallbacksAndMessages(null)
        }
    }
    return state.value
}
