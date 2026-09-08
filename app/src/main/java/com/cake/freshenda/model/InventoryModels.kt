package com.cake.freshenda.model

enum class StorageLocation { REFRIGERATED, FROZEN, PANTRY }
enum class StorageSection { UPPER, LOWER, CRISPER, FREEZER_DRAWER, SHELF }
enum class BatchStatus { ACTIVE, CONSUMED, DISCARDED }
enum class PackagingState { LOOSE, UNOPENED, OPENED }
enum class FoodPhysicalState { WHOLE, CUT, GROUND, COOKED, PREPARED_FOR_FREEZING }
enum class MaturityState { NOT_APPLICABLE, UNRIPE, RIPE, UNKNOWN }
enum class DatePrecision { DATE_ONLY, DATE_TIME }
enum class DateBasis { SUGGESTED, LABEL, CUSTOM, NONE }
enum class DeadlineKind { LABEL, SUGGESTED, OPENED, CUSTOM, THAWED }
enum class FreshnessStatus { OVERDUE, TODAY, DUE_SOON, OK, UNKNOWN, REVIEW }

data class DeadlineCandidate(
    val kind: DeadlineKind,
    val anchorEpochMillis: Long,
    val deadlineEpochMillis: Long,
    val displayDateEpochDay: Long,
    val description: String,
)

data class ExpiryResult(
    val effective: DeadlineCandidate?,
    val status: FreshnessStatus,
    val fractionRemaining: Float,
    val statusText: String,
)

data class AddBatchDraft(
    val food: FoodDefinition,
    val storageLocation: StorageLocation,
    val storageSection: StorageSection,
    val quantityMilli: Long,
    val quantityUnit: String,
    val stageStartedEpochMillis: Long,
    val businessZoneId: String,
    val dateBasis: DateBasis,
    val labelDateEpochDay: Long? = null,
    val customDateEpochDay: Long? = null,
    val packagingState: PackagingState = PackagingState.LOOSE,
    val physicalState: FoodPhysicalState = FoodPhysicalState.WHOLE,
    val maturityState: MaturityState = MaturityState.NOT_APPLICABLE,
    val completedPreparation: String = "NONE",
)
