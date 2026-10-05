package app.soulcramer.shaders

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test

class RenderPassChainScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
    )

    // The 8-bit intermediate keeps 17 levels of red in [0, 1 / 16], so the output shows 17 bands.
    @Test
    fun ramp() {
        paparazzi.snapshot {
            // Shaders need the layoutlib natives that the snapshot loads.
            val chain = remember { RenderPassChain(RuntimeShader(RAMP_SHADER), RuntimeShader(SCALE_SHADER)) }
            Box(
                Modifier
                    .size(100.dp)
                    .graphicsLayer { renderEffect = chain.effect(size.width, size.height, Unit) {} }
                    .background(Color.Black),
            )
        }
    }

    @Test
    fun blendPlus() {
        paparazzi.snapshot {
            val chain = remember { RenderPassChain(RuntimeShader(GREEN_RAMP_SHADER)) }
            Box(
                Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        renderEffect = RenderEffect.createBlendModeEffect(
                            RenderEffect.createOffsetEffect(0f, 0f),
                            chain.effect(size.width, size.height, Unit) {}.asAndroidRenderEffect(),
                            BlendMode.PLUS,
                        ).asComposeRenderEffect()
                    }
                    .background(Color(0xFF400000)),
            )
        }
    }

    private companion object {
        const val RAMP_SHADER = """
            uniform float2 size;
            uniform shader composable;

            half4 main(float2 coord) {
                return half4(coord.x / size.x / 16.0, 0.0, 0.0, 1.0);
            }
        """

        const val SCALE_SHADER = """
            uniform float2 size;
            uniform shader composable;

            half4 main(float2 coord) {
                half4 colour = composable.eval(coord);
                return half4(min(colour.r * 16.0, 1.0), 0.0, 0.0, 1.0);
            }
        """

        const val GREEN_RAMP_SHADER = """
            uniform float2 size;
            uniform shader composable;

            half4 main(float2 coord) {
                return half4(0.0, coord.x / size.x, 0.0, 1.0);
            }
        """
    }
}
