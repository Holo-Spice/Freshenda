package com.cake.freshenda.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "food_batches",
    indices = [
        Index(value = ["status", "effectiveDeadlineEpochMillis"]),
        Index(value = ["foodDefinitionId"]),
    ],
)
data class FoodBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodDefinitionId: String,
    val displayName: String,
    val iconKey: String,
    val quantityMilli: Long,
    val quantityUnit: String,
    val status: String = "ACTIVE",
    val storageLocation: String,
    val storageSection: String,
    val physicalState: String,
    val packagingState: String,
    val maturityState: String,
    val preparation: String,
    val purchasedAtEpochMillis: Long,
    val stageStartedAtEpochMillis: Long,
    val openedAtEpochMillis: Long? = null,
    val thawedAtEpochMillis: Long? = null,
    val datePrecision: String = "DATE_ONLY",
    val businessZoneId: String,
    val labelDateEpochDay: Long? = null,
    val labelStorageScope: String? = null,
    val customDeadlineEpochMillis: Long? = null,
    val suggestedDeadlineEpochMillis: Long? = null,
    val openedDeadlineEpochMillis: Long? = null,
    val thawedDeadlineEpochMillis: Long? = null,
    val effectiveDeadlineEpochMillis: Long? = null,
    val effectiveAnchorEpochMillis: Long? = null,
    val effectiveDeadlineKind: String? = null,
    val effectiveDisplayDateEpochDay: Long? = null,
    val ruleSnapshot: String? = null,
    val ruleDataVersion: String,
    val deadlineRevision: Long = 1,
    val requiresDateReview: Boolean = false,
    val preFreezeRemainingSeconds: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
@Entity(
    tableName = "batch_changes",
    indices = [Index(value = ["batchId", "changedAtEpochMillis"])],
)
data class BatchChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    val kind: String,
    val quantityDeltaMilli: Long = 0,
    val note: String,
    val changedAtEpochMillis: Long,
)

@Serializable
@Entity(tableName = "reminder_deliveries")
data class ReminderDeliveryEntity(
    @PrimaryKey val deliveryKey: String,
    val periodEpochDay: Long,
    val summaryHash: String,
    val deliveredAtEpochMillis: Long,
)

@Serializable
@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val iconKey: String,
    val defaultQuantityUnit: String,
    val refrigeratedDays: Int? = null,
    val frozenDays: Int? = null,
    val pantryDays: Int? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
