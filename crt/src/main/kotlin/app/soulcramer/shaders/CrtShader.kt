package app.soulcramer.shaders

import org.intellij.lang.annotations.Language

/**
 * AGSL port of `AcerolaFX_CRT.fx` from AcerolaFX.
 *
 * The shader warps the content into a curved screen, adds scanline colour fringing, and darkens the edges with a
 * vignette. Outside the warped screen it outputs opaque black. Inside it keeps the alpha of the content and applies
 * the scanline and vignette factors to the unpremultiplied colour only.
 */
@Language("agsl")
internal val Crt: String = """
uniform float2 size;
uniform float pixelDensity;
uniform float curvature;
uniform float vignetteWidth;
uniform int lineSize;
uniform float lineStrength;
uniform float brightnessAdjust;
uniform shader composable;

half4 main(float2 coord) {
    // Warp the uv coordinates, in the range [-1, 1] with 0 at the centre, with a cubic function.
    float2 uv = coord / size * 2.0 - 1.0;
    // A higher curvature value warps the image less.
    float2 offset = uv.yx / clamp(curvature, 1.0, 10.0);
    uv = uv + uv * offset * offset;
    uv = uv * 0.5 + 0.5;

    if (uv.x <= 0.0 || 1.0 <= uv.x || uv.y <= 0.0 || 1.0 <= uv.y) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }

    // The content is premultiplied, so work on the straight colour and premultiply again at the end.
    float4 col = composable.eval(uv * size);
    float3 rgb = col.a > 0.0 ? col.rgb / col.a : float3(0.0);
    rgb = saturate(rgb);

    uv = uv * 2.0 - 1.0;
    float2 vignette = clamp(vignetteWidth, 1.0, 100.0) / size;
    vignette = saturate(smoothstep(float2(0.0), vignette, 1.0 - abs(uv)));

    // Space the lines by the pixel density so that they keep the same physical size on every screen.
    float spacing = max(pixelDensity, 0.01) * exp2(clamp(float(lineSize), 0.0, 4.0));
    float phase = coord.y * 2.0 / spacing;
    float strength = clamp(lineStrength, 1.0, 5.0);
    float brightness = clamp(brightnessAdjust, -1.0, 1.0);
    rgb.g *= (sin(phase) + 1.0) * 0.15 * strength + 1.0 + brightness;
    rgb.rb *= (cos(phase) + 1.0) * 0.135 * strength + 1.0 + brightness;

    rgb = saturate(rgb) * vignette.x * vignette.y;

    return half4(rgb * col.a, col.a);
}
"""

/**
 * AGSL source of the CRT shader, a port of `AcerolaFX_CRT.fx` from AcerolaFX.
 *
 * Build a [android.graphics.RuntimeShader] from this source and set these uniforms:
 * - `size` (`float2`): the size of the content, in pixels.
 * - `pixelDensity` (`float`): the screen density, in pixels per density-independent pixel. It sets the base spacing of
 *   the scanlines.
 * - `curvature` (`float`, 1 to 10): a higher value warps the screen less.
 * - `vignetteWidth` (`float`, 1 to 100): the width of the darkened edges, in pixels.
 * - `lineSize` (`int`, 0 to 4): scales the scanline spacing by 2 to the power of this value.
 * - `lineStrength` (`float`, 1 to 5): the strength of the scanlines.
 * - `brightnessAdjust` (`float`, -1 to 1): adds to the brightness of the scanlines.
 *
 * The shader clamps each value to its range. It reads its input from the `composable` child shader.
 */
public val CrtShader: String = Crt
