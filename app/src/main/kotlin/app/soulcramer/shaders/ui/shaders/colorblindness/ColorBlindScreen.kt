package app.soulcramer.shaders.ui.shaders.colorblindness

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.ColorBlindness
import app.soulcramer.shaders.ColorBlindnessShader
import app.soulcramer.shaders.app.R
import app.soulcramer.shaders.ui.components.SegmentedButton
import app.soulcramer.shaders.ui.shaders.crt.shader
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
public fun ColorBlindScreen(
    modifier: Modifier = Modifier,
    imageUrl: String = "https://scontent-cdg4-3.cdninstagram.com/v/t51.2885-15/290239787_733169821163694_59" +
        "9554219358445074_n.webp?stp=dst-jpg_e35&_nc_ht=scontent-cdg4-3.cdninstagram.com&_nc_cat=106&_n" +
        "c_ohc=chkKLrV9SnsAX99UC1o&edm=ACWDqb8BAAAA&ccb=7-5&ig_cache_key=Mjg2ODY2ODQxMzY0NDA1MzAyOQ%3D%" +
        "3D.2-ccb7-5&oh=00_AfBjUlnuv39uJu9CKmQZCbIaOPP-8Kb_JacFBuxFKSiYkw&oe=648422E6&_nc_sid=640168",
) {
    Column(modifier) {
        val runtimeShader = RuntimeShader(ColorBlindnessShader)

        var imageUri: Any? by remember { mutableStateOf(R.drawable.ic_launcher_background) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) {
            if (it != null) {
                Log.d("PhotoPicker", "Selected URI: $it")
                imageUri = it
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(enable = true)
                .build(),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .clickable {
                    photoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                }
                .graphicsLayer {
                    clip = true
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(
                            runtimeShader, // The RuntimeShader
                            "composable", // The name of the uniform for the RenderNode content
                        )
                        .asComposeRenderEffect()
                },
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
                .padding(top = 16.dp),
        )
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = severity,
            valueRange = 0f..1f,
            steps = 10,
            onValueChange = {
                severity = it
            },
        )

        Text(
            text = "Color blindness type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
        )

        val options = ColorBlindNessType.entries.map { it.name }
        var selectedOption by remember {
            mutableStateOf(options.first())
        }
        runtimeShader.setIntUniform(
            "colorblindType",
            ColorBlindNessType.valueOf(selectedOption).ordinal,
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

public enum class ColorBlindNessType {
    Deuteranomaly,
    Protanomaly,
    Tritanomaly,
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
private fun ColorBlindScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            ColorBlindScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
