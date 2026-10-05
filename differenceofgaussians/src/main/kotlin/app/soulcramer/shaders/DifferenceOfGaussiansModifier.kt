package app.soulcramer.shaders

import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints

/**
 * Default values of the [differenceOfGaussians] modifier, which match the defaults of `DifferenceOfGaussians.cs`.
 */
public object DifferenceOfGaussiansDefaults {
    /** Default [differenceOfGaussians] kernel radius, in pixels. */
    public const val KERNEL_RADIUS: Int = 5

    /** Default [differenceOfGaussians] standard deviation of the first Gaussian. */
    public const val SIGMA: Float = 2f

    /** Default [differenceOfGaussians] scale of the standard deviation of the second Gaussian. */
    public const val SIGMA_SCALE: Float = 1.6f

    /** Default [differenceOfGaussians] weight of the second Gaussian. */
    public const val TAU: Float = 1f

    /** Default [differenceOfGaussians] thresholding toggle. */
    public const val THRESHOLDING: Boolean = true

    /** Default [differenceOfGaussians] tanh toggle. */
    public const val TANH: Boolean = false

    /** Default [differenceOfGaussians] steepness of the tanh fall-off. */
    public const val PHI: Float = 1f

    /** Default [differenceOfGaussians] threshold. */
    public const val THRESHOLD: Float = 0.005f

    /** Default [differenceOfGaussians] invert toggle. */
    public const val INVERT: Boolean = false
}

private val DefaultSigma: () -> Float = { DifferenceOfGaussiansDefaults.SIGMA }
private val DefaultSigmaScale: () -> Float = { DifferenceOfGaussiansDefaults.SIGMA_SCALE }
private val DefaultTau: () -> Float = { DifferenceOfGaussiansDefaults.TAU }
private val DefaultPhi: () -> Float = { DifferenceOfGaussiansDefaults.PHI }
private val DefaultThreshold: () -> Float = { DifferenceOfGaussiansDefaults.THRESHOLD }

/**
 * Draws the content of this element as edge lines: the difference of two Gaussian blurs of the luminance, with an
 * optional threshold. With the defaults, the modifier draws white lines on black. The modifier keeps the alpha of the
 * content.
 *
 * The modifier calls the lambda parameters only when it updates the graphics layer of the content. When a lambda
 * reads Compose state, a change to that state updates the layer without a recomposition of the caller. The shader
 * clamps every value to its range.
 *
 * @param kernelRadius The radius of the blur, from 1 to 10 pixels.
 * @param sigma Returns the standard deviation of the first Gaussian, from 0.1 to 5.
 * @param sigmaScale Returns the scale from [sigma] to the standard deviation of the second Gaussian, from 0.1 to 5.
 * @param tau Returns the weight of the second Gaussian in the difference, from 0.01 to 5.
 * @param thresholding When `true`, values at or above the threshold become white and the other values become black.
 * @param tanh When `true` and [thresholding] is `true`, values below the threshold fall off smoothly instead of
 *   becoming black.
 * @param phi Returns the steepness of the tanh fall-off, from 0.01 to 100.
 * @param threshold Returns the threshold of the difference, from -1 to 1.
 * @param invert When `true`, the modifier inverts the result.
 */
public fun Modifier.differenceOfGaussians(
    kernelRadius: Int = DifferenceOfGaussiansDefaults.KERNEL_RADIUS,
    sigma: () -> Float = DefaultSigma,
    sigmaScale: () -> Float = DefaultSigmaScale,
    tau: () -> Float = DefaultTau,
    thresholding: Boolean = DifferenceOfGaussiansDefaults.THRESHOLDING,
    tanh: Boolean = DifferenceOfGaussiansDefaults.TANH,
    phi: () -> Float = DefaultPhi,
    threshold: () -> Float = DefaultThreshold,
    invert: Boolean = DifferenceOfGaussiansDefaults.INVERT,
): Modifier = this then DifferenceOfGaussiansElement(
    kernelRadius,
    sigma,
    sigmaScale,
    tau,
    thresholding,
    tanh,
    phi,
    threshold,
    invert,
)

private class DifferenceOfGaussiansElement(
    private val kernelRadius: Int,
    private val sigma: () -> Float,
    private val sigmaScale: () -> Float,
    private val tau: () -> Float,
    private val thresholding: Boolean,
    private val tanh: Boolean,
    private val phi: () -> Float,
    private val threshold: () -> Float,
    private val invert: Boolean,
) : ModifierNodeElement<DifferenceOfGaussiansNode>() {
    override fun create(): DifferenceOfGaussiansNode =
        DifferenceOfGaussiansNode(kernelRadius, sigma, sigmaScale, tau, thresholding, tanh, phi, threshold, invert)

    override fun update(node: DifferenceOfGaussiansNode) {
        node.update(kernelRadius, sigma, sigmaScale, tau, thresholding, tanh, phi, threshold, invert)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "differenceOfGaussians"
        properties["kernelRadius"] = kernelRadius
        properties["sigma"] = sigma
        properties["sigmaScale"] = sigmaScale
        properties["tau"] = tau
        properties["thresholding"] = thresholding
        properties["tanh"] = tanh
        properties["phi"] = phi
        properties["threshold"] = threshold
        properties["invert"] = invert
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DifferenceOfGaussiansElement) return false
        return kernelRadius == other.kernelRadius &&
            sigma === other.sigma &&
            sigmaScale === other.sigmaScale &&
            tau === other.tau &&
            thresholding == other.thresholding &&
            tanh == other.tanh &&
            phi === other.phi &&
            threshold === other.threshold &&
            invert == other.invert
    }

    override fun hashCode(): Int {
        var result = kernelRadius
        result = 31 * result + sigma.hashCode()
        result = 31 * result + sigmaScale.hashCode()
        result = 31 * result + tau.hashCode()
        result = 31 * result + thresholding.hashCode()
        result = 31 * result + tanh.hashCode()
        result = 31 * result + phi.hashCode()
        result = 31 * result + threshold.hashCode()
        result = 31 * result + invert.hashCode()
        return result
    }
}

/** The uniform values of one chain effect, apart from the size. */
private data class DifferenceOfGaussiansUniforms(
    val kernelRadius: Int,
    val sigma: Float,
    val sigmaScale: Float,
    val tau: Float,
    val thresholding: Boolean,
    val tanh: Boolean,
    val phi: Float,
    val threshold: Float,
    val invert: Boolean,
)

/**
 * Owns the two passes for the lifetime of the node and applies their chain through the layer of the content.
 *
 * The layer block calls the lambdas under snapshot observation, so a state read inside them invalidates only the
 * layer properties, not the measure, the placement, or the drawing of the content. [RenderPassChain] creates a new
 * effect only when the size or a uniform value differs from the last effect.
 */
private class DifferenceOfGaussiansNode(
    private var kernelRadius: Int,
    private var sigma: () -> Float,
    private var sigmaScale: () -> Float,
    private var tau: () -> Float,
    private var thresholding: Boolean,
    private var tanh: Boolean,
    private var phi: () -> Float,
    private var threshold: () -> Float,
    private var invert: Boolean,
) : Modifier.Node(),
    LayoutModifierNode {
    private val chain = RenderPassChain(
        RuntimeShader(DifferenceOfGaussiansBlurShader),
        RuntimeShader(DifferenceOfGaussiansThresholdShader),
    )
    private lateinit var uniforms: DifferenceOfGaussiansUniforms

    private val setUniforms: (List<RuntimeShader>) -> Unit = { passes ->
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

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        uniforms = DifferenceOfGaussiansUniforms(
            kernelRadius = kernelRadius,
            sigma = sigma(),
            sigmaScale = sigmaScale(),
            tau = tau(),
            thresholding = thresholding,
            tanh = tanh,
            phi = phi(),
            threshold = threshold(),
            invert = invert,
        )
        renderEffect = chain.effect(size.width, size.height, uniforms, setUniforms)
    }

    // update() refreshes the layer itself, so the default remeasure is unnecessary.
    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(
        kernelRadius: Int,
        sigma: () -> Float,
        sigmaScale: () -> Float,
        tau: () -> Float,
        thresholding: Boolean,
        tanh: Boolean,
        phi: () -> Float,
        threshold: () -> Float,
        invert: Boolean,
    ) {
        if (
            kernelRadius == this.kernelRadius &&
            sigma === this.sigma &&
            sigmaScale === this.sigmaScale &&
            tau === this.tau &&
            thresholding == this.thresholding &&
            tanh == this.tanh &&
            phi === this.phi &&
            threshold === this.threshold &&
            invert == this.invert
        ) {
            return
        }
        this.kernelRadius = kernelRadius
        this.sigma = sigma
        this.sigmaScale = sigmaScale
        this.tau = tau
        this.thresholding = thresholding
        this.tanh = tanh
        this.phi = phi
        this.threshold = threshold
        this.invert = invert
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
}
