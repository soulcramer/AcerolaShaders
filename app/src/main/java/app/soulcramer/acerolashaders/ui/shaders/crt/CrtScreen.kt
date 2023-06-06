package app.soulcramer.acerolashaders.ui.shaders.crt

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.acerolashaders.ui.shaders.colorblindness.ColorBlindScreen
import app.soulcramer.acerolashaders.ui.shaders.colorblindness.ColorBlindness
import app.soulcramer.acerolashaders.ui.theme.AcerolaShadersTheme
import coil.compose.AsyncImage
import kotlin.math.roundToInt

val shader = """
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
fun CrtScreen(
    modifier: Modifier = Modifier,
    imageUrl: String = "https://scontent-cdg4-3.cdninstagram.com/v/t51.2885-15/290239787_733169821163694_599554219358445074_n.webp?stp=dst-jpg_e35&_nc_ht=scontent-cdg4-3.cdninstagram.com&_nc_cat=106&_nc_ohc=chkKLrV9SnsAX99UC1o&edm=ACWDqb8BAAAA&ccb=7-5&ig_cache_key=Mjg2ODY2ODQxMzY0NDA1MzAyOQ%3D%3D.2-ccb7-5&oh=00_AfBjUlnuv39uJu9CKmQZCbIaOPP-8Kb_JacFBuxFKSiYkw&oe=648422E6&_nc_sid=640168"
) {
    val runtimeShader = RuntimeShader(shader)

    var enabled by remember { mutableStateOf(true) }
    var curvature by remember { mutableStateOf(10f) }
    val curvatureLabel by remember {
        derivedStateOf {
            (curvature).roundToInt()
        }
    }
    runtimeShader.setFloatUniform("curvature", curvature.coerceIn(1f, 10f))

    var vignetteWidth by remember { mutableStateOf(30f) }
    val vignetteWidthLabel by remember {
        derivedStateOf {
            vignetteWidth.roundToInt()
        }
    }
    runtimeShader.setFloatUniform("vignetteWidth", vignetteWidth.coerceIn(1f, 100f))
    runtimeShader.setFloatUniform("pixelDensity", LocalDensity.current.density)


    Column(modifier) {

        AsyncImage(
            model = imageUrl,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .onSizeChanged { size ->
                    runtimeShader.setFloatUniform(
                        "size",
                        size.width.toFloat(),
                        size.height.toFloat()
                    )
                }
                .then(
                    if (enabled) {
                        Modifier.graphicsLayer {
                            clip = true
                            renderEffect = RenderEffect
                                .createRuntimeShaderEffect(
                                    runtimeShader, // The RuntimeShader
                                    "composable" // The name of the uniform for the RenderNode content
                                )
                                .asComposeRenderEffect()
                        }
                    } else Modifier
                )

        )

        ShaderParamLabel(paramName = "Curvature: $curvatureLabel")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = curvature,
            valueRange = 1f..10f,
            steps = 10,
            onValueChange = {
                curvature = it
            }
        )

        ShaderParamLabel(paramName = "Vignette Width: $vignetteWidthLabel")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = vignetteWidth,
            valueRange = 1f..100f,
            steps = 100,
            onValueChange = {
                vignetteWidth = it
            }
        )

        ShaderParamLabel(paramName = "Enable Shader")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = enabled,
            onCheckedChange = {
                enabled = it
            }
        )

    }
}

@Composable
fun ShaderParamLabel(
    paramName: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = paramName,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    )
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
