package app.soulcramer.shaders

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorBlindnessTypeTest {
    @Test
    fun codesMatchTheShaderTypes() {
        assertEquals(0, ColorBlindnessType.Protanomaly.code)
        assertEquals(1, ColorBlindnessType.Deuteranomaly.code)
        assertEquals(2, ColorBlindnessType.Tritanomaly.code)
    }
}
