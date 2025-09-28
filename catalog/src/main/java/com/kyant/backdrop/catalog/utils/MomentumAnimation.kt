package com.kyant.backdrop.catalog.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberMomentumAnimation(
    maxScale: Float,
    progressAnimationSpec: FiniteAnimationSpec<Float> =
        spring(1f, 1000f, 0.01f),
    velocityAnimationSpec: FiniteAnimationSpec<Float> =
        spring(0.5f, 250f, 5f),
    scaleXAnimationSpec: FiniteAnimationSpec<Float> =
        spring(0.4f, 400f, 0.01f),
    scaleYAnimationSpec: FiniteAnimationSpec<Float> =
        spring(0.6f, 400f, 0.01f)
): MomentumAnimation {
    val animationScope = rememberCoroutineScope()
    return remember(
        maxScale,
        animationScope,
        progressAnimationSpec,
        velocityAnimationSpec,
        scaleXAnimationSpec,
        scaleYAnimationSpec
    ) {
        MomentumAnimation(
            maxScale = maxScale,
            animationScope = animationScope,
            progressAnimationSpec = progressAnimationSpec,
            velocityAnimationSpec = velocityAnimationSpec,
            scaleXAnimationSpec = scaleXAnimationSpec,
            scaleYAnimationSpec = scaleYAnimationSpec
        )
    }
}

class MomentumAnimation(
    val maxScale: Float,
    private val animationScope: CoroutineScope,
    private val progressAnimationSpec: FiniteAnimationSpec<Float>,
    private val velocityAnimationSpec: FiniteAnimationSpec<Float>,
    private val scaleXAnimationSpec: FiniteAnimationSpec<Float>,
    private val scaleYAnimationSpec: FiniteAnimationSpec<Float>
) {

    private val velocityTracker = VelocityTracker()

    private val progressAnimation = Animatable(0f)
    private val velocityAnimation = Animatable(0f)
    private val scaleXAnimation = Animatable(1f)
    private val scaleYAnimation = Animatable(1f)

    val progress: Float get() = progressAnimation.value
    val velocity: Float get() = velocityAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value

    var isDragging: Boolean by mutableStateOf(false)
        private set

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = {
                isDragging = true
                velocityTracker.resetTracking()
                startPressingAnimation()
            },
            onDragEnd = { change ->
                isDragging = false
                val velocity = velocityTracker.calculateVelocity()
                updateVelocity(velocity)
                velocityTracker.addPointerInputChange(change)
                velocityTracker.resetTracking()
                endPressingAnimation()
                settleVelocity()
            },
            onDragCancel = {
                isDragging = false
                velocityTracker.resetTracking()
                endPressingAnimation()
                settleVelocity()
            }
        ) { change, _ ->
            isDragging = true
            velocityTracker.addPointerInputChange(change)
            val velocity = velocityTracker.calculateVelocity()
            updateVelocity(velocity)
        }
    }

    private fun updateVelocity(velocity: Velocity) {
        animationScope.launch { velocityAnimation.animateTo(velocity.x, velocityAnimationSpec) }
    }

    private fun settleVelocity() {
        animationScope.launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
    }

    fun startPressingAnimation() {
        animationScope.launch {
            launch { progressAnimation.animateTo(1f, progressAnimationSpec) }
            launch { scaleXAnimation.animateTo(maxScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(maxScale, scaleYAnimationSpec) }
        }
    }

    fun endPressingAnimation() {
        animationScope.launch {
            launch { progressAnimation.animateTo(0f, progressAnimationSpec) }
            launch { scaleXAnimation.animateTo(1f, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(1f, scaleYAnimationSpec) }
        }
    }
}
