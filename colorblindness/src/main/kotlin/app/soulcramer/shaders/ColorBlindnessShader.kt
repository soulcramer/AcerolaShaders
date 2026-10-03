package app.soulcramer.shaders

public val ColorBlindnessShader: String = ColorBlindness + """
uniform float severity;
uniform int colorblindType;
uniform shader composable;

half4 main(float2 coord) {
    float4 col = composable.eval(coord);
    if (col.a > 0.0) {
        col.rgb /= col.a;
    }

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

    return float4(cb * col.a, col.a);
}
"""
