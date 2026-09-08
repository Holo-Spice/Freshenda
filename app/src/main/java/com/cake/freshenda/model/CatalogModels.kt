package com.cake.freshenda.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodCatalog(
    val schemaVersion: Int,
    val dataVersion: String,
    val checkedAt: String,
    val purpose: String,
    val categories: List<FoodCategory>,
    val profiles: List<StorageProfile>,
    val foods: List<FoodDefinition>,
    val sources: List<CatalogSource>,
)

@Serializable
data class FoodCategory(val id: String, val name: String)

@Serializable
data class FoodDefinition(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val categoryId: String,
    val profileId: String,
    val iconKey: String,
    val iconBrief: String,
    val iconDeliveryStatus: String,
    val defaultStorage: String,
    val defaultQuantityUnit: String,
    val evidence: EvidenceSummary,
    val requiredState: String,
)

@Serializable
data class EvidenceSummary(
    val refrigerated: String,
    val frozen: String,
    val pantry: String,
)

@Serializable
data class StorageProfile(
    val id: String,
    val title: String,
    val refrigerated: StorageWindow,
    val frozen: StorageWindow,
    val pantry: StorageWindow,
    val openedRefrigerated: StorageWindow? = null,
)

@Serializable
data class StorageWindow(
    val status: String,
    val defaultValue: Int? = null,
    val rangeMin: Int? = null,
    val rangeMax: Int? = null,
    val unit: String? = null,
    val sourceIds: List<String> = emptyList(),
    val sourceItem: String = "",
    val condition: String,
    val basis: String,
    val preparation: String,
    val selectionPolicy: String = "",
)

@Serializable
data class CatalogSource(
    val id: String,
    val title: String,
    val url: String,
    val sourceDate: String? = null,
    val checkedAt: String,
    val scope: String,
)
