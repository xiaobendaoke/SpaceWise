/**
 * 用户设置仓库。
 *
 * 职责：
 * - 基于 DataStore 持久化保存用户的偏好设置。
 *
 * 上层用途：
 * - 被 `SpaceViewModel` 及其它逻辑组件调用，获取个性化配置信息。
 */
package com.example.myapplication.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val remindersEnabled: Boolean = true,
    val daysBeforeExpiry: Int = 3,
    val hasSeenOnboarding: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val daysBeforeExpiry = intPreferencesKey("days_before_expiry")
        val hasSeenOnboarding = booleanPreferencesKey("has_seen_onboarding")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            remindersEnabled = prefs[Keys.remindersEnabled] ?: true,
            daysBeforeExpiry = (prefs[Keys.daysBeforeExpiry] ?: 3).coerceIn(0, 30),
            hasSeenOnboarding = prefs[Keys.hasSeenOnboarding] ?: false,
        )
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = enabled }
    }

    suspend fun setDaysBeforeExpiry(days: Int) {
        context.dataStore.edit { it[Keys.daysBeforeExpiry] = days.coerceIn(0, 30) }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { it[Keys.hasSeenOnboarding] = seen }
    }
}
