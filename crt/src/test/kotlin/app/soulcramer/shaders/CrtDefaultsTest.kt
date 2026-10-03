package app.soulcramer.shaders

import org.junit.Assert.assertEquals
import org.junit.Test

class CrtDefaultsTest {
    @Test
    fun defaultsMatchAcerolaFx() {
        assertEquals(10f, CrtDefaults.CURVATURE, 0f)
        assertEquals(30f, CrtDefaults.VIGNETTE_WIDTH, 0f)
        assertEquals(0, CrtDefaults.LINE_SIZE)
        assertEquals(1f, CrtDefaults.LINE_STRENGTH, 0f)
        assertEquals(0f, CrtDefaults.BRIGHTNESS_ADJUST, 0f)
    }
}
