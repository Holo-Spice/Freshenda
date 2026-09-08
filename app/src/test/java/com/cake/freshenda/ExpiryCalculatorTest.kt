package com.cake.freshenda

import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
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
