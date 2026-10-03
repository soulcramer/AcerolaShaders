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
 * Simulates the colour vision deficiency [type] on the content of this element.
 *
 * The modifier calls [severity] only when it updates the graphics layer of the content. When [severity] reads Compose
 * state, a change to that state updates the layer without a recomposition of the caller.
 *
 * @param type The colour vision deficiency to simulate.
 * @param severity Returns the severity of the deficiency, from 0 (none) to 1 (full). The shader clamps values outside
 * this range.
 */
public fun Modifier.colorBlindness(type: ColorBlindnessType, severity: () -> Float): Modifier =
    this then ColorBlindnessElement(type, severity)

private class ColorBlindnessElement(private val type: ColorBlindnessType, private val severity: () -> Float) :
    ModifierNodeElement<ColorBlindnessNode>() {
    override fun create(): ColorBlindnessNode = ColorBlindnessNode(type, severity)

    override fun update(node: ColorBlindnessNode) {
        node.update(type, severity)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "colorBlindness"
        properties["type"] = type
        properties["severity"] = severity
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorBlindnessElement) return false
        return type == other.type && severity === other.severity
    }

    override fun hashCode(): Int = 31 * type.hashCode() + severity.hashCode()
}

/**
 * Owns one [RuntimeShader] for the lifetime of the node and applies it through the layer of the content.
 *
 * The layer block calls [severity] under snapshot observation, so a state read inside it invalidates only the layer
 * properties, not the measure, the placement, or the drawing of the content. A [RenderEffect] copies the uniforms when
 * it is created, so the node creates a new one only when the type or the severity value differ from the last effect.
 */
private class ColorBlindnessNode(private var type: ColorBlindnessType, private var severity: () -> Float) :
    Modifier.Node(),
    LayoutModifierNode {
    private val shader = RuntimeShader(ColorBlindnessShader)
    private var effect: ComposeRenderEffect? = null
    private var effectType: ColorBlindnessType? = null
    private var effectSeverity: Float = Float.NaN

    private val layerBlock: GraphicsLayerScope.() -> Unit = { renderEffect = effectFor(type, severity()) }

    // update() refreshes the layer itself, so the default remeasure is unnecessary.
    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(type: ColorBlindnessType, severity: () -> Float) {
        if (type == this.type && severity === this.severity) return
        this.type = type
        this.severity = severity
        // Rerun the layer block so that it calls the new lambda, even one that reads no state, and observes its reads
        // instead of the reads of the previous lambda.
        updateLayerBlock(layerBlock)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    private fun effectFor(type: ColorBlindnessType, severity: Float): ComposeRenderEffect {
        val current = effect
        if (current != null && type == effectType && severity == effectSeverity) return current
        shader.setFloatUniform("severity", severity)
        shader.setIntUniform("colorblindType", type.code)
        return RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect().also {
            effect = it
            effectType = type
            effectSeverity = severity
        }
    }
}
