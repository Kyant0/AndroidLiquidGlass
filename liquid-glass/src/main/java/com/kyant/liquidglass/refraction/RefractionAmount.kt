package com.kyant.liquidglass.refraction

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.liquidglass.utils.requirePrecondition

@Suppress("FunctionName")
@Stable
fun RefractionAmount(value: Dp): RefractionAmount.Fixed {
    return RefractionAmount.Fixed(value)
}

@Immutable
sealed interface RefractionAmount : RefractionValue {

    @Immutable
    @JvmInline
    value class Fixed(val value: Dp) : RefractionAmount {

        init {
            requirePrecondition(value.value <= 0f) {
                "Refraction amount can't be positive!"
            }
        }

        override fun toPx(size: Size, density: Density): Float {
            return with(density) { value.toPx() }
        }
    }

    @Immutable
    data object Full : RefractionAmount {

        override fun toPx(size: Size, density: Density): Float {
            return -size.minDimension
        }
    }

    @Immutable
    data object Half : RefractionAmount {

        override fun toPx(size: Size, density: Density): Float {
            return -size.minDimension / 2f
        }
    }

    @Immutable
    data object None : RefractionAmount {

        override fun toPx(size: Size, density: Density): Float {
            return 0f
        }
    }
}

fun lerp(
    start: RefractionAmount,
    stop: RefractionAmount,
    fraction: Float
): RefractionAmount {
    return LerpRefractionAmount(
        start = start,
        stop = stop,
        fraction = fraction
    )
}

private class LerpRefractionAmount(
    val start: RefractionAmount,
    val stop: RefractionAmount,
    val fraction: Float
) : RefractionAmount {

    override fun toPx(size: Size, density: Density): Float {
        val startPx = start.toPx(size, density)
        val stopPx = stop.toPx(size, density)
        return lerp(startPx, stopPx, fraction).fastCoerceAtMost(0f)
    }
}
