package app.soulcramer.shaders

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class PaletteSwapPaletteTest {
    @Test
    fun hashOfZeroMatchesAcerolaFx() {
        // ReShade keeps the low 32 bits of the upstream constant 0x1376312589U, that is 0x76312589 or 1982932361.
        assertEquals(1982932361f / 2147483647f, hash(0), 0f)
    }

    @Test
    fun countSetsTheNumberOfColours() {
        assertEquals(3, randomPalette(count = 3).size)
        assertEquals(4, randomPalette(count = 4).size)
        assertEquals(16, randomPalette(count = 16).size)
    }

    @Test
    fun countOutsideThreeToSixteenIsClamped() {
        assertEquals(3, randomPalette(count = 2).size)
        assertEquals(16, randomPalette(count = 17).size)
    }

    @Test
    fun seedOutsideZeroToOneBillionIsClamped() {
        assertEquals(randomPalette(seed = 0), randomPalette(seed = -1))
        assertEquals(randomPalette(seed = 1_000_000_000), randomPalette(seed = 1_000_000_001))
    }

    @Test
    fun sameArgumentsGiveEqualPalettes() {
        assertEquals(
            randomPalette(seed = 12_345, count = 8, hueMode = PaletteSwapHueMode.TriadicComplementary),
            randomPalette(seed = 12_345, count = 8, hueMode = PaletteSwapHueMode.TriadicComplementary),
        )
    }

    @Test
    fun everyChannelIsFromZeroToOne() {
        for (seed in 0..99) {
            for (hueMode in PaletteSwapHueMode.entries) {
                for (count in listOf(3, 8, 16)) {
                    val palette = randomPalette(seed = seed, count = count, hueMode = hueMode)
                    val inRange = palette.all { it.red in 0f..1f && it.green in 0f..1f && it.blue in 0f..1f }
                    assertTrue("seed $seed, $hueMode, count $count: $palette", inRange)
                }
            }
        }
    }

    @Test
    fun defaultPaletteGoesFromDarkToLight() {
        for (seed in 0..20) {
            for (hueMode in PaletteSwapHueMode.entries) {
                for (count in listOf(4, 8, 16)) {
                    val lightness = randomPalette(seed = seed, count = count, hueMode = hueMode)
                        .map { it.convert(ColorSpaces.Oklab).red }
                    val ordered = lightness.zipWithNext().all { (dark, light) -> light - dark >= 0.02f }
                    assertTrue("seed $seed, $hueMode, count $count: $lightness", ordered)
                }
            }
        }
    }

    @Test
    fun defaultPaletteKeepsTheRequestedHue() {
        for (seed in 0..20) {
            for (hueMode in PaletteSwapHueMode.entries) {
                for (count in listOf(4, 8, 16)) {
                    randomPalette(seed = seed, count = count, hueMode = hueMode).forEachIndexed { i, color ->
                        val oklab = color.convert(ColorSpaces.Oklab)
                        if (hypot(oklab.green, oklab.blue) > 0.02f) {
                            val t = i.toFloat() / (count - 1)
                            val requested = hash(seed) * 2f * PI.toFloat() +
                                (hash(seed + 2) * t * 2f * PI.toFloat() + PI.toFloat() / 4f) * hueMode.factor
                            val difference = Math.toDegrees((atan2(oklab.blue, oklab.green) - requested).toDouble())
                            val error = abs((difference + 180.0).mod(360.0) - 180.0)
                            assertTrue("seed $seed, $hueMode, count $count, colour $i: $error", error <= 2.0)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun colourOutsideSrgbKeepsItsLightness() {
        val color = randomPalette(
            count = 3,
            luminance = 0.6f..0.6f,
            luminanceContrast = 0f..0f,
            chroma = 0.4f..0.4f,
        ).first()
        assertTrue("$color", color.red in 0f..1f && color.green in 0f..1f && color.blue in 0f..1f)
        assertEquals(0.6f, color.convert(ColorSpaces.Oklab).red, 0.01f)
    }

    @Test
    fun hueModeFactorsMatchAcerolaFx() {
        assertEquals(0f, PaletteSwapHueMode.Monochromatic.factor, 0f)
        assertEquals(0.25f, PaletteSwapHueMode.Analogous.factor, 0f)
        assertEquals(0.33f, PaletteSwapHueMode.Complementary.factor, 0f)
        assertEquals(0.66f, PaletteSwapHueMode.TriadicComplementary.factor, 0f)
        assertEquals(0.75f, PaletteSwapHueMode.TetradicComplementary.factor, 0f)
    }

    @Test
    fun oklchPaletteCountSetsTheNumberOfColours() {
        assertEquals(3, oklchPalette(hue = 0.1f, chroma = 0.15f, count = 3).size)
        assertEquals(16, oklchPalette(hue = 0.1f, chroma = 0.15f, count = 16).size)
    }

    @Test
    fun oklchPaletteCountOutsideThreeToSixteenIsClamped() {
        assertEquals(3, oklchPalette(hue = 0.1f, chroma = 0.15f, count = 2).size)
        assertEquals(16, oklchPalette(hue = 0.1f, chroma = 0.15f, count = 17).size)
    }

    @Test
    fun oklchPaletteSameArgumentsGiveEqualPalettes() {
        assertEquals(
            oklchPalette(hue = 0.6f, chroma = 0.1f, count = 8, hueMode = PaletteSwapHueMode.Analogous),
            oklchPalette(hue = 0.6f, chroma = 0.1f, count = 8, hueMode = PaletteSwapHueMode.Analogous),
        )
    }

    @Test
    fun oklchPaletteHueWraps() {
        assertEquals(oklchPalette(hue = 0.25f, chroma = 0.15f), oklchPalette(hue = 1.25f, chroma = 0.15f))
    }

    @Test
    fun oklchPaletteGoesFromDarkToLight() {
        for (hueMode in PaletteSwapHueMode.entries) {
            val lightness = oklchPalette(hue = 0.4f, chroma = 0.15f, count = 8, hueMode = hueMode)
                .map { it.convert(ColorSpaces.Oklab).red }
            assertTrue("$hueMode: $lightness", lightness.zipWithNext().all { (dark, light) -> light > dark })
        }
    }

    @Test
    fun oklchPaletteCentresTheBaseHueInEightColours() {
        val expected = mapOf(
            PaletteSwapHueMode.Monochromatic to listOf(25, 25, 25, 25, 25, 25, 25, 25),
            PaletteSwapHueMode.Analogous to listOf(55, 55, 55, 25, 25, 355, 355, 355),
            PaletteSwapHueMode.Complementary to listOf(205, 205, 25, 25, 25, 25, 205, 205),
            PaletteSwapHueMode.TriadicComplementary to listOf(145, 145, 145, 25, 25, 265, 265, 265),
            PaletteSwapHueMode.TetradicComplementary to listOf(205, 115, 115, 25, 25, 295, 295, 205),
        )
        for ((hueMode, hues) in expected) {
            val measured = oklchPalette(hue = 25f / 360f, chroma = 0.1f, count = 8, hueMode = hueMode).map(::hueDegrees)
            val matches = measured.zip(hues).all { (degrees, hue) ->
                abs(hueDifference(degrees, hue.toDouble())) <= 3.0
            }
            assertTrue("$hueMode: $measured", matches)
        }
    }

    @Test
    fun oklchPaletteHuesAreSymmetricAboutTheMiddle() {
        for (hueMode in PaletteSwapHueMode.entries) {
            for (count in 3..16) {
                // A constant lightness keeps the chroma high at both ends, so the measured hues are accurate.
                val offsets = oklchPalette(
                    hue = 25f / 360f,
                    chroma = 0.1f,
                    count = count,
                    hueMode = hueMode,
                    luminance = 0.6f,
                    luminanceContrast = 0f,
                ).map { hueDifference(hueDegrees(it), 25.0) }
                val symmetric = offsets.indices.all { i ->
                    val mirror = offsets[count - 1 - i]
                    abs(hueDifference(offsets[i] + mirror, 0.0)) <= 3.0
                }
                assertTrue("$hueMode, count $count: $offsets", symmetric)
            }
        }
    }

    @Test
    fun oklchPaletteWithZeroChromaGivesGreys() {
        oklchPalette(hue = 0.3f, chroma = 0f, count = 8).forEach { color ->
            assertEquals("$color", color.red, color.green, 1f / 255f)
            assertEquals("$color", color.red, color.blue, 1f / 255f)
        }
    }

    @Test
    fun oklchPaletteClampsNegativeChroma() {
        assertEquals(oklchPalette(hue = 0.3f, chroma = 0f), oklchPalette(hue = 0.3f, chroma = -0.1f))
    }

    @Test
    fun paletteSwapRejectsAnEmptyPalette() {
        assertThrows(IllegalArgumentException::class.java) { Modifier.paletteSwap(emptyList()) }
    }

    @Test
    fun paletteSwapAcceptsOneColour() {
        Modifier.paletteSwap(listOf(Color.Black))
    }

    @Test
    fun paletteSwapAcceptsSixteenColours() {
        Modifier.paletteSwap(List(16) { Color.Black })
    }

    @Test
    fun paletteSwapRejectsSeventeenColours() {
        assertThrows(IllegalArgumentException::class.java) { Modifier.paletteSwap(List(17) { Color.Black }) }
    }

    // The gamut reduction keeps the hue, so the measured hue has only the error of 8-bit channels.
    private fun hueDegrees(color: Color): Double {
        val oklab = color.convert(ColorSpaces.Oklab)
        return Math.toDegrees(atan2(oklab.blue, oklab.green).toDouble())
    }

    private fun hueDifference(degrees: Double, from: Double): Double = (degrees - from + 180.0).mod(360.0) - 180.0
}
