/**
 * 示例封面图资源。
 *
 * 职责：
 * - 提供应用预置的空间示例图案。
 *
 * 上层用途：
 * - 在应用首次启动或创建示例数据时生成默认空间封面。
 */
package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils

object SampleCovers {
    val livingRoom: Bitmap by lazy { createCover(0xFFCCBBA8.toInt(), 0xFFB59F88.toInt(), 0xFFF7F1E8.toInt()) }
    val bedroom: Bitmap by lazy { createCover(0xFFCBB6A0.toInt(), 0xFF9E8975.toInt(), 0xFFF5EDE3.toInt()) }
    val office: Bitmap by lazy { createCover(0xFFD7DFE8.toInt(), 0xFF9DA9B7.toInt(), 0xFFF3F6FA.toInt()) }

    private fun createCover(primary: Int, accent: Int, background: Int): Bitmap {
        val width = 900
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(background, ColorUtils.blendARGB(primary, background, 0.55f)),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        paint.color = ColorUtils.blendARGB(background, primary, 0.25f)
        canvas.drawRoundRect(RectF(0f, height * 0.55f, width.toFloat(), height.toFloat()), 48f, 48f, paint)

        paint.color = primary
        canvas.drawRoundRect(RectF(width * 0.10f, height * 0.42f, width * 0.55f, height * 0.62f), 46f, 46f, paint)

        paint.color = accent
        canvas.drawRoundRect(RectF(width * 0.52f, height * 0.50f, width * 0.85f, height * 0.66f), 42f, 42f, paint)

        paint.color = ColorUtils.blendARGB(accent, 0xFFFFFFFF.toInt(), 0.35f)
        canvas.drawCircle(width * 0.25f, height * 0.32f, 26f, paint)
        canvas.drawCircle(width * 0.65f, height * 0.28f, 18f, paint)

        return bitmap
    }
}
