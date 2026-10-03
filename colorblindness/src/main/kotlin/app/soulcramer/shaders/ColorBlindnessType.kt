package app.soulcramer.shaders

/**
 * A type of colour vision deficiency that [ColorBlindnessShader] simulates.
 *
 * @property code The `colorblindType` uniform value that [ColorBlindnessShader] expects.
 */
public enum class ColorBlindnessType(public val code: Int) {
    Protanomaly(0),
    Deuteranomaly(1),
    Tritanomaly(2),
}
