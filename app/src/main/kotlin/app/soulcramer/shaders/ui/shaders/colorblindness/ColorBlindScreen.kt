package app.soulcramer.shaders.ui.shaders.colorblindness

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.ColorBlindnessShader
import app.soulcramer.shaders.ui.components.SegmentedButton
import app.soulcramer.shaders.ui.components.ShaderImage
import app.soulcramer.shaders.ui.components.ShaderParamLabel
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import kotlin.math.roundToInt

@Composable
public fun ColorBlindScreen(
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val runtimeShader = remember { RuntimeShader(ColorBlindnessShader) }

        var severity by remember { mutableStateOf(0.5f) }
        val options = ColorBlindNessType.entries.map { it.name }
        var selectedOption by remember {
            mutableStateOf(options.first())
        }

        ShaderImage(shader = runtimeShader) {
            runtimeShader.setFloatUniform("severity", severity)
            runtimeShader.setIntUniform(
                "colorblindType",
                ColorBlindNessType.valueOf(selectedOption).ordinal,
            )
        }

        ShaderParamLabel(paramName = "Severity: ${(severity * 10).roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = severity,
            valueRange = 0f..1f,
            steps = 10,
            onValueChange = {
                severity = it
            },
        )

        ShaderParamLabel(paramName = "Color blindness type")

        SegmentedButton(
            options = options,
            selectedOption = selectedOption,
            onOptionSelect = { option ->
                selectedOption = option
            },
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
                .height(48.dp),
        )
    }
}

/**
 * The ordinal is the `colorblindType` index that `getColorBlindnessMatrix` in the shader expects.
 */
public enum class ColorBlindNessType {
    Protanomaly,
    Deuteranomaly,
    Tritanomaly,
}

@Preview
@Composable
private fun ColorBlindScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            ColorBlindScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
