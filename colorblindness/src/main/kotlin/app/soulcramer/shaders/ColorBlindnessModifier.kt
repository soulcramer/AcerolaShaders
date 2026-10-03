package app.soulcramer.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Simulates the colour vision deficiency [type] on the content of this element.
 *
 * @param type The colour vision deficiency to simulate.
 * @param severity The severity of the deficiency, from 0 (none) to 1 (full). The shader clamps values outside this range.
 */
public fun Modifier.colorBlindness(type: ColorBlindnessType, severity: Float): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "colorBlindness"
        properties["type"] = type
        properties["severity"] = severity
    },
) {
    val shader = remember { RuntimeShader(ColorBlindnessShader) }
    graphicsLayer {
        shader.setFloatUniform("severity", severity)
        shader.setIntUniform("colorblindType", type.code)
        renderEffect = RenderEffect
            .createRuntimeShaderEffect(shader, "composable")
            .asComposeRenderEffect()
    }
}
