package app.soulcramer.shaders

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteSwapDefaultsTest {
    @Test
    fun upstreamDefaultsMatchAcerolaFx() {
        assertEquals(
            listOf(Color(0f, 0f, 0f), Color(0.1f, 0.1f, 0.1f), Color(0.2f, 0.2f, 0.2f), Color(0.3f, 0.3f, 0.3f)),
            PaletteSwapDefaults.PALETTE,
        )
        assertEquals(PaletteSwapIndex.Luminance, PaletteSwapDefaults.INDEX)
        assertEquals(4, PaletteSwapDefaults.COLOR_COUNT)
        assertEquals(0, PaletteSwapDefaults.SEED)
        assertEquals(PaletteSwapHueMode.Monochromatic, PaletteSwapDefaults.HUE_MODE)
        assertEquals(0f, PaletteSwapDefaults.HUE_CONTRAST.start, 0f)
        assertEquals(1f, PaletteSwapDefaults.HUE_CONTRAST.endInclusive, 0f)
    }

    @Test
    fun generatorRangesKeepTheVideoPalette() {
        assertEquals(0.2f, PaletteSwapDefaults.LUMINANCE.start, 0f)
        assertEquals(0.3f, PaletteSwapDefaults.LUMINANCE.endInclusive, 0f)
        assertEquals(0.5f, PaletteSwapDefaults.LUMINANCE_CONTRAST.start, 0f)
        assertEquals(0.6f, PaletteSwapDefaults.LUMINANCE_CONTRAST.endInclusive, 0f)
        assertEquals(0.12f, PaletteSwapDefaults.CHROMA.start, 0f)
        assertEquals(0.20f, PaletteSwapDefaults.CHROMA.endInclusive, 0f)
        assertEquals(0f, PaletteSwapDefaults.CHROMA_CONTRAST.start, 0f)
        assertEquals(0f, PaletteSwapDefaults.CHROMA_CONTRAST.endInclusive, 0f)
    }
}
