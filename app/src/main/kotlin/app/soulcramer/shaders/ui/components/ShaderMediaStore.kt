package app.soulcramer.shaders.ui.components

import android.graphics.Bitmap
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * A media file in [ShaderMediaStore], and whether it is a video.
 */
internal data class StoredMedia(val file: PlatformFile, val isVideo: Boolean)

/**
 * Keeps one picked image or video for each shader in the app cache, so that a shader screen shows it again later.
 *
 * A key contains only letters and digits. The media of a key is `shader-media/<key>-<save time in ms>.<extension>`.
 */
internal object ShaderMediaStore {
    private const val TEMPORARY_EXTENSION = "tmp"
    private val keyPattern = Regex("[A-Za-z0-9]+")
    private val extensionPattern = Regex("[a-z0-9]+")

    // A picker result after an activity recreation can arrive while the restore of the same key runs.
    private val mutex = Mutex()

    private val mediaDirectory: PlatformFile get() = FileKit.cacheDir / "shader-media"
    private val shareDirectory: PlatformFile get() = FileKit.cacheDir / "shader-share"

    /**
     * Copies [picked] as the media of [key], deletes the previous media of [key], and returns the copy.
     */
    suspend fun save(key: String, picked: PlatformFile): StoredMedia = fileOperation(key) {
        val directory = mediaDirectory.also { it.createDirectories() }
        // A new name for each pick, so that the Coil memory cache and the player never show the previous media.
        val target = directory / "$key-${System.currentTimeMillis()}.${picked.mediaExtension()}"
        // A crash during the copy leaves only a temporary file, which the next save of the key deletes.
        val temporary = directory / "${target.name}.$TEMPORARY_EXTENSION"
        picked.copyTo(temporary)
        temporary.atomicMove(target)
        directory.deleteFilesOf(key, except = target)
        target.toStoredMedia()
    }

    /**
     * Returns the newest media of [key], or null when [key] has no media.
     */
    suspend fun restore(key: String): StoredMedia? = fileOperation(key) {
        val directory = mediaDirectory
        if (!directory.exists()) return@fileOperation null
        directory.filesOf(key)
            .filter { it.extension != TEMPORARY_EXTENSION }
            .maxByOrNull { it.nameWithoutExtension.substringAfterLast('-').toLongOrNull() ?: 0L }
            ?.toStoredMedia()
    }

    /**
     * Writes [image] to a PNG file to share for [key], deletes the previous share files of [key], and returns the file.
     */
    suspend fun saveShareImage(key: String, image: ImageBitmap): PlatformFile = fileOperation(key) {
        val bytes = ByteArrayOutputStream().use { stream ->
            if (!image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw IOException("The PNG encoding of the shaded image failed")
            }
            stream.toByteArray()
        }
        val directory = shareDirectory.also { it.createDirectories() }
        val target = directory / "$key-${System.currentTimeMillis()}.png"
        target.write(bytes)
        directory.deleteFilesOf(key, except = target)
        target
    }

    private suspend fun <T> fileOperation(key: String, block: suspend CoroutineScope.() -> T): T {
        // The key goes into file names, so a separator or a path segment in it could reach other files.
        require(keyPattern.matches(key)) { "A media key must contain only letters and digits: $key" }
        return mutex.withLock { withContext(Dispatchers.IO, block) }
    }

    private fun PlatformFile.mediaExtension(): String {
        // A document provider supplies the name, so a file name takes only an extension of letters and digits.
        extension.lowercase().takeIf(extensionPattern::matches)?.let { return it }
        val mimeType = mimeType() ?: throw IOException("The picked file has no extension and no media type: $name")
        return MimeTypeMap.getSingleton().getExtensionFromMimeType("${mimeType.primaryType}/${mimeType.subtype}")
            ?: throw IOException("The media type $mimeType has no file extension")
    }

    private fun PlatformFile.filesOf(key: String): List<PlatformFile> = list().filter { it.name.startsWith("$key-") }

    private suspend fun PlatformFile.deleteFilesOf(key: String, except: PlatformFile) {
        filesOf(key).filter { it.name != except.name }.forEach { it.delete(mustExist = false) }
    }

    private fun PlatformFile.toStoredMedia(): StoredMedia =
        StoredMedia(file = this, isVideo = mimeType()?.primaryType == "video")
}
