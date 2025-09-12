package com.kyant.liquidglass.dispersion

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

@Immutable
sealed interface Dispersion {

    @Immutable
    data object None : Dispersion

    @Immutable
    data object Automatic : Dispersion

    @Immutable
    data class Fractional(
        @param:FloatRange(from = 0.0, to = 1.0) val fraction: Float
    ) : Dispersion
}
