package com.kyant.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.dispersion
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.rememberBackdrop
import com.kyant.backdrop.shadow.Shadow
import com.kyant.liquidglass.dispersion.Dispersion
import com.kyant.liquidglass.highlight.GlassHighlightStyle

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop
): Modifier =
    this.liquidGlass(
        backdrop = backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = onDrawBackdrop,
        style = { style }
    )

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    style: () -> GlassStyle
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { style().shape },
            highlight = {
                style().highlight?.let {
                    Highlight(
                        width = it.width,
                        color = it.color,
                        blendMode = it.blendMode
                    ) {
                        when (it.style) {
                            is GlassHighlightStyle.Solid -> HighlightStyle.Solid
                            is GlassHighlightStyle.Soft -> HighlightStyle.Solid
                            is GlassHighlightStyle.Dynamic ->
                                HighlightStyle.Dynamic(
                                    angle = it.style.angle,
                                    falloff = it.style.falloff
                                )

                            else -> HighlightStyle.Default
                        }
                    }
                }
            },
            shadow = {
                style().shadow?.run {
                    Shadow(
                        elevation = elevation,
                        color = color,
                        offset = offset,
                        blendMode = blendMode
                    )
                }
            },
            onDrawBackdrop = onDrawBackdrop,
            onDrawSurface = {
                style().material.run {
                    if (brush != null) {
                        drawRect(
                            brush = brush,
                            alpha = alpha,
                            blendMode = blendMode
                        )
                    }
                }
            }
        ) {
            val style = style()

            style.material.colorFilter?.let { colorFilter(it) }

            blur(style.material.blurRadius.toPx())

            val refractionHeight = style.innerRefraction.height.toPx(size, this)
            val refractionAmount = -style.innerRefraction.amount.toPx(size, this)
            refraction(
                height = refractionHeight,
                amount = refractionAmount,
                hasDepthEffect = style.innerRefraction.depthEffect > 0f
            )

            val dispersion = style.dispersion
            val dispersionHeight: Float
            val dispersionAmount: Float
            when (dispersion) {
                Dispersion.None -> {
                    dispersionHeight = 0f
                    dispersionAmount = 0f
                }

                Dispersion.Automatic -> {
                    dispersionHeight = refractionHeight
                    dispersionAmount = refractionAmount
                }

                is Dispersion.Fractional -> {
                    dispersionHeight = refractionHeight * dispersion.fraction
                    dispersionAmount = refractionAmount * dispersion.fraction
                }
            }
            dispersion(
                height = dispersionHeight,
                amount = dispersionAmount
            )
        }

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null
): Modifier =
    this.liquidGlass(
        backdrop = state.backdrop,
        style = style,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = { drawBackdrop ->
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        }
    )

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this.liquidGlass(
        backdrop = state.backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = { drawBackdrop ->
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        },
        style = style
    )

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlassProvider(state: LiquidGlassProviderState): Modifier =
    this.backdrop(state.backdrop)

@Deprecated("[Backdrop API] Use `rememberBackdrop` instead")
@Composable
fun rememberLiquidGlassProviderState(backgroundColor: Color?): LiquidGlassProviderState {
    return LiquidGlassProviderState(
        backgroundColor,
        rememberBackdrop {
            backgroundColor?.let { drawRect(it) }
            drawContent()
        }
    )
}

@Deprecated("[Backdrop API] Use `Backdrop` instead")
@Stable
class LiquidGlassProviderState internal constructor(
    val backgroundColor: Color?,
    internal val backdrop: Backdrop
)
