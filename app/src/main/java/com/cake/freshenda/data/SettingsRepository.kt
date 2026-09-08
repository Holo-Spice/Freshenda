package com.cake.freshenda.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class UserSettings(
    val remindersEnabled: Boolean = false,
    val dailySummary: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val normalAdvanceDays: Int = 1,
    val frozenAdvanceDays: Int = 7,
    val openedReminders: Boolean = true,
    val customDateReminders: Boolean = true,
    val recentFoodIds: List<String> = emptyList(),
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { preferences ->
        UserSettings(
            remindersEnabled = preferences[KEY_ENABLED] ?: false,
            dailySummary = preferences[KEY_DAILY] ?: false,
            reminderHour = preferences[KEY_HOUR] ?: 20,
            reminderMinute = preferences[KEY_MINUTE] ?: 0,
            normalAdvanceDays = preferences[KEY_NORMAL_DAYS] ?: 1,
            frozenAdvanceDays = preferences[KEY_FROZEN_DAYS] ?: 7,
            openedReminders = preferences[KEY_OPENED] ?: true,
            customDateReminders = preferences[KEY_CUSTOM] ?: true,
            recentFoodIds = preferences[KEY_RECENT].orEmpty().split(',').filter { it.isNotBlank() },
        )
    }

    suspend fun setRemindersEnabled(value: Boolean) = context.settingsDataStore.edit { it[KEY_ENABLED] = value }
    suspend fun setDailySummary(value: Boolean) = context.settingsDataStore.edit { it[KEY_DAILY] = value }
    suspend fun setOpenedReminders(value: Boolean) = context.settingsDataStore.edit { it[KEY_OPENED] = value }
    suspend fun setCustomDateReminders(value: Boolean) = context.settingsDataStore.edit { it[KEY_CUSTOM] = value }
    suspend fun setReminderHour(value: Int) = context.settingsDataStore.edit { it[KEY_HOUR] = value.coerceIn(0, 23) }
    suspend fun recordRecent(foodId: String) = context.settingsDataStore.edit { preferences ->
        val ids = preferences[KEY_RECENT].orEmpty().split(',').filter { it.isNotBlank() && it != foodId }
        preferences[KEY_RECENT] = (listOf(foodId) + ids).take(12).joinToString(",")
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEY_DAILY = booleanPreferencesKey("daily_summary")
        val KEY_HOUR = intPreferencesKey("reminder_hour")
        val KEY_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_NORMAL_DAYS = intPreferencesKey("normal_advance_days")
        val KEY_FROZEN_DAYS = intPreferencesKey("frozen_advance_days")
        val KEY_OPENED = booleanPreferencesKey("opened_reminders")
        val KEY_CUSTOM = booleanPreferencesKey("custom_date_reminders")
        val KEY_RECENT = stringPreferencesKey("recent_food_ids")
    }
}
