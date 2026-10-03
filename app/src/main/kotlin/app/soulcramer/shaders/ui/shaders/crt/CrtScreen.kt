package app.soulcramer.shaders.ui.shaders.crt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.CrtDefaults
import app.soulcramer.shaders.crt
import app.soulcramer.shaders.ui.components.ShaderImage
import app.soulcramer.shaders.ui.components.ShaderParamLabel
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import kotlin.math.roundToInt

@Composable
public fun CrtScreen(
    modifier: Modifier = Modifier,
) {
    var enabled by remember { mutableStateOf(true) }
    var curvature by remember { mutableFloatStateOf(CrtDefaults.CURVATURE) }
    var vignetteWidth by remember { mutableFloatStateOf(CrtDefaults.VIGNETTE_WIDTH) }
    var lineSize by remember { mutableFloatStateOf(CrtDefaults.LINE_SIZE.toFloat()) }
    var lineStrength by remember { mutableFloatStateOf(CrtDefaults.LINE_STRENGTH) }
    var brightnessAdjust by remember { mutableFloatStateOf(CrtDefaults.BRIGHTNESS_ADJUST) }

    Column(modifier.verticalScroll(rememberScrollState())) {
        ShaderImage(
            effect = if (enabled) {
                Modifier
                    .clipToBounds()
                    .crt(
                        curvature = { curvature },
                        vignetteWidth = { vignetteWidth },
                        lineSize = lineSize.roundToInt(),
                        lineStrength = { lineStrength },
                        brightnessAdjust = { brightnessAdjust },
                    )
            } else {
                Modifier
            },
        )

        ShaderParamLabel(paramName = "Curvature: ${curvature.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = curvature,
            valueRange = 1f..10f,
            // Steps exclude the two end values, so 8 steps give every whole number from 1 to 10.
            steps = 8,
            onValueChange = { curvature = it },
        )

        ShaderParamLabel(paramName = "Vignette Width: ${vignetteWidth.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = vignetteWidth,
            valueRange = 1f..100f,
            steps = 98,
            onValueChange = { vignetteWidth = it },
        )

        ShaderParamLabel(paramName = "Line Size: ${lineSize.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = lineSize,
            valueRange = 0f..4f,
            steps = 3,
            onValueChange = { lineSize = it },
        )

        ShaderParamLabel(paramName = "Line Strength: ${lineStrength.toTenths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = lineStrength,
            valueRange = 1f..5f,
            // 39 steps give every tenth from 1 to 5.
            steps = 39,
            onValueChange = { lineStrength = it },
        )

        ShaderParamLabel(paramName = "Brightness Adjust: ${brightnessAdjust.toTenths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = brightnessAdjust,
            valueRange = -1f..1f,
            // 19 steps give every tenth from -1 to 1.
            steps = 19,
            onValueChange = { brightnessAdjust = it },
        )

        ShaderParamLabel(paramName = "Enable Shader")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = enabled,
            onCheckedChange = { enabled = it },
        )
    }
}

private fun Float.toTenths(): Float = (this * 10f).roundToInt() / 10f

@Preview
@Composable
private fun CrtScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            CrtScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
