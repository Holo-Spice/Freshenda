package com.cake.freshenda

import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.reminder.ReminderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = ZonedDateTime.of(2026, 9, 8, 20, 10, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun disabledReminderProducesNoDecision() {
        assertNull(ReminderPolicy.decide(listOf(batch()), UserSettings(remindersEnabled = false), now, zone))
    }

    @Test
    fun multipleDueBatchesProduceOneSummaryDecision() {
        val decision = ReminderPolicy.decide(listOf(batch(1), batch(2)), UserSettings(remindersEnabled = true), now, zone)
        assertNotNull(decision)
        assertEquals(2, decision!!.batches.size)
    }

    @Test
    fun notificationLeadTimesRemainIndependentFromTheThreeDayScreenWindow() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        fun dated(id: Long, days: Long, storage: String) = batch(id).copy(
            storageLocation = storage,
            effectiveDisplayDateEpochDay = today.plusDays(days).toEpochDay(),
            effectiveDeadlineEpochMillis = today.plusDays(days + 1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val settings = UserSettings(remindersEnabled = true, dailySummary = false, normalAdvanceDays = 1, frozenAdvanceDays = 7)
        val decision = ReminderPolicy.decide(listOf(dated(1, 2, "REFRIGERATED"), dated(2, 5, "FROZEN"), dated(3, 8, "FROZEN")), settings, now, zone)
        assertEquals(listOf(2L), decision!!.batches.map { it.id })
    }

    private fun batch(id: Long = 1) = FoodBatchEntity(
        id = id,
        foodDefinitionId = "spinach$id",
        displayName = "菠菜$id",
        iconKey = "food_spinach",
        quantityMilli = 1000,
        quantityUnit = "份",
        storageLocation = "REFRIGERATED",
        storageSection = "CRISPER",
        physicalState = "WHOLE",
        packagingState = "LOOSE",
        maturityState = "NOT_APPLICABLE",
        preparation = "NONE",
        purchasedAtEpochMillis = now - 2 * 86_400_000,
        stageStartedAtEpochMillis = now - 2 * 86_400_000,
        businessZoneId = zone.id,
        effectiveDeadlineEpochMillis = now + 3_600_000,
        effectiveAnchorEpochMillis = now - 2 * 86_400_000,
        effectiveDeadlineKind = "SUGGESTED",
        effectiveDisplayDateEpochDay = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone).toLocalDate().toEpochDay(),
        ruleDataVersion = "test",
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
