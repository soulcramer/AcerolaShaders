package app.soulcramer.shaders.ui.components

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.soulcramer.shaders.app.R
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Shows an image that opens the photo picker on click, with [effect] applied to the drawn image.
 */
@Composable
internal fun ShaderImage(
    modifier: Modifier = Modifier,
    effect: Modifier = Modifier,
) {
    var imageUri: Any? by remember { mutableStateOf(R.drawable.ic_launcher_background) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) {
        if (it != null) {
            Log.d("PhotoPicker", "Selected URI: $it")
            imageUri = it
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(imageUri)
            .crossfade(enable = true)
            .build(),
        contentDescription = "",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clickable {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            }
            .then(effect),
    )
}
