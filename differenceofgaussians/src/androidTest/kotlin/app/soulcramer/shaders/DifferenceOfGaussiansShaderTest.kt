package app.soulcramer.shaders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class DifferenceOfGaussiansShaderTest {
    @Test
    fun blurPassCompiles() {
        RuntimeShader(DifferenceOfGaussiansBlurShader)
    }

    @Test
    fun thresholdPassCompiles() {
        RuntimeShader(DifferenceOfGaussiansThresholdShader)
    }

    @Test
    fun flatGreyIsBlack() {
        val output = render(solid(Color.rgb(128, 128, 128)))
        assertEveryPixel(output, Color.BLACK)
    }

    @Test
    fun flatGreyIsWhiteWithInvert() {
        val output = render(solid(Color.rgb(128, 128, 128)), Uniforms(invert = true))
        assertEveryPixel(output, Color.WHITE)
    }

    @Test
    fun blackSquareOnWhiteHasAWhiteLineOutside() {
        val input = solid(Color.WHITE)
        Canvas(input).drawRect(24f, 24f, 40f, 40f, Paint().apply { color = Color.BLACK })
        val output = render(input)
        assertPixel(Color.WHITE, output, 22, 32)
        assertPixel(Color.BLACK, output, 8, 8)
        assertPixel(Color.BLACK, output, 32, 32)
    }

    @Test
    fun squaredGreyRampHasNoWhitePixel() {
        val input = Bitmap.createBitmap(RAMP_WIDTH, SIZE, Bitmap.Config.ARGB_8888)
        for (x in 0 until RAMP_WIDTH) {
            val t = x / 255f
            val grey = (255f * t * t).roundToInt()
            for (y in 0 until SIZE) input.setPixel(x, y, Color.rgb(grey, grey, grey))
        }
        val output = render(input)
        for (y in 0 until SIZE) {
            for (x in 10..245) {
                val pixel = output.getPixel(x, y)
                assertTrue("($x, $y) is ${Integer.toHexString(pixel)}", Color.red(pixel) < 128)
            }
        }
    }

    @Test
    fun transparentPixelStaysTransparent() {
        val output = render(solid(Color.TRANSPARENT)).getPixel(CENTRE, CENTRE)
        assertEquals(0, Color.alpha(output))
    }

    @Test
    fun halfAlphaWhiteKeepsItsAlpha() {
        val output = premultipliedPixel(render(solid(HALF_WHITE)), CENTRE, CENTRE)
        assertTrue("alpha ${output.alpha}", abs(output.alpha - 127.5f) <= 1f)
    }

    @Test
    fun halfAlphaWhiteWithInvertHasColourChannelsEqualToItsAlpha() {
        val output = premultipliedPixel(render(solid(HALF_WHITE), Uniforms(invert = true)), CENTRE, CENTRE)
        assertTrue("alpha ${output.alpha}", abs(output.alpha - 127.5f) <= 1f)
        assertEquals("red", output.alpha, output.red)
        assertEquals("green", output.alpha, output.green)
        assertEquals("blue", output.alpha, output.blue)
    }

    private data class Uniforms(
        val kernelRadius: Int = DifferenceOfGaussiansDefaults.KERNEL_RADIUS,
        val sigma: Float = DifferenceOfGaussiansDefaults.SIGMA,
        val sigmaScale: Float = DifferenceOfGaussiansDefaults.SIGMA_SCALE,
        val tau: Float = DifferenceOfGaussiansDefaults.TAU,
        val thresholding: Boolean = DifferenceOfGaussiansDefaults.THRESHOLDING,
        val tanh: Boolean = DifferenceOfGaussiansDefaults.TANH,
        val phi: Float = DifferenceOfGaussiansDefaults.PHI,
        val threshold: Float = DifferenceOfGaussiansDefaults.THRESHOLD,
        val invert: Boolean = DifferenceOfGaussiansDefaults.INVERT,
    )

    private data class Rgba(val red: Int, val green: Int, val blue: Int, val alpha: Int)

    private fun solid(color: Int): Bitmap =
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    // Builds the chain as DifferenceOfGaussiansNode does, so that the test covers the 8-bit store between the passes.
    private fun render(input: Bitmap, uniforms: Uniforms = Uniforms()): Bitmap {
        val chain = RenderPassChain(
            RuntimeShader(DifferenceOfGaussiansBlurShader),
            RuntimeShader(DifferenceOfGaussiansThresholdShader),
        )
        val effect = chain.effect(input.width.toFloat(), input.height.toFloat(), uniforms) { passes ->
            for (pass in passes) {
                pass.setIntUniform("kernelRadius", uniforms.kernelRadius)
                pass.setFloatUniform("sigma", uniforms.sigma)
                pass.setFloatUniform("sigmaScale", uniforms.sigmaScale)
            }
            val dog = passes[1]
            dog.setFloatUniform("tau", uniforms.tau)
            dog.setIntUniform("thresholding", if (uniforms.thresholding) 1 else 0)
            dog.setIntUniform("useTanh", if (uniforms.tanh) 1 else 0)
            dog.setFloatUniform("phi", uniforms.phi)
            dog.setFloatUniform("threshold", uniforms.threshold)
            dog.setIntUniform("invert", if (uniforms.invert) 1 else 0)
        }
        val node = RenderNode("differenceOfGaussians").apply {
            setPosition(0, 0, input.width, input.height)
            setRenderEffect(effect.asAndroidRenderEffect())
        }
        node.beginRecording().drawBitmap(input, 0f, 0f, null)
        node.endRecording()
        return drawWithHardware(node, input.width, input.height)
    }

    // Software canvases reject RuntimeShader, so the node is drawn on the GPU into an ImageReader.
    private fun drawWithHardware(node: RenderNode, width: Int, height: Int): Bitmap {
        val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1, usage)
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

    // Bitmap.getPixel unpremultiplies, so read the stored bytes to compare the colour channels with the alpha.
    private fun premultipliedPixel(bitmap: Bitmap, x: Int, y: Int): Rgba {
        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        val offset = y * bitmap.rowBytes + x * 4
        fun byte(index: Int): Int = buffer.get(offset + index).toInt() and 0xFF
        return Rgba(red = byte(0), green = byte(1), blue = byte(2), alpha = byte(3))
    }

    private fun assertEveryPixel(bitmap: Bitmap, expected: Int) {
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) assertPixel(expected, bitmap, x, y)
        }
    }

    private fun assertPixel(expected: Int, bitmap: Bitmap, x: Int, y: Int) {
        val actual = bitmap.getPixel(x, y)
        val message = "($x, $y): expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}"
        assertEquals(message, expected, actual)
    }

    private companion object {
        const val SIZE = 64
        const val CENTRE = SIZE / 2
        const val RAMP_WIDTH = 256
        val HALF_WHITE = Color.argb(128, 255, 255, 255)
    }
}
