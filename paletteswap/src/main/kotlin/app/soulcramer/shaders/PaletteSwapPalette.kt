package app.soulcramer.shaders

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The hues of a generated palette, with the names of the hue modes of `AcerolaFX_PaletteSwap.fx`.
 *
 * Each mode has two meanings. [oklchPalette] uses the colour harmony of the mode: a set of hues at fixed angles from
 * the base hue. [randomPalette] uses the hue sweep of AcerolaFX: the mode multiplies the hue offset of every colour by
 * its factor, so a larger factor spreads the hues over more of the colour wheel.
 *
 * The harmony is a list of rings, in turns, from the base hue outward. A ring at d adds the hue base + d on the dark
 * side of the palette and the hue base - d on the light side.
 */
public enum class PaletteSwapHueMode(internal val factor: Float, internal val rings: List<Float>) {
    /**
     * In [oklchPalette], only the base hue. In [randomPalette], multiplies the hue offset by 0, so every colour has the
     * base hue.
     */
    Monochromatic(0f, emptyList()),

    /**
     * In [oklchPalette], the base hue in the middle, the hue 30° above it on the dark side, and the hue 30° below it on
     * the light side. In [randomPalette], multiplies the hue offset by 0.25.
     */
    Analogous(0.25f, listOf(1f / 12f)),

    /**
     * In [oklchPalette], the base hue in the middle and the hue 180° from it at the dark end and the light end. In
     * [randomPalette], multiplies the hue offset by 0.33.
     */
    Complementary(0.33f, listOf(1f / 2f)),

    /**
     * In [oklchPalette], the base hue in the middle, the hue 120° from it on the dark side, and the hue 240° from it on
     * the light side. In [randomPalette], multiplies the hue offset by 0.66.
     */
    TriadicComplementary(0.66f, listOf(1f / 3f)),

    /**
     * In [oklchPalette], the base hue in the middle, the hue 90° from it on the dark side, the hue 270° from it on the
     * light side, and the hue 180° from it at the dark end and the light end. In [randomPalette], multiplies the hue
     * offset by 0.75.
     */
    TetradicComplementary(0.75f, listOf(1f / 4f, 1f / 2f)),
}

private const val Pi: Float = PI.toFloat()

/** The integer hash of Hugo Elias in `AcerolaFX_PaletteSwap.fx`. It returns a value from 0 to 1. */
internal fun hash(n: Int): Float {
    // Int wraps modulo 2^32 as uint does. Upstream writes 0x1376312589U, and ReShade keeps its low 32 bits.
    val x = (n shl 13) xor n
    val h = x * (x * x * 15731 + 0x789221) + 0x76312589
    return (h and 0x7fffffff).toFloat() / 0x7fffffff.toFloat()
}

/**
 * Generates a palette of [count] colours from [seed], as the generator of the Acerola video "I tried to make a better
 * color palette generator".
 *
 * The seed selects the base hue and one value from each range. The colours go from the base values at the first colour
 * to the base values plus the full contrast at the last colour, in equal steps in the OKLCH colour space. The hues use
 * the hue sweep of `AcerolaFX_PaletteSwap.fx`, not the colour harmonies of [oklchPalette]. With the defaults, the
 * requested chroma stays constant and the lightness increases from the first colour to the last colour. When a colour
 * is outside sRGB, the function reduces its chroma until it fits, so the lightness and the hue stay. Thus the darkest
 * and lightest colours can have less chroma than the others. The function then applies the sRGB transfer function.
 * `AcerolaFX_PaletteSwap.fx` does neither, so a seed does not give the same colours as in AcerolaFX. The same arguments
 * always give the same palette.
 *
 * @param seed The seed of the palette, from 0 to 1,000,000,000. The function clamps values outside this range.
 * @param count The number of colours, from 3 to 16. The function clamps values outside this range.
 * @param hueMode How much of the colour wheel the hues use. The function multiplies the hue offset of every colour by
 *   the factor of the mode, as `AcerolaFX_PaletteSwap.fx` does.
 * @param hueContrast The range of the hue increase from the first colour to the last colour, in turns of the colour
 *   wheel, before [hueMode] multiplies the increase by its factor. The factor of [PaletteSwapHueMode.Monochromatic]
 *   is 0, so every colour has the base hue.
 * @param luminance The range of the OKLCH lightness of the first colour.
 * @param luminanceContrast The range of the lightness increase from the first colour to the last colour.
 * @param chroma The range of the OKLCH chroma of the first colour.
 * @param chromaContrast The range of the chroma increase from the first colour to the last colour.
 * @return The colours in the order that [paletteSwap] expects, with each channel from 0 to 1.
 */
public fun randomPalette(
    seed: Int = PaletteSwapDefaults.SEED,
    count: Int = PaletteSwapDefaults.COLOR_COUNT,
    hueMode: PaletteSwapHueMode = PaletteSwapDefaults.HUE_MODE,
    hueContrast: ClosedFloatingPointRange<Float> = PaletteSwapDefaults.HUE_CONTRAST,
    luminance: ClosedFloatingPointRange<Float> = PaletteSwapDefaults.LUMINANCE,
    luminanceContrast: ClosedFloatingPointRange<Float> = PaletteSwapDefaults.LUMINANCE_CONTRAST,
    chroma: ClosedFloatingPointRange<Float> = PaletteSwapDefaults.CHROMA,
    chromaContrast: ClosedFloatingPointRange<Float> = PaletteSwapDefaults.CHROMA_CONTRAST,
): List<Color> {
    val clampedSeed = seed.coerceIn(0, 1_000_000_000)
    val hueBase = hash(clampedSeed) * 2f * Pi
    val seedHueContrast = hueContrast.lerp(hash(clampedSeed + 2))
    return oklchSteps(
        chroma = chroma.lerp(hash(clampedSeed + 5)),
        count = count,
        luminance = luminance.lerp(hash(clampedSeed + 13)),
        luminanceContrast = luminanceContrast.lerp(hash(clampedSeed + 3)),
        chromaContrast = chromaContrast.lerp(hash(clampedSeed + 7)),
    ) { i, colorCount ->
        val t = i.toFloat() / (colorCount - 1)
        hueBase + (seedHueContrast * t * 2f * Pi + Pi / 4f) * hueMode.factor
    }
}

/**
 * Generates a palette of [count] colours from a base [hue] and a base [chroma], with the colour harmony of [hueMode].
 *
 * The base hue takes the middle colours. Each ring of the harmony then takes the next colours outward on both sides:
 * its hue above the base hue on the dark side, and its hue below the base hue on the light side. Thus the hues are
 * symmetric about the middle. The hue 180° from the base hue is the same on both sides, so it takes the darkest and
 * the lightest colours. Each hue takes about the same number of colours. When [count] is small, the palette does not
 * have the outermost hues. The lightness and the chroma go from the base values at the first colour to the base
 * values plus the contrasts at the last colour, in equal steps in the OKLCH colour space. When a colour is outside
 * sRGB, the function reduces its chroma until it fits, so the lightness and the hue stay. The function then applies
 * the sRGB transfer function. The same arguments always give the same palette.
 *
 * @param hue The base OKLCH hue, in turns of the colour wheel, from 0 to 1. The function wraps values outside this
 *   range, so 1.25 gives the same palette as 0.25.
 * @param chroma The OKLCH chroma of the first colour. The function clamps negative values to 0.
 * @param count The number of colours, from 3 to 16. The function clamps values outside this range.
 * @param hueMode The colour harmony of the hues.
 * @param luminance The OKLCH lightness of the first colour. The default is 0.25, the midpoint of
 *   [PaletteSwapDefaults.LUMINANCE].
 * @param luminanceContrast The lightness increase from the first colour to the last colour. The default is 0.55, the
 *   midpoint of [PaletteSwapDefaults.LUMINANCE_CONTRAST].
 * @param chromaContrast The chroma increase from the first colour to the last colour. The default is 0, the midpoint
 *   of [PaletteSwapDefaults.CHROMA_CONTRAST].
 * @return The colours in the order that [paletteSwap] expects, with each channel from 0 to 1.
 */
public fun oklchPalette(
    hue: Float,
    chroma: Float,
    count: Int = PaletteSwapDefaults.COLOR_COUNT,
    hueMode: PaletteSwapHueMode = PaletteSwapDefaults.HUE_MODE,
    luminance: Float = PaletteSwapDefaults.LUMINANCE.lerp(0.5f),
    luminanceContrast: Float = PaletteSwapDefaults.LUMINANCE_CONTRAST.lerp(0.5f),
    chromaContrast: Float = PaletteSwapDefaults.CHROMA_CONTRAST.lerp(0.5f),
): List<Color> {
    val base = hue - floor(hue)
    val offsets = listOf(0f) + hueMode.rings
    // The outer edge of each hue on one side, from the middle. The hue 180° from the base is the same on both sides,
    // so its ring is half as wide.
    val edges = hueMode.rings.runningFold(0.5f) { edge, ring -> edge + if (ring == 0.5f) 0.5f else 1f }
    return oklchSteps(
        chroma = chroma,
        count = count,
        luminance = luminance,
        luminanceContrast = luminanceContrast,
        chromaContrast = chromaContrast,
    ) { i, colorCount ->
        // Integer distances from the middle keep entries i and colorCount - 1 - i exactly symmetric.
        val fromMiddle = 2 * i + 1 - colorCount
        val distance = abs(fromMiddle) * edges.last() / colorCount
        val offset = offsets[edges.indexOfFirst { distance <= it }]
        val turns = if (fromMiddle < 0) base + offset else base - offset
        (turns - floor(turns)) * 2f * Pi
    }
}

private fun oklchSteps(
    chroma: Float,
    count: Int,
    luminance: Float,
    luminanceContrast: Float,
    chromaContrast: Float,
    hue: (index: Int, colorCount: Int) -> Float,
): List<Color> {
    val colorCount = count.coerceIn(3, MaxPaletteSize)
    return List(colorCount) { i ->
        val t = i.toFloat() / (colorCount - 1)
        val entryChroma = (chroma + chromaContrast * t).coerceAtLeast(0f)
        oklchToColor(luminance + luminanceContrast * t, entryChroma, hue(i, colorCount))
    }
}

private fun ClosedFloatingPointRange<Float>.lerp(fraction: Float): Float = start + (endInclusive - start) * fraction

private const val GamutTolerance: Float = 1e-4f

private fun oklchToColor(lightness: Float, chroma: Float, hue: Float): Color {
    var rgb = oklchToLinearSrgb(lightness, chroma, hue)
    if (!rgb.fitsSrgb()) {
        val clampedLightness = lightness.coerceIn(0f, 1f)
        var low = 0f
        var high = chroma
        repeat(20) {
            val mid = (low + high) / 2f
            if (oklchToLinearSrgb(clampedLightness, mid, hue).fitsSrgb()) low = mid else high = mid
        }
        rgb = oklchToLinearSrgb(clampedLightness, low, hue)
    }
    return Color(
        rgb[0].coerceIn(0f, 1f),
        rgb[1].coerceIn(0f, 1f),
        rgb[2].coerceIn(0f, 1f),
        colorSpace = ColorSpaces.LinearSrgb,
    )
        .convert(ColorSpaces.Srgb)
}

private fun FloatArray.fitsSrgb(): Boolean = all { it in -GamutTolerance..1f + GamutTolerance }

private fun oklchToLinearSrgb(lightness: Float, chroma: Float, hue: Float): FloatArray {
    val a = chroma * cos(hue)
    val b = chroma * sin(hue)
    val l = cube(lightness + 0.3963377774f * a + 0.2158037573f * b)
    val m = cube(lightness - 0.1055613458f * a - 0.0638541728f * b)
    val s = cube(lightness - 0.0894841775f * a - 1.2914855480f * b)
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
    )
}

private fun cube(x: Float): Float = x * x * x
