package app.soulcramer.shaders

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class RenderPassChainPrecisionTest {
    // Pass 1 stores red in [0, 1 / 16] in an 8-bit intermediate, and pass 2 scales it back, so the store rounding
    // shows as a 16x larger error.
    @Test
    fun intermediateStoreKeepsTheRampWithinEightSteps() {
        val chain = RenderPassChain(RuntimeShader(RAMP_SHADER), RuntimeShader(SCALE_SHADER))
        val node = RenderNode("chain").apply {
            setPosition(0, 0, WIDTH, HEIGHT)
            setRenderEffect(chain.effect(WIDTH.toFloat(), HEIGHT.toFloat(), Unit) {}.asAndroidRenderEffect())
        }
        val opaque = Paint().apply { color = Color.BLACK }
        node.beginRecording().drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), opaque)
        node.endRecording()

        val bitmap = drawWithHardware(node, WIDTH, HEIGHT)
        val reds = (0 until WIDTH).map { x -> Color.red(bitmap.getPixel(x, ROW)) }
        val distinct = reds.distinct().size
        val maxDifference = reds.withIndex().maxOf { (x, red) ->
            abs(red - (255f * (x + 0.5f) / WIDTH).roundToInt())
        }

        val message = "distinct red values $distinct, maximum difference $maxDifference / 255"
        Log.i(TAG, message)
        assertTrue(message, maxDifference <= MAX_DIFFERENCE)
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

    private companion object {
        const val TAG = "RenderPassChainPrecision"
        const val WIDTH = 256
        const val HEIGHT = 16
        const val ROW = 8
        const val MAX_DIFFERENCE = 8

        const val RAMP_SHADER = """
            uniform float2 size;
            uniform shader composable;

            half4 main(float2 coord) {
                return half4(coord.x / size.x / 16.0, 0.0, 0.0, 1.0);
            }
        """

        const val SCALE_SHADER = """
            uniform float2 size;
            uniform shader composable;

            half4 main(float2 coord) {
                half4 colour = composable.eval(coord);
                return half4(min(colour.r * 16.0, 1.0), 0.0, 0.0, 1.0);
            }
        """
    }
}
