package com.cake.freshenda.data.catalog

import android.content.Context
import com.cake.freshenda.model.FoodCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CatalogLoader(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): FoodCatalog = withContext(Dispatchers.IO) {
        val text = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
        json.decodeFromString<FoodCatalog>(text)
    }

    private companion object {
        const val FILE_NAME = "food_catalog.v1.json"
    }
}
