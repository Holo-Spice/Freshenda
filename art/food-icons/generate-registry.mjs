import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "../..");
const catalogPath = path.join(root, "app/src/main/assets/food_catalog.v1.json");
const outputPath = path.join(root, "app/src/main/java/com/cake/freshenda/ui/components/FoodIconRegistry.kt");
const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));

const entries = catalog.foods
  .map((food) => `        "${food.iconKey}" to R.drawable.${food.iconKey},`)
  .join("\n");

const source = `package com.cake.freshenda.ui.components

import androidx.annotation.DrawableRes
import com.cake.freshenda.R

object FoodIconRegistry {
    private val icons: Map<String, Int> = mapOf(
${entries}
    )

    @DrawableRes
    fun resolve(iconKey: String): Int = icons[iconKey] ?: R.drawable.ic_food_custom

    fun contains(iconKey: String): Boolean = icons.containsKey(iconKey)

    val size: Int get() = icons.size
}
`;

fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, source, "utf8");
console.log(`Generated ${catalog.foods.length} explicit icon mappings.`);
