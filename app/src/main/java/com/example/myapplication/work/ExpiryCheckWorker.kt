package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.notifications.Notifications
import com.example.myapplication.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).settings.first()
        if (!settings.remindersEnabled) return Result.success()

        val now = System.currentTimeMillis()
        val upper = now + TimeUnit.DAYS.toMillis(settings.daysBeforeExpiry.toLong())
        val dao = AppDatabase.get(applicationContext).dao()
        val expiring = dao.listExpiringItems(upperBoundEpochMs = upper)

        if (expiring.isEmpty()) return Result.success()

        val expiredCount = expiring.count { it.expiryDateEpochMs <= now }
        val soonCount = expiring.size - expiredCount

        val title = when {
            expiredCount > 0 && soonCount > 0 -> "有 $expiredCount 个已过期，$soonCount 个临近过期"
            expiredCount > 0 -> "有 $expiredCount 个已过期物品"
            else -> "有 $soonCount 个临近过期物品"
        }

        val preview = expiring.take(5).joinToString("\n") { row ->
            "${row.spaceName} > ${row.spotName}：${row.itemName}"
        }

        Notifications.showExpiryNotification(
            context = applicationContext,
            title = title,
            text = preview
        )

        return Result.success()
    }
}

