package app.soulcramer.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

/**
 * The value of the content that selects the palette colour of each pixel in [paletteSwap].
 *
 * @property code The `indexMode` uniform value that [PaletteSwapShader] expects.
 */
public enum class PaletteSwapIndex(public val code: Int) {
    /** Selects the colour by the luminance of the content, so that colour content also works. */
    Luminance(0),

    /** Selects the colour by the red channel of the content, as `AcerolaFX_PaletteSwap.fx` does. */
    Red(1),
}

/**
 * Default values of the [paletteSwap] modifier and the [randomPalette] generator. They match the defaults of
 * `AcerolaFX_PaletteSwap.fx`, except the luminance, luminance contrast, chroma, and chroma contrast ranges.
 */
public object PaletteSwapDefaults {
    /** Default [paletteSwap] palette: four greys at 0, 0.1, 0.2, and 0.3. */
    public val PALETTE: List<Color> = listOf(
        Color(0f, 0f, 0f),
        Color(0.1f, 0.1f, 0.1f),
        Color(0.2f, 0.2f, 0.2f),
        Color(0.3f, 0.3f, 0.3f),
    )

    /** Default [paletteSwap] index. */
    public val INDEX: PaletteSwapIndex = PaletteSwapIndex.Luminance

    /** Default [randomPalette] colour count. */
    public const val COLOR_COUNT: Int = 4

    /** Default [randomPalette] seed. */
    public const val SEED: Int = 0

    /** Default [randomPalette] hue mode. */
    public val HUE_MODE: PaletteSwapHueMode = PaletteSwapHueMode.Monochromatic

    /** Default [randomPalette] hue contrast range. */
    public val HUE_CONTRAST: ClosedFloatingPointRange<Float> = 0f..1f

    /** Default [randomPalette] luminance range. Upstream uses 0 to 1, which makes most colours too light to show. */
    public val LUMINANCE: ClosedFloatingPointRange<Float> = 0.2f..0.3f

    /**
     * Default [randomPalette] luminance contrast range. Upstream uses 0 to 1, and this range keeps every palette in
     * order from dark to light.
     */
    public val LUMINANCE_CONTRAST: ClosedFloatingPointRange<Float> = 0.5f..0.6f

    /**
     * Default [randomPalette] chroma range. Upstream uses 0 to 1, which puts most colours outside sRGB. Many colours in
     * this range are also outside sRGB, and [randomPalette] reduces their chroma until they fit.
     */
    public val CHROMA: ClosedFloatingPointRange<Float> = 0.12f..0.20f

    /** Default [randomPalette] chroma contrast range. Upstream uses 0 to 1, and the video keeps the chroma constant. */
    public val CHROMA_CONTRAST: ClosedFloatingPointRange<Float> = 0f..0f
}

/**
 * Replaces the colour of each pixel of the content of this element with one colour of [palette].
 *
 * The modifier divides the range of the [index] value, from 0 to 1, into one band of the same width for each colour.
 * Each pixel takes the colour of its band, so the first colour replaces the lowest values and the last colour
 * replaces the highest values. The modifier uses the sRGB red, green, and blue of each colour and ignores its alpha.
 * It keeps the alpha of the content. Use [randomPalette] to generate a palette from a seed.
 *
 * @param palette The colours, from 1 to 16, in the order of the bands.
 * @param index The value of the content that selects the colour of each pixel.
 * @throws IllegalArgumentException When [palette] holds fewer than 1 or more than 16 colours.
 */
public fun Modifier.paletteSwap(
    palette: List<Color> = PaletteSwapDefaults.PALETTE,
    index: PaletteSwapIndex = PaletteSwapDefaults.INDEX,
): Modifier {
    require(palette.size in 1..MaxPaletteSize) { "A palette holds 1 to $MaxPaletteSize colours, not ${palette.size}." }
    // The copy keeps a snapshot, so that a change to a mutable list of the caller still updates the effect.
    return this then PaletteSwapElement(palette.toList(), index)
}

private class PaletteSwapElement(private val palette: List<Color>, private val index: PaletteSwapIndex) :
    ModifierNodeElement<PaletteSwapNode>() {
    override fun create(): PaletteSwapNode = PaletteSwapNode(palette, index)

    override fun update(node: PaletteSwapNode) {
        node.update(palette, index)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "paletteSwap"
        properties["palette"] = palette
        properties["index"] = index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaletteSwapElement) return false
        return palette == other.palette && index == other.index
    }

    override fun hashCode(): Int = 31 * palette.hashCode() + index.hashCode()
}

/** The uniform values of one [RenderEffect]. */
private data class PaletteSwapUniforms(val palette: List<Color>, val index: PaletteSwapIndex)

/**
 * Owns one [RuntimeShader] for the lifetime of the node and applies it through the layer of the content.
 *
 * A [RenderEffect] copies the uniforms when it is created, so the node creates a new one only when the palette or the
 * index differs from the last effect.
 */
private class PaletteSwapNode(private var palette: List<Color>, private var index: PaletteSwapIndex) :
    Modifier.Node(),
    LayoutModifierNode {
    private val shader = RuntimeShader(PaletteSwapShader)
    private var effect: ComposeRenderEffect? = null
    private var effectUniforms: PaletteSwapUniforms? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        renderEffect = effectFor(PaletteSwapUniforms(palette, index))
    }

    // update() refreshes the layer itself, so the default remeasure is unnecessary.
    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(palette: List<Color>, index: PaletteSwapIndex) {
        if (palette == this.palette && index == this.index) return
        this.palette = palette
        this.index = index
        updateLayerBlock(layerBlock)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    private fun effectFor(uniforms: PaletteSwapUniforms): ComposeRenderEffect {
        val current = effect
        if (current != null && uniforms == effectUniforms) return current
        shader.setPaletteSwapUniforms(uniforms.palette, uniforms.index)
        return RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect().also {
            effect = it
            effectUniforms = uniforms
        }
    }
}

/**
 * Sets the uniforms of [PaletteSwapShader] from [palette] and [index]. Each colour takes its sRGB red, green, and blue
 * with alpha 1, and the unused entries stay 0.
 */
internal fun RuntimeShader.setPaletteSwapUniforms(palette: List<Color>, index: PaletteSwapIndex) {
    val values = FloatArray(MaxPaletteSize * 4)
    palette.forEachIndexed { i, color ->
        val srgb = color.convert(ColorSpaces.Srgb)
        values[i * 4] = srgb.red
        values[i * 4 + 1] = srgb.green
        values[i * 4 + 2] = srgb.blue
        values[i * 4 + 3] = 1f
    }
    setFloatUniform("palette", values)
    setIntUniform("count", palette.size)
    setIntUniform("indexMode", index.code)
}
