package app.soulcramer.shaders.ui.shaders.crt

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.ui.components.ShaderImage
import app.soulcramer.shaders.ui.components.ShaderParamLabel
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import kotlin.math.roundToInt

private const val shader: String = """
uniform half2 size;
uniform float pixelDensity;
uniform float curvature;
uniform float vignetteWidth;
uniform shader composable;

half4 main(float2 coord) {
    // Use a cubic function that takes in the linear uv coordinates and returns sphericaly warped uv coordinates.
    float2 composableUV = coord / size;
    // range [-1, 1] with 0 being the center
    float2 uv = composableUV * 2.0 - 1.0;
    // create an offset value to control how much we warp the image
    // The higher the value, the less warped the image will be
    float2 offset = uv.yx / curvature;
    // apply the offset to the uv coordinates
    uv = uv + uv * offset * offset;
    // convert the warped uv coordinates back to the range [0, 1]
    uv = uv * 0.5 + 0.5;

    // sample the image with the warped uv coordinates
    half4 color = composable.eval(uv * size.xy);
    if (uv.x <= 0.0 || 1.0 <= uv.x || uv.y <= 0.0 || 1.0 <= uv.y) {
        color = half4(0);
    }

    // Once again we want to work in the range [-1, 1] with 0 being the center like in the warped coordinates.
    uv = uv * 2.0 - 1.0;
    float2 vignette = vignetteWidth / size.xy;
    vignette = smoothstep(float2(0.0,0.0), vignette, 1.0 - abs(uv));
    vignette = saturate(vignette);

    // Add some color fringing with the pixel density to make it look more like a crt screen on android phones
    color.g *= (sin(composableUV.y * size.y * 2.0 / pixelDensity) + 1.0) * 0.15 + 1.0;
    color.rb *= (cos(composableUV.y * size.y * 2.0 / pixelDensity) + 1.0) * 0.135 + 1.0;

    // Apply the vignette on the crt lines
    return saturate(color) * vignette.x * vignette.y;
}
"""

@Composable
public fun CrtScreen(
    modifier: Modifier = Modifier,
) {
    val runtimeShader = remember { RuntimeShader(shader) }

    var enabled by remember { mutableStateOf(true) }
    var curvature by remember { mutableStateOf(10f) }
    var vignetteWidth by remember { mutableStateOf(30f) }

    Column(modifier) {
        ShaderImage(shader = runtimeShader, enabled = enabled) {
            runtimeShader.setFloatUniform("size", size.width, size.height)
            runtimeShader.setFloatUniform("pixelDensity", density)
            runtimeShader.setFloatUniform("curvature", curvature.coerceIn(1f, 10f))
            runtimeShader.setFloatUniform("vignetteWidth", vignetteWidth.coerceIn(1f, 100f))
        }

        ShaderParamLabel(paramName = "Curvature: ${curvature.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = curvature,
            valueRange = 1f..10f,
            steps = 10,
            onValueChange = {
                curvature = it
            },
        )

        ShaderParamLabel(paramName = "Vignette Width: ${vignetteWidth.roundToInt()}")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = vignetteWidth,
            valueRange = 1f..100f,
            steps = 100,
            onValueChange = {
                vignetteWidth = it
            },
        )

        ShaderParamLabel(paramName = "Enable Shader")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = enabled,
            onCheckedChange = {
                enabled = it
            },
        )
    }
}

@Preview
@Composable
private fun CrtScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            CrtScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
