package app.soulcramer.shaders

import org.junit.Assert.assertEquals
import org.junit.Test

class DifferenceOfGaussiansDefaultsTest {
    @Test
    fun defaultsMatchPostProcessing() {
        assertEquals(5, DifferenceOfGaussiansDefaults.KERNEL_RADIUS)
        assertEquals(2f, DifferenceOfGaussiansDefaults.SIGMA, 0f)
        assertEquals(1.6f, DifferenceOfGaussiansDefaults.SIGMA_SCALE, 0f)
        assertEquals(1f, DifferenceOfGaussiansDefaults.TAU, 0f)
        assertEquals(true, DifferenceOfGaussiansDefaults.THRESHOLDING)
        assertEquals(false, DifferenceOfGaussiansDefaults.TANH)
        assertEquals(1f, DifferenceOfGaussiansDefaults.PHI, 0f)
        assertEquals(0.005f, DifferenceOfGaussiansDefaults.THRESHOLD, 0f)
        assertEquals(false, DifferenceOfGaussiansDefaults.INVERT)
    }
}
