package app.soulcramer.shaders

import org.intellij.lang.annotations.Language

/** The number of entries of the `palette` uniform array of [PaletteSwapShader]. */
internal const val MaxPaletteSize: Int = 16

/**
 * AGSL port of `AcerolaFX_PaletteSwap.fx` from AcerolaFX.
 *
 * The shader replaces the colour of each pixel with one palette entry. The luminance or the red channel of the
 * unpremultiplied content selects the entry. The shader keeps the alpha of the content.
 */
@Language("agsl")
internal val PaletteSwap: String = """
uniform float4 palette[16];
uniform int count;
uniform int indexMode;
uniform shader composable;

half4 main(float2 coord) {
    // The content is premultiplied, so select the entry from the straight colour and premultiply the entry at the end.
    float4 col = composable.eval(coord);
    float3 rgb = col.a > 0.0 ? col.rgb / col.a : float3(0.0);
    rgb = saturate(rgb);

    float v = indexMode == 1 ? rgb.r : saturate(dot(rgb, float3(0.2127, 0.7152, 0.0722)));
    // AGSL has no integer min or clamp, because they are GLSL ES 3 functions, so compute the index in float.
    float n = clamp(float(count), 1.0, 16.0);
    int index = int(min(floor(v * n), n - 1.0));

    // An AGSL array index must be a constant or a loop index, so loop over every entry and keep the one at the index.
    float3 swapped = float3(0.0);
    for (int i = 0; i < 16; i++) {
        if (i == index) {
            swapped = palette[i].rgb;
        }
    }

    return half4(swapped * col.a, col.a);
}
"""

/**
 * AGSL source of the palette swap shader, a port of `AcerolaFX_PaletteSwap.fx` from AcerolaFX.
 *
 * Build a [android.graphics.RuntimeShader] from this source and set these uniforms:
 * - `palette` (`float4[16]`): the palette entries, as sRGB components. Set it with an array of 64 floats, that is the
 *   red, green, blue, and alpha of each entry. The shader ignores the alpha.
 * - `count` (`int`, 1 to 16): the number of palette entries, from the first entry.
 * - `indexMode` (`int`): 1 selects the entry by the red channel of the content, and any other value selects it by the
 *   luminance. [PaletteSwapIndex.code] gives the value of each mode.
 *
 * The shader clamps `count` to its range. It divides the range 0 to 1 into `count` bands of the same width, and a
 * value in band i takes entry i. It reads its input from the `composable` child shader.
 */
public val PaletteSwapShader: String = PaletteSwap
