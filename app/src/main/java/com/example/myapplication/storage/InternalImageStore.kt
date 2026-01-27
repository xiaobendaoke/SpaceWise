package com.example.myapplication.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object InternalImageStore {
    private const val IMAGES_DIR = "images"

    fun resolveFile(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun createTempCameraUri(context: Context): Uri {
        val tempFile = File.createTempFile("capture_", ".jpg", context.cacheDir)
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, tempFile)
    }

    fun persistFromUri(context: Context, sourceUri: Uri, preferredExtension: String = ".jpg"): String? {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
            val ext = preferredExtension.ifBlank { ".jpg" }.let { if (it.startsWith(".")) it else ".$it" }
            val filename = UUID.randomUUID().toString() + ext
            val dest = File(imagesDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: return null
            "$IMAGES_DIR/$filename"
        } catch (_: Throwable) {
            null
        }
    }

    fun persistBitmap(context: Context, bitmap: Bitmap, preferredExtension: String = ".jpg", quality: Int = 92): String? {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
            val ext = preferredExtension.ifBlank { ".jpg" }.let { if (it.startsWith(".")) it else ".$it" }
            val filename = UUID.randomUUID().toString() + ext
            val dest = File(imagesDir, filename)
            FileOutputStream(dest).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), output)
            }
            "$IMAGES_DIR/$filename"
        } catch (_: Throwable) {
            null
        }
    }

    fun copyFromGalleryToInternal(context: Context, uri: Uri): String? {
        val extension = guessExtension(context.contentResolver, uri) ?: ".jpg"
        return persistFromUri(context, uri, extension)
    }

    private fun guessExtension(resolver: ContentResolver, uri: Uri): String? {
        return try {
            val type = resolver.getType(uri) ?: return null
            when (type.lowercase()) {
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                "image/jpeg", "image/jpg" -> ".jpg"
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }
}

