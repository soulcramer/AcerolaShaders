package app.soulcramer.shaders

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
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
import androidx.compose.ui.graphics.Color as ComposeColor

@RunWith(AndroidJUnit4::class)
class PaletteSwapShaderTest {
    @Test
    fun shaderCompiles() {
        RuntimeShader(PaletteSwapShader)
    }

    @Test
    fun defaultsMapBlackToBlackAndWhiteToTheLastGrey() {
        assertPixel(renderSolid(Color.BLACK), red = 0f, green = 0f, blue = 0f)
        assertPixel(renderSolid(Color.WHITE), red = 0.3f, green = 0.3f, blue = 0.3f)
    }

    @Test
    fun fourColoursSplitAtAQuarter() {
        assertEntry(renderSolid(Color.rgb(0x3E, 0x3E, 0x3E), FOUR_COLOURS), FOUR_COLOURS[0])
        assertEntry(renderSolid(Color.rgb(0x42, 0x42, 0x42), FOUR_COLOURS), FOUR_COLOURS[1])
    }

    @Test
    fun sixteenColoursMapEveryBand() {
        val input = Bitmap.createBitmap(SIXTEEN_COLOURS.size * COLUMN_WIDTH, SIZE, Bitmap.Config.ARGB_8888)
        for (x in 0 until input.width) {
            // Grey 16 * i + 8 is near the middle of band i, at least 6 in 255 from each band edge.
            val grey = 16 * (x / COLUMN_WIDTH) + 8
            for (y in 0 until SIZE) input.setPixel(x, y, Color.rgb(grey, grey, grey))
        }
        val output = render(input, SIXTEEN_COLOURS, PaletteSwapIndex.Luminance)
        for (i in SIXTEEN_COLOURS.indices) {
            assertEntry(premultipliedPixel(output, i * COLUMN_WIDTH + COLUMN_WIDTH / 2, CENTRE), SIXTEEN_COLOURS[i])
        }
    }

    @Test
    fun redModeReadsTheRedChannel() {
        assertEntry(renderSolid(Color.RED, FOUR_COLOURS, PaletteSwapIndex.Red), FOUR_COLOURS[3])
        assertEntry(renderSolid(Color.RED, FOUR_COLOURS, PaletteSwapIndex.Luminance), FOUR_COLOURS[0])
    }

    @Test
    fun transparentPixelStaysTransparent() {
        assertEquals(Rgba(red = 0, green = 0, blue = 0, alpha = 0), renderSolid(Color.TRANSPARENT, FOUR_COLOURS))
    }

    @Test
    fun halfAlphaWhiteRendersTheLastEntryAtHalfAlpha() {
        assertEntry(renderSolid(HALF_WHITE, FOUR_COLOURS), FOUR_COLOURS[3], alpha = 127.5f)
    }

    private data class Rgba(val red: Int, val green: Int, val blue: Int, val alpha: Int)

    private fun solid(color: Int): Bitmap =
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun renderSolid(
        color: Int,
        palette: List<ComposeColor> = PaletteSwapDefaults.PALETTE,
        index: PaletteSwapIndex = PaletteSwapDefaults.INDEX,
    ): Rgba = premultipliedPixel(render(solid(color), palette, index), CENTRE, CENTRE)

    // Sets the uniforms as PaletteSwapNode does, so that the test covers the packing of the palette array.
    private fun render(input: Bitmap, palette: List<ComposeColor>, index: PaletteSwapIndex): Bitmap {
        val shader = RuntimeShader(PaletteSwapShader).apply { setPaletteSwapUniforms(palette, index) }
        val node = RenderNode("paletteSwap").apply {
            setPosition(0, 0, input.width, input.height)
            setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "composable"))
        }
        node.beginRecording().drawBitmap(input, 0f, 0f, null)
        node.endRecording()
        return drawWithHardware(node, input.width, input.height)
    }

    // Software canvases reject RuntimeShader, so draw the node on the GPU into an ImageReader.
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

    private fun assertEntry(actual: Rgba, entry: ComposeColor, alpha: Float = 255f) {
        assertPixel(actual, entry.red, entry.green, entry.blue, alpha)
    }

    // The shader premultiplies the entry, so each colour channel is the component times the stored alpha.
    private fun assertPixel(actual: Rgba, red: Float, green: Float, blue: Float, alpha: Float = 255f) {
        val message = "expected ($red, $green, $blue) at alpha $alpha, got $actual"
        assertTrue(message, abs(actual.alpha - alpha) <= TOLERANCE)
        assertTrue(message, abs(actual.red - red * actual.alpha) <= TOLERANCE)
        assertTrue(message, abs(actual.green - green * actual.alpha) <= TOLERANCE)
        assertTrue(message, abs(actual.blue - blue * actual.alpha) <= TOLERANCE)
    }

    private companion object {
        const val SIZE = 64
        const val CENTRE = SIZE / 2
        const val COLUMN_WIDTH = 4
        const val TOLERANCE = 1
        val HALF_WHITE = Color.argb(128, 255, 255, 255)

        // Each component is 0 or 1, and no entry is a grey or red, so a shader that returns its input fails.
        val FOUR_COLOURS = listOf(ComposeColor.Blue, ComposeColor.Green, ComposeColor.Yellow, ComposeColor.Magenta)

        // Entry i has a red byte of 17 * i and a green of 0, so each entry is different and no entry is a grey.
        val SIXTEEN_COLOURS = List(16) { i -> ComposeColor(i / 15f, 0f, 1f - i / 15f) }
    }
}
