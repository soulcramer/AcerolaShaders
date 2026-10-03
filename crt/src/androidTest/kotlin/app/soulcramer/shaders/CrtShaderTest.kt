package app.soulcramer.shaders

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class CrtShaderTest {
    @Test
    fun shaderCompiles() {
        RuntimeShader(CrtShader)
    }

    @Test
    fun outsideTheScreenIsOpaqueBlack() {
        val opaque = render(Color.WHITE, curvature = 1f)
        assertEquals(Color.BLACK, opaque.getPixel(0, 0))

        val transparent = render(Color.TRANSPARENT, curvature = 1f)
        assertEquals(Color.BLACK, transparent.getPixel(0, 0))
    }

    @Test
    fun centrePixelAtDefaultSettingsAppliesTheScanlines() {
        val input = Color.argb(255, 100, 120, 140)
        val output = render(input).getPixel(CENTRE, CENTRE)
        assertClose(expectedCentre(input), output)
    }

    @Test
    fun transparentPixelStaysTransparent() {
        val output = render(Color.TRANSPARENT).getPixel(CENTRE, CENTRE)
        assertEquals(0, Color.alpha(output))
    }

    @Test
    fun translucentPixelKeepsItsAlphaAndItsColour() {
        val input = Color.argb(128, 100, 120, 140)
        val output = render(input).getPixel(CENTRE, CENTRE)
        assertClose(expectedCentre(input), output, tolerance = 4)
    }

    @Test
    fun vignetteDarkensTheColourWithoutFadingTheAlpha() {
        val output = render(Color.argb(128, 255, 255, 255)).getPixel(0, CENTRE)
        assertTrue("alpha ${Color.alpha(output)}", abs(Color.alpha(output) - 128) <= TOLERANCE)
        assertTrue("red ${Color.red(output)}", Color.red(output) <= TOLERANCE)
        assertTrue("green ${Color.green(output)}", Color.green(output) <= TOLERANCE)
        assertTrue("blue ${Color.blue(output)}", Color.blue(output) <= TOLERANCE)
    }

    /** Applies the scanline factors of the shader at default settings to the centre pixel. */
    private fun expectedCentre(input: Int): Int {
        val phase = (CENTRE + 0.5f) * 2f / DENSITY
        val green = (sin(phase) + 1f) * 0.15f + 1f
        val redBlue = (cos(phase) + 1f) * 0.135f + 1f
        fun scale(channel: Int, factor: Float): Int = (channel * factor).roundToInt().coerceAtMost(255)
        return Color.argb(
            Color.alpha(input),
            scale(Color.red(input), redBlue),
            scale(Color.green(input), green),
            scale(Color.blue(input), redBlue),
        )
    }

    private fun render(color: Int, curvature: Float = CrtDefaults.CURVATURE): Bitmap {
        val input = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val shader = RuntimeShader(CrtShader).apply {
            setFloatUniform("size", SIZE.toFloat(), SIZE.toFloat())
            setFloatUniform("pixelDensity", DENSITY)
            setFloatUniform("curvature", curvature)
            setFloatUniform("vignetteWidth", CrtDefaults.VIGNETTE_WIDTH)
            setIntUniform("lineSize", CrtDefaults.LINE_SIZE)
            setFloatUniform("lineStrength", CrtDefaults.LINE_STRENGTH)
            setFloatUniform("brightnessAdjust", CrtDefaults.BRIGHTNESS_ADJUST)
            setInputShader("composable", BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        }
        val node = RenderNode("crt").apply { setPosition(0, 0, SIZE, SIZE) }
        node.beginRecording().drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), Paint().apply { this.shader = shader })
        node.endRecording()
        return drawWithHardware(node)
    }

    // Software canvases reject RuntimeShader, so the node is drawn on the GPU into an ImageReader.
    private fun drawWithHardware(node: RenderNode): Bitmap {
        val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        val reader = ImageReader.newInstance(SIZE, SIZE, PixelFormat.RGBA_8888, 1, usage)
        val renderer = HardwareRenderer()
        try {
            renderer.setOpaque(false)
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            reader.acquireNextImage().use { image ->
                val buffer = requireNotNull(image.hardwareBuffer)
                val hardwareBitmap = requireNotNull(
                    Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB)),
                )
                buffer.close()
                return hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
        } finally {
            renderer.destroy()
            reader.close()
        }
    }

    private fun assertClose(expected: Int, actual: Int, tolerance: Int = TOLERANCE) {
        val channels = listOf<(Int) -> Int>(Color::alpha, Color::red, Color::green, Color::blue)
        for (channel in channels) {
            val message = "expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}"
            assertTrue(message, abs(channel(expected) - channel(actual)) <= tolerance)
        }
    }

    private companion object {
        const val SIZE = 64
        const val CENTRE = SIZE / 2
        const val DENSITY = 2f
        const val TOLERANCE = 2
    }
}
