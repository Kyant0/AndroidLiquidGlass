package com.kyant.backdrop

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.skiaImageFilter
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.toArgb
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.internal.AmbientHighlightShaderString
import com.kyant.backdrop.internal.LightingShaderString
import com.kyant.backdrop.internal.TintShaderString
import com.kyant.backdrop.internal.colorUniformComponents
import com.kyant.backdrop.internal.setHdrColor
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HdrRenderingTest {
    @Test
    fun floatSurfacePreservesHdrBaseline() {
        assertPixel(floatArrayOf(2f, 2f, 2f, 1f), render(floatArrayOf(2f, 2f, 2f, 1f)) {})
    }

    @Test
    fun vibrancyMatchesExistingMatrixInsideSdrRange() {
        val luminance = 0.213f * 0.4f + 0.715f * 0.3f + 0.072f * 0.2f
        assertPixel(
            floatArrayOf(
                luminance + (0.4f - luminance) * 1.5f,
                luminance + (0.3f - luminance) * 1.5f,
                luminance + (0.2f - luminance) * 1.5f,
                1f
            ), render(floatArrayOf(0.4f, 0.3f, 0.2f, 1f)) { vibrancy() })
    }

    @Test
    fun colorUniformConversionPreservesHdrAndOutOfGamutValues() {
        val hdr = colorUniformComponents(Color(4f, 4f, 4f, colorSpace = ColorSpaces.LinearExtendedSrgb))
        assertTrue(hdr.all { it > 1f })
        val p3 = colorUniformComponents(Color(0f, 1f, 0f, colorSpace = ColorSpaces.DisplayP3))
        assertTrue(p3[0] < 0f)
        assertTrue(p3[1] > 1f)
        assertTrue(p3[2] < 0f)
    }

    @Test
    fun colorUniformIsNotPremultipliedTwice() {
        val shader = RuntimeShader(
            """
            layout(color) uniform float4 color;
            float4 main(float2 coord) { return float4(color.rgb * color.a, color.a); }
        """
        )
        shader.setColorUniform("color", Color(0.8f, 0.4f, 0.2f, 0.5f))
        assertPixel(floatArrayOf(0.4f, 0.2f, 0.1f, 0.5f), renderShader(shader.asSkikoRuntimeShader().makeShader()))
    }

    @Test
    fun nativeSkikoImageFilterCurrentlyClampsHdrWithoutBackdrop() {
        val identity = RuntimeShaderBuilder(
            RuntimeEffect.makeForShader(
                """
            uniform shader content;
            float4 main(float2 p) { return content.eval(p); }
        """
            )
        )
        val source = constantShader(floatArrayOf(2f, 2f, 2f, 1f))
        assertPixel(floatArrayOf(1f, 1f, 1f, 1f), renderShader(source) {
            imageFilter = org.jetbrains.skia.ImageFilter.makeRuntimeShader(identity, "content", null)
        })
    }

    @Test
    fun tintModesMatchNativeSkiaForSdrInputs() {
        val composeModes = listOf(
            BlendMode.Clear, BlendMode.Src, BlendMode.Dst,
            BlendMode.SrcOver, BlendMode.DstOver, BlendMode.SrcIn, BlendMode.DstIn,
            BlendMode.SrcOut, BlendMode.DstOut, BlendMode.SrcAtop, BlendMode.DstAtop,
            BlendMode.Xor, BlendMode.Plus, BlendMode.Modulate, BlendMode.Multiply, BlendMode.Screen
        )
        val nativeModes = listOf(
            org.jetbrains.skia.BlendMode.CLEAR, org.jetbrains.skia.BlendMode.SRC,
            org.jetbrains.skia.BlendMode.DST, org.jetbrains.skia.BlendMode.SRC_OVER,
            org.jetbrains.skia.BlendMode.DST_OVER, org.jetbrains.skia.BlendMode.SRC_IN,
            org.jetbrains.skia.BlendMode.DST_IN, org.jetbrains.skia.BlendMode.SRC_OUT,
            org.jetbrains.skia.BlendMode.DST_OUT, org.jetbrains.skia.BlendMode.SRC_ATOP,
            org.jetbrains.skia.BlendMode.DST_ATOP, org.jetbrains.skia.BlendMode.XOR,
            org.jetbrains.skia.BlendMode.PLUS, org.jetbrains.skia.BlendMode.MODULATE,
            org.jetbrains.skia.BlendMode.MULTIPLY, org.jetbrains.skia.BlendMode.SCREEN
        )
        val tint = Color(0.2f, 0.4f, 0.6f, 0.5f)
        val input = floatArrayOf(0.3f, 0.15f, 0.1f, 0.75f)
        composeModes.indices.forEach { i ->
            // Native Skia represents the identity Dst filter as null, which Compose's
            // constructor cannot wrap; only the constructible modes are tested.
            if (composeModes[i] == BlendMode.Dst) return@forEach
            val actual = renderCached(input, listOf("BackdropTint0" to TintShaderString)) {
                colorFilter(ColorFilter.tint(tint, composeModes[i]))
            }
            org.jetbrains.skia.ColorFilter.makeBlend(tint.toArgb(), nativeModes[i]).use { filter ->
                val expected = renderShader(constantShader(input)) { colorFilter = filter }
                assertPixel(expected, actual)
            }
        }
    }

    @Test
    fun tintKeepsExtendedColorAndPremultipliesOnce() {
        val hdr = Color(2f, 2f, 2f, 0.5f, ColorSpaces.ExtendedSrgb)
        assertPixel(
            floatArrayOf(0.5f, 0.5f, 0.5f, 0.25f), renderCached(
                floatArrayOf(0.25f, 0.25f, 0.25f, 0.5f),
                listOf("BackdropTint0" to TintShaderString)
            ) { colorFilter(ColorFilter.tint(hdr)) })
    }

    @Test
    fun successiveTintsKeepSeparateUniformsAndHdrPlus() {
        assertPixel(
            floatArrayOf(5f, 5f, 5f, 1f), renderCached(
                floatArrayOf(2f, 2f, 2f, 1f),
                listOf("BackdropTint0" to TintShaderString, "BackdropTint1" to TintShaderString)
            ) {
                colorFilter(ColorFilter.tint(Color(1f, 1f, 1f), BlendMode.Plus))
                colorFilter(ColorFilter.tint(Color(2f, 2f, 2f, colorSpace = ColorSpaces.ExtendedSrgb), BlendMode.Plus))
            })
    }

    @Test
    fun ambientHighlightKeepsExtendedRangeSheen() {
        val scope = object : BackdropEffectScopeImpl() {
            override val shape: Shape = RectangleShape
        }
        val runtime = scope.obtainRuntimeShader("Ambient", AmbientHighlightShaderString)
        // The sheen covers the +gradient side of a small shape, so the sampled pixel is inside it.
        runtime.setFloatUniform("size", 4f, 4f)
        runtime.setFloatUniform("cornerRadii", 0f, 0f, 0f, 0f)
        runtime.setColorUniform("color", Color(2f, 2f, 2f, 1f, ColorSpaces.ExtendedSrgb))
        runtime.setFloatUniform("angle", (45f * (PI / 180f)).toFloat())
        runtime.setFloatUniform("falloff", 1f)
        val builder = runtime.asSkikoRuntimeShader()
        builder.child("content", constantShader(floatArrayOf(0f, 0f, 0f, 0f)))
        assertPixel(floatArrayOf(2f, 2f, 2f, 1f), renderShader(builder.makeShader()))
    }

    @Test
    fun lightingPreservesHdrAndSourceAlpha() {
        assertPixel(
            floatArrayOf(1.5f, 1.5f, 1.5f, 0.5f), renderCached(
                floatArrayOf(1f, 1f, 1f, 0.5f),
                listOf("BackdropLighting0" to LightingShaderString)
            ) { colorFilter(ColorFilter.lighting(Color.White, Color.White)) })
    }

    @Test
    fun plainPaintKeepsExtendedRgbAndAlphaAtRendererBoundary() {
        val paint = androidx.compose.ui.graphics.Paint()
        paint.setHdrColor(Color(2f, 2f, 2f, 0.5f, ColorSpaces.ExtendedSrgb))
        val native = paint.skiaPaint.color4f
        assertEquals(2f, native.r, 0.004f)
        assertEquals(2f, native.g, 0.004f)
        assertEquals(2f, native.b, 0.004f)
        assertEquals(0.5f, native.a, 0.004f)
    }

    private fun renderCached(
        input: FloatArray, shaders: List<Pair<String, String>>,
        effects: BackdropEffectScope.() -> Unit
    ): FloatArray {
        val scope = object : BackdropEffectScopeImpl() {
            override val shape: Shape = RectangleShape
        }
        scope.apply(effects)
        var shader = constantShader(input)
        for ((key, source) in shaders) {
            val builder = scope.obtainRuntimeShader(key, source).asSkikoRuntimeShader()
            builder.child("content", shader)
            shader = builder.makeShader()
        }
        return renderShader(shader)
    }

    // Exercise the exact cached matrix shaders populated by colorFilter(), without
    // Skiko's independently clamped ImageFilter intermediate buffers.
    private fun constantShader(input: FloatArray): Shader =
        RuntimeShaderBuilder(
            RuntimeEffect.makeForShader(
                """
            uniform float4 color;
            float4 main(float2 coord) { return color; }
        """
            )
        ).apply { uniform("color", input) }.makeShader()

    private fun render(input: FloatArray, effects: BackdropEffectScope.() -> Unit): FloatArray {
        val scope = object : BackdropEffectScopeImpl() {
            override val shape: Shape = RectangleShape
        }
        scope.apply(effects)
        val shader = RuntimeShaderBuilder(
            RuntimeEffect.makeForShader(
                """
            uniform float4 color;
            float4 main(float2 coord) { return color; }
        """
            )
        ).apply { uniform("color", input) }.makeShader()
        return renderShader(shader) { imageFilter = scope.renderEffect?.skiaImageFilter }
    }

    private fun renderShader(shader: Shader, configure: Paint.() -> Unit = {}): FloatArray {
        val info = ImageInfo(16, 16, ColorType.RGBA_F16, ColorAlphaType.PREMUL, ColorSpace.sRGB)
        Surface.makeRaster(info).use { surface ->
            Paint().use { paint ->
                paint.configure()
                surface.canvas.saveLayer(
                    Canvas.SaveLayerRec(
                        paint = paint,
                        saveLayerFlags = Canvas.SaveLayerFlags(Canvas.SaveLayerFlagsSet.F16ColorType)
                    )
                )
                Paint().use { sourcePaint ->
                    sourcePaint.shader = shader
                    surface.canvas.drawRect(Rect.makeWH(16f, 16f), sourcePaint)
                }
                surface.canvas.restore()
            }
            Bitmap().use { bitmap ->
                // Skiko's RGBA_F32 ordinal currently maps to native RGB_F16F16F16x; recheck when
                // Skiko is updated.
                // Read the correctly mapped RGBA_F16 format, including alpha.
                assertTrue(
                    bitmap.allocPixels(
                        ImageInfo(
                            1,
                            1,
                            ColorType.RGBA_F16,
                            ColorAlphaType.PREMUL,
                            ColorSpace.sRGB
                        )
                    )
                )
                assertTrue(surface.readPixels(bitmap, 8, 8))
                val bytes = requireNotNull(
                    bitmap.readPixels(
                        ImageInfo(1, 1, ColorType.RGBA_F16, ColorAlphaType.PREMUL, ColorSpace.sRGB), 8
                    )
                )
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
                return FloatArray(4) { halfToFloat(buffer.short.toInt() and 0xffff) }
            }
        }
    }

    private fun halfToFloat(bits: Int): Float {
        val sign = if (bits and 0x8000 == 0) 1f else -1f
        val exponent = (bits ushr 10) and 31
        val mantissa = bits and 1023
        return sign * when (exponent) {
            0 -> Math.scalb(mantissa.toFloat(), -24)
            31 -> if (mantissa == 0) Float.POSITIVE_INFINITY else Float.NaN
            else -> Math.scalb(1f + mantissa / 1024f, exponent - 15)
        }
    }

    private fun assertPixel(expected: FloatArray, actual: FloatArray, label: String = "") {
        expected.indices.forEach { index ->
            val message = if (label.isEmpty()) "Channel $index" else "$label channel $index"
            assertEquals(expected[index], actual[index], 0.004f, message)
        }
    }
}
