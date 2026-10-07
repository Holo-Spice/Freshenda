package com.cake.freshenda

import com.cake.freshenda.data.catalog.FoodSearch
import com.cake.freshenda.model.FoodCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FoodCatalogTest {
    private val catalog = Json
        .decodeFromString<FoodCatalog>(File("src/main/assets/food_catalog.v1.json").readText())

    @Test fun commonNamesFindTheExpectedIngredientFirst() {
        for ((query, id) in mapOf("肉丝" to "pork_strips", " 猪 肉 丝 " to "pork_strips", "瘦肉丝" to "pork_strips",
            "肉片" to "pork_slices", "鸡丁" to "chicken_cubes", "牛肉丝" to "beef_strips", "虾仁" to "peeled_shrimp",
            "香菇" to "shiitake", "面条" to "fresh_noodles", "腐竹" to "dried_tofu_skin")) {
            assertEquals(query, id, FoodSearch.find(catalog.foods, query).first().id)
        }
        assertTrue(FoodSearch.find(catalog.foods, "不存在的食材名称").isEmpty())
        assertTrue(FoodSearch.find(catalog.foods, "肉丝", "leafy").isEmpty())
    }

    @Test fun everyFoodHasUniqueIdentityAndAnExistingProfileAndIcon() {
        assertEquals(catalog.foods.size, catalog.foods.map { it.id }.toSet().size)
        assertEquals(catalog.foods.size, catalog.foods.map { it.iconKey }.toSet().size)
        for (food in catalog.foods) {
            assertTrue(food.id, catalog.profiles.any { it.id == food.profileId })
            assertTrue(food.id, catalog.categories.any { it.id == food.categoryId })
            assertTrue(food.iconKey, File("src/main/res/drawable/${food.iconKey}.xml").isFile)
        }
    }
}
