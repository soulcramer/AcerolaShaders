package app.soulcramer.shaders.ui.shaders.differenceofgaussians

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
import app.soulcramer.shaders.DifferenceOfGaussiansDefaults
import app.soulcramer.shaders.differenceOfGaussians
import app.soulcramer.shaders.ui.components.ShaderImage
import app.soulcramer.shaders.ui.components.ShaderParamLabel
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import kotlin.math.roundToInt

@Composable
public fun DifferenceOfGaussiansScreen(
    modifier: Modifier = Modifier,
) {
    var enabled by remember { mutableStateOf(true) }
    var kernelRadius by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.KERNEL_RADIUS.toFloat()) }
    var sigma by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.SIGMA) }
    var sigmaScale by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.SIGMA_SCALE) }
    var tau by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.TAU) }
    var thresholding by remember { mutableStateOf(DifferenceOfGaussiansDefaults.THRESHOLDING) }
    var tanh by remember { mutableStateOf(DifferenceOfGaussiansDefaults.TANH) }
    var phi by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.PHI) }
    var threshold by remember { mutableFloatStateOf(DifferenceOfGaussiansDefaults.THRESHOLD) }
    var invert by remember { mutableStateOf(DifferenceOfGaussiansDefaults.INVERT) }

    Column(modifier.verticalScroll(rememberScrollState())) {
        ShaderImage(
            effect = if (enabled) {
                Modifier
                    .clipToBounds()
                    .differenceOfGaussians(
                        kernelRadius = kernelRadius.roundToInt(),
                        sigma = { sigma },
                        sigmaScale = { sigmaScale },
                        tau = { tau },
                        thresholding = thresholding,
                        tanh = tanh,
                        phi = { phi },
                        threshold = { threshold },
                        invert = invert,
                    )
            } else {
                Modifier
            },
        )

        ShaderParamLabel(paramName = "Kernel Radius: ${kernelRadius.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = kernelRadius,
            valueRange = 1f..10f,
            // Steps exclude the two end values, so 8 steps give every whole number from 1 to 10.
            steps = 8,
            onValueChange = { kernelRadius = it },
        )

        ShaderParamLabel(paramName = "Sigma: ${sigma.toTenths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = sigma,
            valueRange = 0.1f..5f,
            onValueChange = { sigma = it },
        )

        ShaderParamLabel(paramName = "Sigma Scale: ${sigmaScale.toTenths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = sigmaScale,
            valueRange = 0.1f..5f,
            onValueChange = { sigmaScale = it },
        )

        ShaderParamLabel(paramName = "Tau: ${tau.toHundredths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = tau,
            valueRange = 0.01f..5f,
            onValueChange = { tau = it },
        )

        ShaderParamLabel(paramName = "Thresholding")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = thresholding,
            onCheckedChange = { thresholding = it },
        )

        ShaderParamLabel(paramName = "Tanh")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = tanh,
            onCheckedChange = { tanh = it },
        )

        ShaderParamLabel(paramName = "Phi: ${phi.toHundredths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = phi,
            valueRange = 0.01f..100f,
            onValueChange = { phi = it },
        )

        ShaderParamLabel(paramName = "Threshold: ${threshold.toThousandths()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = threshold,
            valueRange = -1f..1f,
            onValueChange = { threshold = it },
        )

        ShaderParamLabel(paramName = "Invert")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = invert,
            onCheckedChange = { invert = it },
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

private fun Float.toHundredths(): Float = (this * 100f).roundToInt() / 100f

// The default threshold is 0.005, which tenths and hundredths both round away.
private fun Float.toThousandths(): Float = (this * 1000f).roundToInt() / 1000f

@Preview
@Composable
private fun DifferenceOfGaussiansScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            DifferenceOfGaussiansScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
