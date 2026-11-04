package app.soulcramer.shaders.ui.components

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import app.soulcramer.shaders.app.R
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.ShareResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
import io.github.vinceglb.filekit.exceptions.FileKitException
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

private const val TAG = "ShaderMedia"
private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

/**
 * Shows the stored image or video of [mediaKey], with [effect] applied to the drawn pixels.
 *
 * A click picks a new image or video for [mediaKey]. When the media is a stored image, a button shares the image
 * with the shader applied.
 */
@Composable
internal fun ShaderMedia(
    mediaKey: String,
    modifier: Modifier = Modifier,
    effect: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        // App Startup does not run in previews, so FileKit has no context there.
        MediaImage(model = R.drawable.ic_launcher_background, modifier = modifier.mediaFrame(), effect = effect)
    } else {
        PickableShaderMedia(mediaKey = mediaKey, modifier = modifier, effect = effect)
    }
}

@Composable
private fun PickableShaderMedia(
    mediaKey: String,
    modifier: Modifier = Modifier,
    effect: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var media: StoredMedia? by remember(mediaKey) { mutableStateOf(null) }

    LaunchedEffect(mediaKey) {
        logFailure("The media restore failed") {
            ShaderMediaStore.restore(mediaKey)?.let { media = it }
        }
    }

    val picker = rememberFilePickerLauncher(
        type = FileKitType.ImageAndVideo,
        onError = { Log.w(TAG, "The media pick failed", it) },
    ) { picked ->
        if (pi  cked != null) {
            scope.launch {
                logFailure("The media save failed") { media = ShaderMediaStore.save(mediaKey, picked) }
            }
        }
    }
    val shareLauncher = rememberShareFileLauncher(onError = { Log.w(TAG, "The share failed", it) })

    Box(modifier) {
        val current = media
        if (current?.isVideo == true) {
            MediaVideo(
                file = current.file,
                modifier = Modifier.mediaFrame().clickable { picker.launch() },
                effect = effect,
            )
        } else {
            MediaImage(
                model = current?.file ?: R.drawable.ic_launcher_background,
                // The record sits before the effect, so that the recorded layer holds the shaded pixels.
                modifier = Modifier.mediaFrame().clickable { picker.launch() }.recordInto(graphicsLayer),
                effect = effect,
            )
        }

        if (current != null && !current.isVideo) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        logFailure("The share failed") { shareShadedImage(mediaKey, graphicsLayer, shareLauncher) }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share the shaded image")
            }
        }
    }
}

@Composable
private fun MediaImage(
    model: Any,
    modifier: Modifier = Modifier,
    effect: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(model)
            .crossfade(enable = true)
            .build(),
        contentDescription = "",
        contentScale = ContentScale.Crop,
        modifier = modifier.then(effect),
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun MediaVideo(
    file: PlatformFile,
    modifier: Modifier = Modifier,
    effect: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player: ExoPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(file, lifecycle) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(file.path).toUri()))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
        // The lifecycle replays the events up to its current state, so a resumed screen starts the video at once.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        player = exoPlayer
        onDispose {
            lifecycle.removeObserver(observer)
            player = null
            exoPlayer.release()
        }
    }

    val videoSize = rememberPresentationState(player).videoSizeDp
    Box(modifier, contentAlignment = Alignment.Center) {
        PlayerSurface(
            player = player,
            // A TextureView draws into the Compose layer, so the effect changes its pixels. A SurfaceView does not.
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            // The effect sits on the surface, so that the shader size is the video size, not the letterboxed frame.
            modifier = Modifier
                .aspectRatio(videoSize?.let { it.width / it.height } ?: DEFAULT_VIDEO_ASPECT_RATIO)
                .then(effect),
        )
    }
}

private suspend fun shareShadedImage(
    key: String,
    graphicsLayer: GraphicsLayer,
    shareLauncher: ShareResultLauncher,
) {
    // The layer has no size until Coil draws the image, and a bitmap needs a width and a height above zero.
    if (graphicsLayer.size.width == 0 || graphicsLayer.size.height == 0) {
        Log.w(TAG, "The image is not drawn yet, so there is nothing to share")
        return
    }
    val image = graphicsLayer.toImageBitmap()
    shareLauncher.launch(ShaderMediaStore.saveShareImage(key, image))
}

private fun Modifier.mediaFrame(): Modifier = fillMaxWidth().heightIn(max = 300.dp)

private fun Modifier.recordInto(graphicsLayer: GraphicsLayer): Modifier = drawWithContent {
    graphicsLayer.record { this@drawWithContent.drawContent() }
    drawLayer(graphicsLayer)
}

// FileKit reports file errors as IOException or FileKitException. A content provider can refuse a read with a
// SecurityException.
private inline fun logFailure(message: String, block: () -> Unit) {
    try {
        block()
    } catch (exception: IOException) {
        Log.w(TAG, message, exception)
    } catch (exception: FileKitException) {
        Log.w(TAG, message, exception)
    } catch (exception: SecurityException) {
        Log.w(TAG, message, exception)
    }
}
