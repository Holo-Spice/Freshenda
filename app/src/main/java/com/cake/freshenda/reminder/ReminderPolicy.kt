package com.cake.freshenda.reminder

import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.FreshnessStatus
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ReminderDecision(
    val periodEpochDay: Long,
    val batches: List<FoodBatchEntity>,
)

object ReminderPolicy {
    fun decide(
        batches: List<FoodBatchEntity>,
        settings: UserSettings,
        nowEpochMillis: Long,
        zoneId: ZoneId,
    ): ReminderDecision? {
        if (!settings.remindersEnabled) return null
        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId)
        val preferred = LocalTime.of(settings.reminderHour, settings.reminderMinute)
        val periodDate = if (now.toLocalTime() >= preferred) now.toLocalDate() else now.toLocalDate().minusDays(1)
        val selected = batches.filter { batch ->
            val result = ExpiryCalculator.result(batch, nowEpochMillis, settings.normalAdvanceDays, settings.frozenAdvanceDays)
            val dateAllowed = when (batch.effectiveDeadlineKind) {
                "OPENED" -> settings.openedReminders
                "CUSTOM" -> settings.customDateReminders
                else -> true
            }
            dateAllowed && result.effective != null && (settings.dailySummary || result.status in setOf(FreshnessStatus.OVERDUE, FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON))
        }.sortedWith(compareBy<FoodBatchEntity> { it.effectiveDeadlineEpochMillis }.thenBy { it.id })

        return if (selected.isEmpty()) null else ReminderDecision(periodDate.toEpochDay(), selected)
    }
}
