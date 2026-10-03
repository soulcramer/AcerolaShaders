package app.soulcramer.shaders.ui.shaders.colorblindness

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.ColorBlindnessType
import app.soulcramer.shaders.colorBlindness
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
        var severity by remember { mutableStateOf(0.5f) }
        val options = ColorBlindnessType.entries.map { it.name }
        var selectedOption by remember {
            mutableStateOf(options.first())
        }

        ShaderImage(
            effect = Modifier
                .clipToBounds()
                .colorBlindness(type = ColorBlindnessType.valueOf(selectedOption), severity = { severity }),
        )

        ShaderParamLabel(paramName = "Severity: ${(severity * 10).roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = severity,
            valueRange = 0f..1f,
            steps = 9,
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

@Preview
@Composable
private fun ColorBlindScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            ColorBlindScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
