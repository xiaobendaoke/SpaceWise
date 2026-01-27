/**
 * OCR 文字识别组件。
 *
 * 职责：
 * - 使用 ML Kit 识别图片中的中文字符。
 *
 * 上层用途：
 * - 在物品编辑页面等场景，帮助用户快速录入物品信息。
 */
package com.example.myapplication.ocr

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object OcrRecognizer {
    suspend fun recognizeFromInternalPath(context: Context, relativePath: String): String? {
        return try {
            val file = File(context.filesDir, relativePath)
            if (!file.exists()) return null
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            val text = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result -> cont.resume(result.text) }
                    .addOnFailureListener { err -> cont.resumeWithException(err) }
            }
            text?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }
}
