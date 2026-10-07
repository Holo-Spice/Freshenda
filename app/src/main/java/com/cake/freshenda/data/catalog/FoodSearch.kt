package com.cake.freshenda.data.catalog

import com.cake.freshenda.model.FoodDefinition
import java.util.Locale

object FoodSearch {
    fun find(foods: List<FoodDefinition>, query: String, category: String = "ALL"): List<FoodDefinition> {
        val term = normalize(query)
        val inCategory = foods.filter { category == "ALL" || it.categoryId == category }
        if (term.isEmpty()) return inCategory
        return inCategory.mapNotNull { food ->
            val name = normalize(food.name)
            val aliases = food.aliases.map(::normalize)
            val rank = when {
                name == term -> 0
                term in aliases -> 1
                name.startsWith(term) -> 2
                name.contains(term) -> 3
                aliases.any { it.contains(term) } -> 4
                food.id.lowercase(Locale.ROOT).contains(term) -> 5
                else -> return@mapNotNull null
            }
            food to rank
        }.sortedBy { it.second }.map { it.first }
    }

    private fun normalize(value: String) = value.filterNot(Char::isWhitespace).lowercase(Locale.ROOT)
}
