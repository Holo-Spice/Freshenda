package com.cake.freshenda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_batches WHERE status = 'ACTIVE' AND quantityMilli > 0 ORDER BY effectiveDeadlineEpochMillis IS NULL, effectiveDeadlineEpochMillis, purchasedAtEpochMillis, id")
    fun observeActiveBatches(): Flow<List<FoodBatchEntity>>

    @Query("SELECT * FROM food_batches WHERE status = 'ACTIVE' AND quantityMilli > 0")
    suspend fun activeBatches(): List<FoodBatchEntity>

    @Query("SELECT * FROM food_batches ORDER BY id")
    suspend fun allBatches(): List<FoodBatchEntity>

    @Query("SELECT * FROM custom_foods ORDER BY updatedAtEpochMillis DESC")
    fun observeCustomFoods(): Flow<List<CustomFoodEntity>>

    @Query("SELECT * FROM custom_foods ORDER BY id")
    suspend fun allCustomFoods(): List<CustomFoodEntity>

    @Query("SELECT * FROM custom_foods WHERE id = :id LIMIT 1")
    suspend fun customFood(id: String): CustomFoodEntity?

    @Query("SELECT * FROM food_batches WHERE id = :id LIMIT 1")
    fun observeBatch(id: Long): Flow<FoodBatchEntity?>

    @Query("SELECT * FROM food_batches WHERE id = :id LIMIT 1")
    suspend fun batch(id: Long): FoodBatchEntity?

    @Insert
    suspend fun insertBatch(batch: FoodBatchEntity): Long

    @Insert
    suspend fun insertChange(change: BatchChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: ReminderDeliveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFoods(foods: List<CustomFoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<FoodBatchEntity>)

    @Update
    suspend fun updateBatch(batch: FoodBatchEntity)

    @Query("SELECT * FROM batch_changes WHERE batchId = :batchId ORDER BY changedAtEpochMillis DESC LIMIT 8")
    fun observeRecentChanges(batchId: Long): Flow<List<BatchChangeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_deliveries WHERE deliveryKey = :key)")
    suspend fun deliveryExists(key: String): Boolean

    @Query("DELETE FROM batch_changes")
    suspend fun clearChanges()

    @Query("DELETE FROM reminder_deliveries")
    suspend fun clearDeliveries()

    @Query("DELETE FROM food_batches")
    suspend fun clearBatches()

    @Query("DELETE FROM custom_foods")
    suspend fun clearCustomFoods()

    @Transaction
    suspend fun replaceAll(batches: List<FoodBatchEntity>, customFoods: List<CustomFoodEntity>) {
        clearChanges()
        clearDeliveries()
        clearBatches()
        clearCustomFoods()
        insertBatches(batches)
        insertCustomFoods(customFoods)
    }
}
