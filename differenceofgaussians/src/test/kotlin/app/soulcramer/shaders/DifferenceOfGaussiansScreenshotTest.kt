package app.soulcramer.shaders

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test

class DifferenceOfGaussiansScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
    )

    @Test
    fun defaults() {
        snapshotDifferenceOfGaussians(Modifier.differenceOfGaussians())
    }

    @Test
    fun tanhInvert() {
        snapshotDifferenceOfGaussians(Modifier.differenceOfGaussians(tanh = true, phi = { 50f }, invert = true))
    }

    private fun snapshotDifferenceOfGaussians(differenceOfGaussians: Modifier) {
        paparazzi.snapshot {
            // Decode here, not in a property initialiser: BitmapFactory needs the layoutlib natives that the
            // snapshot loads, and fails with UnsatisfiedLinkError before then.
            val bitmap = sampleBitmap()
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(100.dp).clipToBounds().then(differenceOfGaussians),
            )
        }
    }

    private fun sampleBitmap(): ImageBitmap = requireNotNull(javaClass.classLoader?.getResourceAsStream("sample.png"))
        .use { BitmapFactory.decodeStream(it) }
        .asImageBitmap()
}
