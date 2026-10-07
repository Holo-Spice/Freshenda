package com.cake.freshenda

import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.expiry.DueSchedule
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.model.StorageWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExpiryCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun labelDateUsesExclusiveNextMidnight() {
        val label = LocalDate.of(2026, 9, 9)
        val deadline = ExpiryCalculator.labelDeadline(label.toEpochDay(), zone.id)
        assertEquals(LocalDate.of(2026, 9, 10).atStartOfDay(zone).toInstant().toEpochMilli(), deadline)

        val batch = batch(
            anchor = LocalDate.of(2026, 9, 8).atStartOfDay(zone).toInstant().toEpochMilli(),
            deadline = deadline,
            displayDay = label.toEpochDay(),
            kind = "LABEL",
        )
        val noon = ZonedDateTime.of(2026, 9, 9, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(FreshnessStatus.TODAY, ExpiryCalculator.result(batch, noon).status)
        assertEquals("今天到期", ExpiryCalculator.result(batch, noon).statusText)
    }

    @Test
    fun calendarMonthDoesNotMeanThirtyDays() {
        val anchor = ZonedDateTime.of(2025, 1, 31, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val window = StorageWindow("SUPPORTED", 1, 1, 1, "MONTH", condition = "test", basis = "QUALITY_GUIDANCE", preparation = "NONE")
        val deadline = ExpiryCalculator.deadlineFromWindow(anchor, zone.id, window)
        assertEquals(ZonedDateTime.of(2025, 2, 28, 10, 0, 0, 0, zone).toInstant().toEpochMilli(), deadline)
    }

    @Test
    fun missingDeadlineStaysUnknown() {
        val result = ExpiryCalculator.result(batch(anchor = null, deadline = null, displayDay = null, kind = null), System.currentTimeMillis())
        assertEquals(FreshnessStatus.UNKNOWN, result.status)
        assertNull(result.effective)
    }

    @Test
    fun nearDatesTakePriorityAcrossStorageLocationsAndDateSources() {
        val today = LocalDate.of(2026, 10, 7)
        val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        fun dated(id: Long, days: Long, storage: String, kind: String) = batch(
            now - 86_400_000,
            ExpiryCalculator.labelDeadline(today.plusDays(days).toEpochDay(), zone.id),
            today.plusDays(days).toEpochDay(), kind,
        ).copy(id = id, storageLocation = storage)
        val pork = dated(1, 5, "FROZEN", "LABEL")
        val mushroom = dated(2, 2, "REFRIGERATED", "SUGGESTED")
        val spinach = dated(3, 2, "REFRIGERATED", "SUGGESTED")
        val noodles = dated(4, 3, "REFRIGERATED", "LABEL")
        val groups = DueSchedule.groups(listOf(pork, noodles, spinach, mushroom), now)
        assertEquals(listOf(2L, 3L, 4L, 1L), groups.values.flatten().map { it.first.id })
        assertEquals(listOf(FreshnessStatus.DUE_SOON, FreshnessStatus.OK), groups.keys.toList())
        assertEquals(FreshnessStatus.OK, ExpiryCalculator.result(pork, now).status)
        assertEquals("2天后", ExpiryCalculator.result(mushroom, now).statusText)
    }

    @Test
    fun calendarDayBoundariesAreTheSameForAllStorageLocations() {
        val today = LocalDate.of(2026, 10, 7)
        for (hour in listOf(0, 12, 23)) {
            val now = today.atTime(hour, 59).atZone(zone).toInstant().toEpochMilli()
            for (storage in listOf("FROZEN", "REFRIGERATED", "PANTRY")) {
                for ((days, expected) in listOf(-1L to FreshnessStatus.OVERDUE, 0L to FreshnessStatus.TODAY,
                    1L to FreshnessStatus.DUE_SOON, 3L to FreshnessStatus.DUE_SOON, 4L to FreshnessStatus.OK)) {
                    val date = today.plusDays(days)
                    val item = batch(now - 10 * 86_400_000, ExpiryCalculator.labelDeadline(date.toEpochDay(), zone.id), date.toEpochDay(), "LABEL")
                        .copy(storageLocation = storage)
                    assertEquals("$storage at $hour, $days days", expected, ExpiryCalculator.result(item, now).status)
                }
            }
        }
    }

    @Test
    fun undatedItemsDoNotInterruptChronologicalPlan() {
        val today = LocalDate.of(2026, 10, 7)
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val unknown = batch(null, null, null, null).copy(id = 2)
        val review = unknown.copy(id = 3, requiresDateReview = true)
        val dated = batch(now, ExpiryCalculator.labelDeadline(today.plusDays(10).toEpochDay(), zone.id), today.plusDays(10).toEpochDay(), "LABEL")
        assertEquals(listOf(1L, 3L, 2L), DueSchedule.groups(listOf(unknown, review, dated), now).values.flatten().map { it.first.id })
    }

    @Test
    fun referenceAndCustomDatesUsePlanningLanguage() {
        val today = LocalDate.of(2026, 10, 7)
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val date = today.plusDays(1).toEpochDay()
        for (kind in listOf("SUGGESTED", "CUSTOM", "OPENED", "THAWED")) {
            assertEquals("明天安排", ExpiryCalculator.result(batch(now, ExpiryCalculator.labelDeadline(date, zone.id), date, kind), now).statusText)
        }
    }

    private fun batch(anchor: Long?, deadline: Long?, displayDay: Long?, kind: String?) = FoodBatchEntity(
        id = 1,
        foodDefinitionId = "spinach",
        displayName = "菠菜",
        iconKey = "food_spinach",
        quantityMilli = 1000,
        quantityUnit = "份",
        storageLocation = "REFRIGERATED",
        storageSection = "CRISPER",
        physicalState = "WHOLE",
        packagingState = "LOOSE",
        maturityState = "NOT_APPLICABLE",
        preparation = "NONE",
        purchasedAtEpochMillis = anchor ?: 0,
        stageStartedAtEpochMillis = anchor ?: 0,
        businessZoneId = zone.id,
        effectiveDeadlineEpochMillis = deadline,
        effectiveAnchorEpochMillis = anchor,
        effectiveDeadlineKind = kind,
        effectiveDisplayDateEpochDay = displayDay,
        ruleDataVersion = "test",
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
