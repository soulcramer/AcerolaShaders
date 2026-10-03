package app.soulcramer.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
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
 * Default values of the [crt] modifier, which match the defaults of `AcerolaFX_CRT.fx`.
 */
public object CrtDefaults {
    /** Default [crt] curvature. */
    public const val CURVATURE: Float = 10f

    /** Default [crt] vignette width, in pixels. */
    public const val VIGNETTE_WIDTH: Float = 30f

    /** Default [crt] line size. */
    public const val LINE_SIZE: Int = 0

    /** Default [crt] line strength. */
    public const val LINE_STRENGTH: Float = 1f

    /** Default [crt] brightness adjustment. */
    public const val BRIGHTNESS_ADJUST: Float = 0f
}

private val DefaultCurvature: () -> Float = { CrtDefaults.CURVATURE }
private val DefaultVignetteWidth: () -> Float = { CrtDefaults.VIGNETTE_WIDTH }
private val DefaultLineStrength: () -> Float = { CrtDefaults.LINE_STRENGTH }
private val DefaultBrightnessAdjust: () -> Float = { CrtDefaults.BRIGHTNESS_ADJUST }

/**
 * Makes the content of this element look like a CRT screen: a curved screen, scanline colour fringing, and a
 * vignette. Outside the curved screen, the modifier draws opaque black.
 *
 * The modifier calls the lambda parameters only when it updates the graphics layer of the content. When a lambda
 * reads Compose state, a change to that state updates the layer without a recomposition of the caller. The scanlines
 * follow the screen density, so they keep the same physical size on every screen. The shader clamps every value to
 * its range.
 *
 * @param curvature Returns the curvature of the screen, from 1 to 10. A higher value warps the screen less.
 * @param vignetteWidth Returns the width of the darkened edges, from 1 to 100 pixels.
 * @param lineSize Scales the scanline spacing by 2 to the power of this value, from 0 to 4.
 * @param lineStrength Returns the strength of the scanlines, from 1 to 5.
 * @param brightnessAdjust Returns an adjustment to the brightness of the scanlines, from -1 to 1.
 */
public fun Modifier.crt(
    curvature: () -> Float = DefaultCurvature,
    vignetteWidth: () -> Float = DefaultVignetteWidth,
    lineSize: Int = CrtDefaults.LINE_SIZE,
    lineStrength: () -> Float = DefaultLineStrength,
    brightnessAdjust: () -> Float = DefaultBrightnessAdjust,
): Modifier = this then CrtElement(curvature, vignetteWidth, lineSize, lineStrength, brightnessAdjust)

private class CrtElement(
    private val curvature: () -> Float,
    private val vignetteWidth: () -> Float,
    private val lineSize: Int,
    private val lineStrength: () -> Float,
    private val brightnessAdjust: () -> Float,
) : ModifierNodeElement<CrtNode>() {
    override fun create(): CrtNode = CrtNode(curvature, vignetteWidth, lineSize, lineStrength, brightnessAdjust)

    override fun update(node: CrtNode) {
        node.update(curvature, vignetteWidth, lineSize, lineStrength, brightnessAdjust)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "crt"
        properties["curvature"] = curvature
        properties["vignetteWidth"] = vignetteWidth
        properties["lineSize"] = lineSize
        properties["lineStrength"] = lineStrength
        properties["brightnessAdjust"] = brightnessAdjust
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CrtElement) return false
        return curvature === other.curvature &&
            vignetteWidth === other.vignetteWidth &&
            lineSize == other.lineSize &&
            lineStrength === other.lineStrength &&
            brightnessAdjust === other.brightnessAdjust
    }

    override fun hashCode(): Int {
        var result = curvature.hashCode()
        result = 31 * result + vignetteWidth.hashCode()
        result = 31 * result + lineSize
        result = 31 * result + lineStrength.hashCode()
        result = 31 * result + brightnessAdjust.hashCode()
        return result
    }
}

/** The uniform values of one [RenderEffect]. */
private data class CrtUniforms(
    val width: Float,
    val height: Float,
    val pixelDensity: Float,
    val curvature: Float,
    val vignetteWidth: Float,
    val lineSize: Int,
    val lineStrength: Float,
    val brightnessAdjust: Float,
)

/**
 * Owns one [RuntimeShader] for the lifetime of the node and applies it through the layer of the content.
 *
 * The layer block calls the lambdas under snapshot observation, so a state read inside them invalidates only the
 * layer properties, not the measure, the placement, or the drawing of the content. The layer block also runs when the
 * size of the content changes. A [RenderEffect] copies the uniforms when it is created, so the node creates a new one
 * only when a uniform value differs from the last effect.
 */
private class CrtNode(
    private var curvature: () -> Float,
    private var vignetteWidth: () -> Float,
    private var lineSize: Int,
    private var lineStrength: () -> Float,
    private var brightnessAdjust: () -> Float,
) : Modifier.Node(),
    LayoutModifierNode {
    private val shader = RuntimeShader(CrtShader)
    private var effect: ComposeRenderEffect? = null
    private var effectUniforms: CrtUniforms? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val uniforms = CrtUniforms(
            width = size.width,
            height = size.height,
            pixelDensity = density,
            curvature = curvature(),
            vignetteWidth = vignetteWidth(),
            lineSize = lineSize,
            lineStrength = lineStrength(),
            brightnessAdjust = brightnessAdjust(),
        )
        renderEffect = effectFor(uniforms)
    }

    // update() refreshes the layer itself, so the default remeasure is unnecessary.
    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(
        curvature: () -> Float,
        vignetteWidth: () -> Float,
        lineSize: Int,
        lineStrength: () -> Float,
        brightnessAdjust: () -> Float,
    ) {
        if (
            curvature === this.curvature &&
            vignetteWidth === this.vignetteWidth &&
            lineSize == this.lineSize &&
            lineStrength === this.lineStrength &&
            brightnessAdjust === this.brightnessAdjust
        ) {
            return
        }
        this.curvature = curvature
        this.vignetteWidth = vignetteWidth
        this.lineSize = lineSize
        this.lineStrength = lineStrength
        this.brightnessAdjust = brightnessAdjust
        // Rerun the layer block so that it calls the new lambdas, even ones that read no state, and observes their
        // reads instead of the reads of the previous lambdas.
        updateLayerBlock(layerBlock)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    private fun effectFor(uniforms: CrtUniforms): ComposeRenderEffect {
        val current = effect
        if (current != null && uniforms == effectUniforms) return current
        shader.setFloatUniform("size", uniforms.width, uniforms.height)
        shader.setFloatUniform("pixelDensity", uniforms.pixelDensity)
        shader.setFloatUniform("curvature", uniforms.curvature)
        shader.setFloatUniform("vignetteWidth", uniforms.vignetteWidth)
        shader.setIntUniform("lineSize", uniforms.lineSize)
        shader.setFloatUniform("lineStrength", uniforms.lineStrength)
        shader.setFloatUniform("brightnessAdjust", uniforms.brightnessAdjust)
        return RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect().also {
            effect = it
            effectUniforms = uniforms
        }
    }
}
