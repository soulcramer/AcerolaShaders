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

@RunWith(AndroidJUnit4::class)
class ColorBlindnessShaderTest {
    @Test
    fun severityZeroKeepsTheColour() {
        val input = Color.argb(255, 200, 120, 40)
        for (type in ColorBlindnessType.entries) {
            assertClose(input, render(input, type, severity = 0f))
        }
    }

    @Test
    fun transparentPixelStaysTransparent() {
        for (type in ColorBlindnessType.entries) {
            assertEquals(0, Color.alpha(render(Color.TRANSPARENT, type, severity = 1f)))
        }
    }

    @Test
    fun translucentPixelKeepsItsAlpha() {
        val output = render(Color.argb(128, 255, 0, 0), ColorBlindnessType.Protanomaly, severity = 1f)
        assertTrue(abs(Color.alpha(output) - 128) <= TOLERANCE)
    }

    @Test
    fun protanomalyMovesRedTowardsGreen() {
        assertRedMovesTowardsGreen(ColorBlindnessType.Protanomaly)
    }

    @Test
    fun deuteranomalyMovesRedTowardsGreen() {
        assertRedMovesTowardsGreen(ColorBlindnessType.Deuteranomaly)
    }

    @Test
    fun tritanomalyMovesBlueTowardsGreen() {
        val output = render(Color.BLUE, ColorBlindnessType.Tritanomaly, severity = 1f)
        assertTrue("blue ${Color.blue(output)}", Color.blue(output) < 255 - TOLERANCE)
        assertTrue("green ${Color.green(output)}", Color.green(output) > TOLERANCE)
    }

    private fun assertRedMovesTowardsGreen(type: ColorBlindnessType) {
        val output = render(Color.RED, type, severity = 1f)
        assertTrue("red ${Color.red(output)}", Color.red(output) < 255 - TOLERANCE)
        assertTrue("green ${Color.green(output)}", Color.green(output) > TOLERANCE)
    }

    private fun render(color: Int, type: ColorBlindnessType, severity: Float): Int {
        val input = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, color) }
        val shader = RuntimeShader(ColorBlindnessShader).apply {
            setFloatUniform("severity", severity)
            setIntUniform("colorblindType", type.code)
            setInputShader("composable", BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        }
        val node = RenderNode("colorBlindness").apply { setPosition(0, 0, 1, 1) }
        node.beginRecording().drawRect(0f, 0f, 1f, 1f, Paint().apply { this.shader = shader })
        node.endRecording()
        return drawWithHardware(node)
    }

    // Software canvases reject RuntimeShader, so the node is drawn on the GPU into an ImageReader.
    private fun drawWithHardware(node: RenderNode): Int {
        val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        val reader = ImageReader.newInstance(1, 1, PixelFormat.RGBA_8888, 1, usage)
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
                return hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false).getPixel(0, 0)
            }
        } finally {
            renderer.destroy()
            reader.close()
        }
    }

    private fun assertClose(expected: Int, actual: Int) {
        val channels = listOf<(Int) -> Int>(Color::alpha, Color::red, Color::green, Color::blue)
        for (channel in channels) {
            val message = "expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}"
            assertTrue(message, abs(channel(expected) - channel(actual)) <= TOLERANCE)
        }
    }

    private companion object {
        const val TOLERANCE = 2
    }
}
