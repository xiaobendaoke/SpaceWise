/**
 * 系统通知管理器。
 *
 * 职责：
 * - 声明通知渠道。
 * - 构建并发送本地提醒通知。
 *
 * 上层用途：
 * - 被 `ExpiryCheckWorker` 调用，在发现即将过期物品时提醒用户。
 */
package com.example.myapplication.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.R

object Notifications {
    const val CHANNEL_EXPIRY = "expiry"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_EXPIRY,
            "到期提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "物品过期/临近过期提醒"
        }
        manager.createNotificationChannel(channel)
    }

    fun showExpiryNotification(context: Context, title: String, text: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

