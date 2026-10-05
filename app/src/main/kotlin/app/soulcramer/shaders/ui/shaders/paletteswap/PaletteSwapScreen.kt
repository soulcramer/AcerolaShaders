package app.soulcramer.shaders.ui.shaders.paletteswap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.PaletteSwapDefaults
import app.soulcramer.shaders.PaletteSwapHueMode
import app.soulcramer.shaders.PaletteSwapIndex
import app.soulcramer.shaders.oklchPalette
import app.soulcramer.shaders.paletteSwap
import app.soulcramer.shaders.randomPalette
import app.soulcramer.shaders.ui.components.SegmentedButton
import app.soulcramer.shaders.ui.components.ShaderImage
import app.soulcramer.shaders.ui.components.ShaderParamLabel
import app.soulcramer.shaders.ui.theme.AcerolaShadersTheme
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

// One list instance for each control lets SegmentedButton skip when the colour wheel recomposes the screen.
private val HueModeOptions = PaletteSwapHueMode.entries.map { it.name }
private val IndexOptions = PaletteSwapIndex.entries.map { it.name }

// The OKLCH hue of this colour matches the hue of the default seed, and its HSV value is 1, as every colour on the wheel.
private val InitialColor = Color(0xFFFFA1F1)

// Below this chroma the hue is noise, so a pick near the centre of the wheel keeps the previous hue.
private const val MinHueChroma = 0.01f

/** Shows the palette swap effect on an image, with controls for the generated palette and the index. */
@Composable
public fun PaletteSwapScreen(
    modifier: Modifier = Modifier,
) {
    var enabled by remember { mutableStateOf(true) }
    var hue by remember { mutableFloatStateOf(InitialColor.oklchHue()) }
    var chroma by remember { mutableFloatStateOf(InitialColor.oklchChroma()) }
    // Null while the colour wheel sets the palette.
    var seed by remember { mutableStateOf<Int?>(null) }
    var colorCount by remember { mutableIntStateOf(PaletteSwapDefaults.COLOR_COUNT) }
    var hueMode by remember { mutableStateOf(PaletteSwapHueMode.Complementary) }
    var index by remember { mutableStateOf(PaletteSwapDefaults.INDEX) }
    val palette = remember(seed, hue, chroma, colorCount, hueMode) {
        seed?.let { randomPalette(seed = it, count = colorCount, hueMode = hueMode) }
            ?: oklchPalette(hue = hue, chroma = chroma, count = colorCount, hueMode = hueMode)
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        ShaderImage(
            effect = if (enabled) {
                Modifier
                    .clipToBounds()
                    .paletteSwap(palette = palette, index = index)
            } else {
                Modifier
            },
        )

        ShaderParamLabel(paramName = "Palette")
        PaletteSwatches(
            palette = palette,
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
                .height(32.dp),
        )

        val chromaText = String.format(Locale.ROOT, "%.2f", chroma)
        ShaderParamLabel(
            paramName = seed?.let { "Seed: $it" }
                ?: "Base hue and chroma: ${(hue * 360f).roundToInt()}° / $chromaText",
        )
        Box(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HsvColorPicker(
                modifier = Modifier.size(200.dp),
                controller = rememberColorPickerController(),
                initialColor = InitialColor,
                onColorChanged = { envelope ->
                    chroma = envelope.color.oklchChroma()
                    if (chroma >= MinHueChroma) hue = envelope.color.oklchHue()
                    // The picker also reports the colours that it sets itself, for example the initial colour.
                    if (envelope.fromUser) seed = null
                },
            )
        }
        Button(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = { seed = Random.nextInt(0, 1_000_000_001) },
        ) {
            Text(text = "Random seed")
        }

        ShaderParamLabel(paramName = "Color Count: $colorCount")
        Slider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = colorCount.toFloat(),
            valueRange = 3f..16f,
            // Steps exclude the two end values, so 12 steps give every whole number from 3 to 16.
            steps = 12,
            onValueChange = { colorCount = it.roundToInt() },
        )

        // The buttons end long hue mode names with an ellipsis, so the label shows the full name of the selection.
        ShaderParamLabel(paramName = "Hue Mode: ${hueMode.name}")
        SegmentedButton(
            options = HueModeOptions,
            selectedOption = hueMode.name,
            onOptionSelect = { hueMode = PaletteSwapHueMode.valueOf(it) },
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
                .height(48.dp),
        )

        ShaderParamLabel(paramName = "Index")
        SegmentedButton(
            options = IndexOptions,
            selectedOption = index.name,
            onOptionSelect = { index = PaletteSwapIndex.valueOf(it) },
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
                .height(48.dp),
        )

        ShaderParamLabel(paramName = "Enable Shader")
        Switch(
            modifier = Modifier.padding(horizontal = 16.dp),
            checked = enabled,
            onCheckedChange = { enabled = it },
        )
    }
}

/** Returns the OKLCH hue in turns, from 0 to 1. */
private fun Color.oklchHue(): Float {
    val oklab = convert(ColorSpaces.Oklab)
    val turns = atan2(oklab.blue, oklab.green) / (2f * PI.toFloat())
    return turns - floor(turns)
}

private fun Color.oklchChroma(): Float {
    val oklab = convert(ColorSpaces.Oklab)
    return hypot(oklab.green, oklab.blue)
}

@Composable
private fun PaletteSwatches(
    palette: List<Color>,
    modifier: Modifier = Modifier,
) {
    // The outline keeps a dark first colour visible on a dark background.
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small),
    ) {
        palette.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Preview
@Composable
private fun PaletteSwapScreenPreview() {
    AcerolaShadersTheme {
        Surface {
            PaletteSwapScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
