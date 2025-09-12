package com.kyant.liquidglass.dispersion

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

/**
 * The dispersion effect applied to the liquid glass.
 */
@Immutable
sealed interface Dispersion {

    /**
     * No dispersion effect.
     */
    @Immutable
    data object None : Dispersion

    /**
     * Automatic dispersion effect, which adapts to the inner refraction options.
     */
    @Immutable
    data object Automatic : Dispersion

    /**
     * A fractional dispersion effect, which disperses the glass by a fraction of its inner refraction options.
     *
     * @param fraction
     * The fraction of the inner refraction options to disperse the glass by.
     */
    @Immutable
    data class Fractional(
        @param:FloatRange(from = 0.0, to = 1.0) val fraction: Float
    ) : Dispersion
}
