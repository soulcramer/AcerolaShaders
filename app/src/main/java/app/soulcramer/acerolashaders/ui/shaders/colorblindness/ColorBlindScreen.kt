package app.soulcramer.acerolashaders.ui.shaders.colorblindness

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.acerolashaders.ui.components.SegmentedButton
import app.soulcramer.acerolashaders.ui.theme.AcerolaShadersTheme
import coil.compose.AsyncImage
import kotlin.math.roundToInt

val shader = ColorBlindness + """
uniform float severity;
uniform int colorblindType;
uniform shader composable;

half4 main(float2 coord) {
    float4 col = composable.eval(coord);

    int p1 = int(min(10, floor(severity * 10.0)));
    int p2 = int(min(10, floor((severity + 0.1) * 10.0)));
    float weight = fract(severity * 10.0);

    float3x3 matrix1 = getColorBlindnessMatrix(colorblindType, p1);
    float3x3 matrix2 = getColorBlindnessMatrix(colorblindType, p2);

    float3 newCB1 = mix(matrix1[0], matrix2[0], weight);
    float3 newCB2 = mix(matrix1[1], matrix2[1], weight);
    float3 newCB3 = mix(matrix1[2], matrix2[2], weight);

    float3x3 blindness = float3x3(newCB1, newCB2, newCB3);

    float3 cb = saturate(col.rgb * blindness);

    return float4(cb, 1.0);
}
"""

@Composable
fun ColorBlindScreen(
    modifier: Modifier = Modifier,
    imageUrl: String = "https://scontent-cdg4-3.cdninstagram.com/v/t51.2885-15/290239787_733169821163694_599554219358445074_n.webp?stp=dst-jpg_e35&_nc_ht=scontent-cdg4-3.cdninstagram.com&_nc_cat=106&_nc_ohc=chkKLrV9SnsAX99UC1o&edm=ACWDqb8BAAAA&ccb=7-5&ig_cache_key=Mjg2ODY2ODQxMzY0NDA1MzAyOQ%3D%3D.2-ccb7-5&oh=00_AfBjUlnuv39uJu9CKmQZCbIaOPP-8Kb_JacFBuxFKSiYkw&oe=648422E6&_nc_sid=640168"
) {
    Column(modifier) {
        val runtimeShader = RuntimeShader(shader)

        AsyncImage(
            model = imageUrl,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .graphicsLayer {
                    clip = true
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(
                            runtimeShader, // The RuntimeShader
                            "composable" // The name of the uniform for the RenderNode content
                        )
                        .asComposeRenderEffect()
                }
        )
        var severity by remember { mutableStateOf(0.5f) }
        val severityLabel by remember {
            derivedStateOf {
                (severity * 10).roundToInt()
            }
        }
        runtimeShader.setFloatUniform("severity", severity)

        Text(
            text = "Severity: $severityLabel",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        )
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = severity,
            valueRange = 0f..1f,
            steps = 10,
            onValueChange = {
                severity = it
            }
        )


        Text(
            text = "Color blindness type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        )

        val options = ColorBlindNessType.values().map { it.name }
        var selectedOption by remember {
            mutableStateOf(options.first())
        }
        runtimeShader.setIntUniform(
            "colorblindType",
            ColorBlindNessType.valueOf(selectedOption).ordinal
        )

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

enum class ColorBlindNessType {
    Deuteranomaly,
    Protanomaly,
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
