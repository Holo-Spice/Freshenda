package com.cake.freshenda.expiry

import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FreshnessStatus

object DueSchedule {
    /** Date order is independent of notification preferences and storage location. */
    fun groups(batches: List<FoodBatchEntity>, now: Long): Map<FreshnessStatus, List<Pair<FoodBatchEntity, ExpiryResult>>> =
        batches.map { it to ExpiryCalculator.result(it, now) }
            .sortedWith(compareBy<Pair<FoodBatchEntity, ExpiryResult>> { sectionOrder(it.second.status) }
                .thenBy { it.second.effective?.deadlineEpochMillis ?: Long.MAX_VALUE }
                .thenBy { it.first.purchasedAtEpochMillis }
                .thenBy { it.first.id })
            .groupBy { it.second.status }

    private fun sectionOrder(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.OVERDUE -> 0
        FreshnessStatus.TODAY -> 1
        FreshnessStatus.DUE_SOON -> 2
        FreshnessStatus.OK -> 3
        FreshnessStatus.REVIEW -> 4
        FreshnessStatus.UNKNOWN -> 5
    }
}
