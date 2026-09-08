package com.cake.freshenda.data

import android.content.ContentResolver
import android.net.Uri
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.data.local.CustomFoodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val batches: List<FoodBatchEntity>,
    val customFoods: List<CustomFoodEntity> = emptyList(),
)

class BackupManager(
    private val contentResolver: ContentResolver,
    private val foodRepository: FoodRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val envelope = BackupEnvelope(
            exportedAtEpochMillis = System.currentTimeMillis(),
            batches = foodRepository.allBatches(),
            customFoods = foodRepository.allCustomFoods(),
        )
        contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(json.encodeToString(envelope)) }
    }

    suspend fun inspect(uri: Uri): BackupEnvelope = withContext(Dispatchers.IO) {
        val text = contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val envelope = json.decodeFromString<BackupEnvelope>(text)
        require(envelope.schemaVersion == 1) { "不支持的备份版本" }
        require(envelope.batches.all { it.id > 0 && it.quantityMilli >= 0 && it.displayName.isNotBlank() }) { "备份内容无效" }
        envelope
    }

    suspend fun replaceWith(envelope: BackupEnvelope): Int = withContext(Dispatchers.IO) {
        foodRepository.replaceAll(envelope.batches, envelope.customFoods)
        envelope.batches.size
    }
}
