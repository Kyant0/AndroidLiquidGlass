package com.kyant.liquidglass.refraction

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.lerp
import com.kyant.liquidglass.utils.requirePrecondition

@Suppress("FunctionName")
@Stable
fun RefractionHeight(value: Dp): RefractionHeight.Fixed {
    return RefractionHeight.Fixed(value)
}

@Immutable
sealed interface RefractionHeight : RefractionValue {

    @Immutable
    @JvmInline
    value class Fixed(val value: Dp) : RefractionHeight {

        init {
            requirePrecondition(value.value >= 0f) {
                "Refraction height can't be negative!"
            }
        }

        override fun toPx(size: Size, density: Density): Float {
            return with(density) { value.toPx() }
        }
    }

    @Immutable
    data object Full : RefractionHeight {

        override fun toPx(size: Size, density: Density): Float {
            return size.minDimension
        }
    }

    @Immutable
    data object Half : RefractionHeight {

        override fun toPx(size: Size, density: Density): Float {
            return size.minDimension / 2f
        }
    }

    @Immutable
    data object None : RefractionHeight {

        override fun toPx(size: Size, density: Density): Float {
            return 0f
        }
    }
}

fun lerp(
    start: RefractionHeight,
    stop: RefractionHeight,
    fraction: Float
): RefractionHeight {
    return LerpRefractionHeight(
        start = start,
        stop = stop,
        fraction = fraction
    )
}

private class LerpRefractionHeight(
    val start: RefractionHeight,
    val stop: RefractionHeight,
    val fraction: Float
) : RefractionHeight {

    override fun toPx(size: Size, density: Density): Float {
        val startPx = start.toPx(size, density)
        val stopPx = stop.toPx(size, density)
        return lerp(startPx, stopPx, fraction).fastCoerceAtLeast(0f)
    }
}
