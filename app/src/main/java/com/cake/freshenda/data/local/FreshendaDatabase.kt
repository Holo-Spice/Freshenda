package com.cake.freshenda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FoodBatchEntity::class, BatchChangeEntity::class, ReminderDeliveryEntity::class, CustomFoodEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FreshendaDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}
