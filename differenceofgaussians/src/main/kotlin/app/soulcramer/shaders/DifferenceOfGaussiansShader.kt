package app.soulcramer.shaders

import org.intellij.lang.annotations.Language

/**
 * First pass of the AGSL port of `DifferenceOfGaussians.shader` from Post-Processing.
 *
 * The pass blurs the luminance of the content horizontally with two Gaussians and packs the results into red and
 * green. It writes the content alpha into blue and outputs alpha 1, so that premultiplication does not scale the
 * packed values.
 */
@Language("agsl")
internal val DifferenceOfGaussiansBlur: String = """
const float PI = 3.14159265358979323846;

uniform float2 size;
uniform int kernelRadius;
uniform float sigma;
uniform float sigmaScale;
uniform shader composable;

float gaussian(float s, float pos) {
    return (1.0 / sqrt(2.0 * PI * s * s)) * exp(-(pos * pos) / (2.0 * s * s));
}

half4 main(float2 coord) {
    int radius = int(clamp(float(kernelRadius), 1.0, 10.0));
    float sigma1 = clamp(sigma, 0.1, 5.0);
    float sigma2 = sigma1 * clamp(sigmaScale, 0.1, 5.0);

    float sum1 = 0.0;
    float sum2 = 0.0;
    float kernelSum1 = 0.0;
    float kernelSum2 = 0.0;
    // AGSL needs a constant loop bound, so loop to the largest kernel and leave at the end of this one.
    for (int i = 0; i < 21; i++) {
        if (i > 2 * radius) break;
        float x = float(i - radius);
        // The clamp matches the clamp sampler of the Unity textures.
        float4 c = composable.eval(clamp(coord + float2(x, 0.0), float2(0.5), size - 0.5));
        float3 rgb = c.a > 0.0 ? c.rgb / c.a : float3(0.0);
        float luminance = dot(rgb, float3(0.299, 0.587, 0.114));
        float gauss1 = gaussian(sigma1, x);
        float gauss2 = gaussian(sigma2, x);
        sum1 += luminance * gauss1;
        kernelSum1 += gauss1;
        sum2 += luminance * gauss2;
        kernelSum2 += gauss2;
    }

    float alpha = composable.eval(coord).a;
    return half4(sum1 / kernelSum1, sum2 / kernelSum2, alpha, 1.0);
}
"""

/**
 * Second pass of the AGSL port of `DifferenceOfGaussians.shader` from Post-Processing.
 *
 * The pass blurs the two packed Gaussians of [DifferenceOfGaussiansBlur] vertically, takes their difference, and
 * applies the threshold, the tanh soft threshold, and the invert. It restores the content alpha from blue.
 */
@Language("agsl")
internal val DifferenceOfGaussiansThreshold: String = """
const float PI = 3.14159265358979323846;

uniform float2 size;
uniform int kernelRadius;
uniform float sigma;
uniform float sigmaScale;
uniform float tau;
uniform int thresholding;
uniform int useTanh;
uniform float phi;
uniform float threshold;
uniform int invert;
uniform shader composable;

float gaussian(float s, float pos) {
    return (1.0 / sqrt(2.0 * PI * s * s)) * exp(-(pos * pos) / (2.0 * s * s));
}

// AGSL has no tanh, because it is a GLSL ES 3 function.
float hyperbolicTangent(float x) {
    return 1.0 - 2.0 / (exp(2.0 * x) + 1.0);
}

half4 main(float2 coord) {
    int radius = int(clamp(float(kernelRadius), 1.0, 10.0));
    float sigma1 = clamp(sigma, 0.1, 5.0);
    float sigma2 = sigma1 * clamp(sigmaScale, 0.1, 5.0);

    float sum1 = 0.0;
    float sum2 = 0.0;
    float kernelSum1 = 0.0;
    float kernelSum2 = 0.0;
    for (int i = 0; i < 21; i++) {
        if (i > 2 * radius) break;
        float y = float(i - radius);
        float4 c = composable.eval(clamp(coord + float2(0.0, y), float2(0.5), size - 0.5));
        float gauss1 = gaussian(sigma1, y);
        float gauss2 = gaussian(sigma2, y);
        sum1 += c.r * gauss1;
        kernelSum1 += gauss1;
        sum2 += c.g * gauss2;
        kernelSum2 += gauss2;
    }

    float D = sum1 / kernelSum1 - clamp(tau, 0.01, 5.0) * (sum2 / kernelSum2);

    float t = clamp(threshold, -1.0, 1.0);
    if (thresholding != 0) {
        if (useTanh != 0) {
            D = (D >= t) ? 1.0 : 1.0 + hyperbolicTangent(clamp(phi, 0.01, 100.0) * (D - t));
        } else {
            D = (D >= t) ? 1.0 : 0.0;
        }
    }

    if (invert != 0) {
        D = 1.0 - D;
    }

    half a = half(composable.eval(coord).b);
    return half4(half3(saturate(D)) * a, a);
}
"""

/**
 * AGSL source of the first pass of the difference of Gaussians shader, a port of `DifferenceOfGaussians.shader` from
 * Post-Processing.
 *
 * Build a [android.graphics.RuntimeShader] from this source and set these uniforms:
 * - `size` (`float2`): the size of the content, in pixels.
 * - `kernelRadius` (`int`, 1 to 10): the radius of the blur, in pixels.
 * - `sigma` (`float`, 0.1 to 5): the standard deviation of the first Gaussian.
 * - `sigmaScale` (`float`, 0.1 to 5): multiplies `sigma` to give the standard deviation of the second Gaussian.
 *
 * The shader clamps each value to its range. It reads its input from the `composable` child shader. It writes the
 * two horizontal Gaussians of the luminance into red and green, the content alpha into blue, and alpha 1. Chain
 * [DifferenceOfGaussiansThresholdShader] after it.
 */
public val DifferenceOfGaussiansBlurShader: String = DifferenceOfGaussiansBlur

/**
 * AGSL source of the second pass of the difference of Gaussians shader, a port of `DifferenceOfGaussians.shader`
 * from Post-Processing.
 *
 * Build a [android.graphics.RuntimeShader] from this source and set these uniforms:
 * - `size` (`float2`): the size of the content, in pixels.
 * - `kernelRadius`, `sigma`, `sigmaScale`: the same values as for [DifferenceOfGaussiansBlurShader].
 * - `tau` (`float`, 0.01 to 5): the weight of the second Gaussian in the difference.
 * - `thresholding` (`int`, 0 or 1): when 1, the shader thresholds the difference.
 * - `useTanh` (`int`, 0 or 1): when 1, values below the threshold fall off smoothly instead of becoming 0.
 * - `phi` (`float`, 0.01 to 100): the steepness of the tanh fall-off.
 * - `threshold` (`float`, -1 to 1): the threshold of the difference.
 * - `invert` (`int`, 0 or 1): when 1, the shader inverts the result.
 *
 * The shader clamps each value to its range. It reads the output of [DifferenceOfGaussiansBlurShader] from the
 * `composable` child shader.
 */
public val DifferenceOfGaussiansThresholdShader: String = DifferenceOfGaussiansThreshold
