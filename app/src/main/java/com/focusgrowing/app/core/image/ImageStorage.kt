package com.focusgrowing.app.core.image

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * URI-first handling of user images (spec §16): we keep a reference to the original picture
 * and take a persistable read permission. Only when a provider does not allow that
 * (some gallery apps) we copy the file into private app storage, so the background keeps
 * working. Photos never leave the device.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val folder: File get() = File(context.filesDir, FOLDER).apply { mkdirs() }

    /** Returns a URI string that will still be readable after an app restart, or null on failure. */
    suspend fun persist(uri: Uri): String? = withContext(Dispatchers.IO) {
        if (isOwned(uri)) return@withContext uri.toString()
        val persisted = try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: SecurityException) {
            false
        }
        if (persisted) uri.toString() else copyToPrivateStorage(uri)?.toString()
    }

    /** Target file for the system camera app. */
    fun newCameraUri(): Uri {
        val file = File(folder, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun discardCameraUri(uri: Uri) {
        runCatching { if (isOwned(uri)) resolver.delete(uri, null, null) }
    }

    suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.')?.take(40) ?: "My photo"
    }

    /** Releases our access / deletes our private copy when a background is removed. */
    suspend fun release(uriString: String) = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        if (isOwned(uri)) {
            runCatching { resolver.delete(uri, null, null) }
        } else {
            runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        Unit
    }

    private fun isOwned(uri: Uri): Boolean = uri.authority == "${context.packageName}.fileprovider"

    private fun copyToPrivateStorage(source: Uri): Uri? = runCatching {
        val file = File(folder, "copy_${System.currentTimeMillis()}.jpg")
        resolver.openInputStream(source)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    private companion object {
        const val FOLDER = "backgrounds"
    }
}
