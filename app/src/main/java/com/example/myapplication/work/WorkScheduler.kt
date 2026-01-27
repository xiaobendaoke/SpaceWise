/**
 * 后台任务调度中心。
 *
 * 职责：
 * - 调度并管理全应用的后台任务（如定期过期检查）。
 *
 * 上层用途：
 * - 在 `MainActivity` 启动时初始化，确保任务调度按计划开启。
 */
package com.example.myapplication.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_EXPIRY_WORK = "expiry_check"

    fun scheduleExpiryCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_EXPIRY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

