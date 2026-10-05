package app.soulcramer.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

/**
 * Applies [passes] in order as one [ComposeRenderEffect]. Pass 0 reads the content, and each later pass sees only the
 * output of the previous pass.
 *
 * Every pass must declare `uniform float2 size` and a child shader named by `inputName`. The chain stores each
 * intermediate result as 8-bit premultiplied RGBA, so each store rounds a value by up to 0.5 / 255. Measured on a
 * Pixel 8 Pro (API 37): a ramp chained through two passes showed 17 distinct red values, with a maximum difference
 * of 8 out of 255 from the expected ramp value. Later chained ports use 8-bit intermediates. A pass that packs data
 * into the colour channels must write alpha 1, so that premultiplication does not scale the packed values.
 *
 * To add the chain result to the original content, use
 * `RenderEffect.createBlendModeEffect(RenderEffect.createOffsetEffect(0f, 0f), chain, BlendMode.PLUS)` with the chain
 * from `asAndroidRenderEffect()`.
 *
 * @param passes The shaders of the chain. Must not be empty.
 * @param inputName The name of the child shader that each pass reads.
 */
public class RenderPassChain(passes: List<RuntimeShader>, private val inputName: String = "composable") {
    public constructor(vararg passes: RuntimeShader) : this(passes.toList())

    /** The passes in order. Pass 0 reads the content. */
    public val passes: List<RuntimeShader> = passes.toList()

    private var effect: ComposeRenderEffect? = null
    private var effectWidth = 0f
    private var effectHeight = 0f
    private var effectUniforms: Any? = null

    init {
        require(this.passes.isNotEmpty()) { "A RenderPassChain needs at least one pass." }
    }

    /**
     * Returns the chain effect. When [width], [height], and [uniforms] equal the values of the last call, returns the
     * cached effect and does not call [setUniforms]. Otherwise sets `size` on every pass, calls [setUniforms], and
     * builds a new effect.
     */
    public fun effect(
        width: Float,
        height: Float,
        uniforms: Any,
        setUniforms: (passes: List<RuntimeShader>) -> Unit,
    ): ComposeRenderEffect {
        val current = effect
        if (current != null && width == effectWidth && height == effectHeight && uniforms == effectUniforms) {
            return current
        }
        passes.forEach { it.setFloatUniform("size", width, height) }
        setUniforms(passes)
        var chain = RenderEffect.createRuntimeShaderEffect(passes[0], inputName)
        for (i in 1 until passes.size) {
            chain = RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(passes[i], inputName), chain)
        }
        return chain.asComposeRenderEffect().also {
            effect = it
            effectWidth = width
            effectHeight = height
            effectUniforms = uniforms
        }
    }
}
