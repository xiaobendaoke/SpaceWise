package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun Bitmap.toSquareCenterCropped(): Bitmap {
    val size = min(width, height)
    val x = (width - size) / 2
    val y = (height - size) / 2
    return Bitmap.createBitmap(this, x, y, size, size)
}

fun Bitmap.toThumbnail(targetSizePx: Int): Bitmap {
    val square = toSquareCenterCropped()
    return Bitmap.createScaledBitmap(square, targetSizePx, targetSizePx, true)
}

fun Bitmap.scaleDownToMaxDimension(maxDimensionPx: Int): Bitmap {
    if (maxDimensionPx <= 0) return this
    val maxDim = max(width, height)
    if (maxDim <= maxDimensionPx) return this
    val scale = maxDimensionPx.toFloat() / maxDim.toFloat()
    val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

fun Bitmap.toCover(targetWidthPx: Int, targetHeightPx: Int): Bitmap {
    if (targetWidthPx <= 0 || targetHeightPx <= 0) return this
    val scale = max(targetWidthPx.toFloat() / width, targetHeightPx.toFloat() / height)
    val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    val x = ((scaledWidth - targetWidthPx) / 2).coerceAtLeast(0)
    val y = ((scaledHeight - targetHeightPx) / 2).coerceAtLeast(0)
    val cropWidth = targetWidthPx.coerceAtMost(scaledWidth)
    val cropHeight = targetHeightPx.coerceAtMost(scaledHeight)
    return Bitmap.createBitmap(scaled, x, y, cropWidth, cropHeight)
}

fun loadThumbnailFromUri(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            val sampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                reqWidth = targetSizePx,
                reqHeight = targetSizePx
            )
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } ?: return null

        bitmap.toThumbnail(targetSizePx)
    } catch (_: Throwable) {
        null
    }
}

fun loadBitmapFromUri(context: Context, uri: Uri, maxDimensionPx: Int): Bitmap? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                if (maxDimensionPx > 0) {
                    val maxDim = max(info.size.width, info.size.height).coerceAtLeast(1)
                    if (maxDim > maxDimensionPx) {
                        val scale = maxDimensionPx.toFloat() / maxDim.toFloat()
                        val targetW = (info.size.width * scale).roundToInt().coerceAtLeast(1)
                        val targetH = (info.size.height * scale).roundToInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            val sampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                reqWidth = maxDimensionPx,
                reqHeight = maxDimensionPx
            )
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } ?: return null

        bitmap.scaleDownToMaxDimension(maxDimensionPx)
    } catch (_: Throwable) {
        null
    }
}

fun loadCoverFromUri(context: Context, uri: Uri, targetWidthPx: Int, targetHeightPx: Int): Bitmap? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = max(
                    targetWidthPx.toFloat() / info.size.width,
                    targetHeightPx.toFloat() / info.size.height
                )
                val scaledWidth = (info.size.width * scale).roundToInt().coerceAtLeast(1)
                val scaledHeight = (info.size.height * scale).roundToInt().coerceAtLeast(1)
                decoder.setTargetSize(scaledWidth, scaledHeight)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            val sampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                reqWidth = targetWidthPx,
                reqHeight = targetHeightPx
            )
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } ?: return null

        bitmap.toCover(targetWidthPx, targetHeightPx)
    } catch (_: Throwable) {
        null
    }
}

fun loadThumbnailFromInternalPath(context: Context, relativePath: String, targetSizePx: Int): Bitmap? {
    return try {
        val file = File(context.filesDir, relativePath)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            reqWidth = targetSizePx,
            reqHeight = targetSizePx
        )
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        bmp.toThumbnail(targetSizePx)
    } catch (_: Throwable) {
        null
    }
}

fun loadBitmapFromInternalPath(context: Context, relativePath: String, maxDimensionPx: Int): Bitmap? {
    return try {
        val file = File(context.filesDir, relativePath)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            reqWidth = maxDimensionPx,
            reqHeight = maxDimensionPx
        )
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        bmp.scaleDownToMaxDimension(maxDimensionPx)
    } catch (_: Throwable) {
        null
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var inSampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqHeight) {
        inSampleSize *= 2
    }
    return inSampleSize.coerceAtLeast(1)
}
