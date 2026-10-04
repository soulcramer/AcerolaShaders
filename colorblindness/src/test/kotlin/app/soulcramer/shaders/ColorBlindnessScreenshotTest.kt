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

class ColorBlindnessScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
    )

    @Test
    fun original() {
        snapshotColorBlindness(Modifier)
    }

    @Test
    fun protanomaly() {
        snapshotColorBlindness(Modifier.colorBlindness(ColorBlindnessType.Protanomaly) { 1f })
    }

    @Test
    fun deuteranomaly() {
        snapshotColorBlindness(Modifier.colorBlindness(ColorBlindnessType.Deuteranomaly) { 1f })
    }

    @Test
    fun tritanomaly() {
        snapshotColorBlindness(Modifier.colorBlindness(ColorBlindnessType.Tritanomaly) { 1f })
    }

    @Test
    fun deuteranomalySeverity50() {
        snapshotColorBlindness(Modifier.colorBlindness(ColorBlindnessType.Deuteranomaly) { 0.5f })
    }

    private fun snapshotColorBlindness(colorBlindness: Modifier) {
        paparazzi.snapshot {
            // Decode here, not in a property initialiser: BitmapFactory needs the layoutlib natives that the
            // snapshot loads, and fails with UnsatisfiedLinkError before then.
            val bitmap = sampleBitmap()
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(100.dp).clipToBounds().then(colorBlindness),
            )
        }
    }

    private fun sampleBitmap(): ImageBitmap = requireNotNull(javaClass.classLoader?.getResourceAsStream("sample.png"))
        .use { BitmapFactory.decodeStream(it) }
        .asImageBitmap()
}
