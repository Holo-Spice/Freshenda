package com.cake.freshenda.data

import androidx.room.withTransaction
import com.cake.freshenda.data.local.BatchChangeEntity
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.data.local.FreshendaDatabase
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.AddBatchDraft
import com.cake.freshenda.model.DateBasis
import com.cake.freshenda.model.DeadlineCandidate
import com.cake.freshenda.model.DeadlineKind
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.model.StorageProfile
import com.cake.freshenda.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class FoodRepository(
    private val database: FreshendaDatabase,
) {
    private val dao = database.foodDao()
    private val json = Json { encodeDefaults = true }

    fun observeActiveBatches(): Flow<List<FoodBatchEntity>> = dao.observeActiveBatches()
    fun observeCustomFoods(): Flow<List<CustomFoodEntity>> = dao.observeCustomFoods()
    fun observeBatch(id: Long): Flow<FoodBatchEntity?> = dao.observeBatch(id)
    fun observeRecentChanges(id: Long): Flow<List<BatchChangeEntity>> = dao.observeRecentChanges(id)
    suspend fun activeBatches(): List<FoodBatchEntity> = dao.activeBatches()
    suspend fun allBatches(): List<FoodBatchEntity> = dao.allBatches()
    suspend fun allCustomFoods(): List<CustomFoodEntity> = dao.allCustomFoods()

    suspend fun addBatch(draft: AddBatchDraft, catalog: FoodCatalog): Long {
        val profile = catalog.profiles.first { it.id == draft.food.profileId }
        val candidates = buildCandidates(draft, profile)
        val effective = ExpiryCalculator.chooseEffective(candidates)
        val now = Instant.now().toEpochMilli()
        val entity = FoodBatchEntity(
            foodDefinitionId = draft.food.id,
            displayName = draft.food.name,
            iconKey = draft.food.iconKey,
            quantityMilli = draft.quantityMilli,
            quantityUnit = draft.quantityUnit,
            storageLocation = draft.storageLocation.name,
            storageSection = draft.storageSection.name,
            physicalState = draft.physicalState.name,
            packagingState = draft.packagingState.name,
            maturityState = draft.maturityState.name,
            preparation = draft.completedPreparation,
            purchasedAtEpochMillis = draft.stageStartedEpochMillis,
            stageStartedAtEpochMillis = draft.stageStartedEpochMillis,
            businessZoneId = draft.businessZoneId,
            labelDateEpochDay = draft.labelDateEpochDay,
            labelStorageScope = draft.storageLocation.name,
            customDeadlineEpochMillis = candidates.firstOrNull { it.kind == DeadlineKind.CUSTOM }?.deadlineEpochMillis,
            suggestedDeadlineEpochMillis = candidates.firstOrNull { it.kind == DeadlineKind.SUGGESTED }?.deadlineEpochMillis,
            effectiveDeadlineEpochMillis = effective?.deadlineEpochMillis,
            effectiveAnchorEpochMillis = effective?.anchorEpochMillis,
            effectiveDeadlineKind = effective?.kind?.name,
            effectiveDisplayDateEpochDay = effective?.displayDateEpochDay,
            ruleSnapshot = ExpiryCalculator.suggestedWindow(draft, profile)?.let { json.encodeToString(it) },
            ruleDataVersion = catalog.dataVersion,
            requiresDateReview = draft.dateBasis == DateBasis.SUGGESTED && candidates.none { it.kind == DeadlineKind.SUGGESTED },
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        return database.withTransaction {
            if (draft.food.id.startsWith("custom-")) {
                val existing = dao.customFood(draft.food.id)
                val startDay = Instant.ofEpochMilli(draft.stageStartedEpochMillis).atZone(java.time.ZoneId.of(draft.businessZoneId)).toLocalDate()
                val customDays = draft.customDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it).toEpochDay() - startDay.toEpochDay() }?.toInt()?.coerceAtLeast(1)
                dao.insertCustomFood(
                    CustomFoodEntity(
                        id = draft.food.id,
                        name = draft.food.name,
                        categoryId = draft.food.categoryId,
                        iconKey = draft.food.iconKey,
                        defaultQuantityUnit = draft.quantityUnit,
                        refrigeratedDays = if (draft.storageLocation.name == "REFRIGERATED") customDays ?: existing?.refrigeratedDays else existing?.refrigeratedDays,
                        frozenDays = if (draft.storageLocation.name == "FROZEN") customDays ?: existing?.frozenDays else existing?.frozenDays,
                        pantryDays = if (draft.storageLocation.name == "PANTRY") customDays ?: existing?.pantryDays else existing?.pantryDays,
                        createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = now,
                    ),
                )
            }
            dao.insertBatch(entity)
        }
    }

    suspend fun updateBatch(batchId: Long, draft: AddBatchDraft, catalog: FoodCatalog) = database.withTransaction {
        val current = dao.batch(batchId) ?: error("批次不存在")
        val profile = catalog.profiles.first { it.id == draft.food.profileId }
        val candidates = buildCandidates(draft, profile)
        val now = Instant.now().toEpochMilli()
        val keepsOpenedStage = draft.packagingState.name == "OPENED" && draft.storageLocation == StorageLocation.REFRIGERATED
        val keepsThawedStage = draft.storageLocation == StorageLocation.REFRIGERATED
        val purchasedAt = if (current.purchasedAtEpochMillis == current.stageStartedAtEpochMillis) {
            draft.stageStartedEpochMillis
        } else {
            current.purchasedAtEpochMillis
        }
        val updated = current.copy(
            foodDefinitionId = draft.food.id,
            displayName = draft.food.name,
            iconKey = draft.food.iconKey,
            quantityMilli = draft.quantityMilli,
            quantityUnit = draft.quantityUnit,
            storageLocation = draft.storageLocation.name,
            storageSection = draft.storageSection.name,
            physicalState = draft.physicalState.name,
            packagingState = draft.packagingState.name,
            maturityState = draft.maturityState.name,
            preparation = draft.completedPreparation,
            purchasedAtEpochMillis = purchasedAt,
            stageStartedAtEpochMillis = draft.stageStartedEpochMillis,
            businessZoneId = draft.businessZoneId,
            labelDateEpochDay = draft.labelDateEpochDay,
            labelStorageScope = draft.labelDateEpochDay?.let { draft.storageLocation.name },
            customDeadlineEpochMillis = candidates.firstOrNull { it.kind == DeadlineKind.CUSTOM }?.deadlineEpochMillis,
            suggestedDeadlineEpochMillis = candidates.firstOrNull { it.kind == DeadlineKind.SUGGESTED }?.deadlineEpochMillis,
            openedAtEpochMillis = current.openedAtEpochMillis.takeIf { keepsOpenedStage },
            openedDeadlineEpochMillis = current.openedDeadlineEpochMillis.takeIf { keepsOpenedStage },
            thawedAtEpochMillis = current.thawedAtEpochMillis.takeIf { keepsThawedStage },
            thawedDeadlineEpochMillis = current.thawedDeadlineEpochMillis.takeIf { keepsThawedStage },
            effectiveDeadlineEpochMillis = null,
            effectiveAnchorEpochMillis = null,
            effectiveDeadlineKind = null,
            effectiveDisplayDateEpochDay = null,
            ruleSnapshot = ExpiryCalculator.suggestedWindow(draft, profile)?.let { json.encodeToString(it) },
            ruleDataVersion = catalog.dataVersion,
            requiresDateReview = draft.dateBasis == DateBasis.SUGGESTED && candidates.none { it.kind == DeadlineKind.SUGGESTED },
            deadlineRevision = current.deadlineRevision + 1,
            updatedAtEpochMillis = now,
        ).withRecalculatedEffective(now)
        dao.updateBatch(updated)
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "EDIT", note = "修改食材信息", changedAtEpochMillis = now))
    }

    suspend fun consume(batchId: Long, quantityMilli: Long) = database.withTransaction {
        val current = dao.batch(batchId) ?: return@withTransaction
        val consumed = quantityMilli.coerceIn(1, current.quantityMilli)
        val remaining = current.quantityMilli - consumed
        val now = Instant.now().toEpochMilli()
        dao.updateBatch(current.copy(
            quantityMilli = remaining,
            status = if (remaining == 0L) "CONSUMED" else current.status,
            updatedAtEpochMillis = now,
        ))
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "CONSUME", quantityDeltaMilli = -consumed, note = "吃掉 ${formatQuantity(consumed)} ${current.quantityUnit}", changedAtEpochMillis = now))
    }

    suspend fun markOpened(batchId: Long, catalog: FoodCatalog) = database.withTransaction {
        val current = dao.batch(batchId) ?: return@withTransaction
        if (current.packagingState == "OPENED") return@withTransaction
        val now = Instant.now().toEpochMilli()
        val food = catalog.foods.firstOrNull { it.id == current.foodDefinitionId }
        val openedWindow = food?.let { definition -> catalog.profiles.firstOrNull { it.id == definition.profileId }?.openedRefrigerated }
        val openedDeadline = openedWindow?.takeIf { it.defaultValue != null && it.unit != null }?.let { ExpiryCalculator.deadlineFromWindow(now, current.businessZoneId, it) }
        val updated = current.copy(
            packagingState = "OPENED",
            openedAtEpochMillis = now,
            openedDeadlineEpochMillis = openedDeadline,
            suggestedDeadlineEpochMillis = if (openedWindow == null) null else current.suggestedDeadlineEpochMillis,
            requiresDateReview = openedDeadline == null,
            updatedAtEpochMillis = now,
            deadlineRevision = current.deadlineRevision + 1,
        ).withRecalculatedEffective(now)
        dao.updateBatch(updated)
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "OPEN", note = if (openedDeadline == null) "记录开封；开封后日期待确认" else "记录开封并应用明确的开封后期限", changedAtEpochMillis = now))
    }

    suspend fun moveBatch(batchId: Long, target: StorageLocation) = database.withTransaction {
        val current = dao.batch(batchId) ?: return@withTransaction
        if (current.storageLocation == target.name) return@withTransaction
        val now = Instant.now().toEpochMilli()
        val moved = movedCopy(current, target, now).copy(id = current.id, updatedAtEpochMillis = now)
        dao.updateBatch(moved)
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "MOVE", note = "转为${storageText(target)}；新阶段日期待确认", changedAtEpochMillis = now))
    }

    suspend fun splitAndMove(batchId: Long, quantityMilli: Long, target: StorageLocation): Long = database.withTransaction {
        val current = dao.batch(batchId) ?: error("批次不存在")
        require(quantityMilli in 1 until current.quantityMilli) { "拆分数量必须小于剩余数量" }
        val now = Instant.now().toEpochMilli()
        dao.updateBatch(current.copy(quantityMilli = current.quantityMilli - quantityMilli, updatedAtEpochMillis = now))
        val childId = dao.insertBatch(movedCopy(current.copy(id = 0, quantityMilli = quantityMilli), target, now).copy(createdAtEpochMillis = now, updatedAtEpochMillis = now))
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "SPLIT", quantityDeltaMilli = -quantityMilli, note = "拆出 ${formatQuantity(quantityMilli)} ${current.quantityUnit}", changedAtEpochMillis = now))
        dao.insertChange(BatchChangeEntity(batchId = childId, kind = "SPLIT", quantityDeltaMilli = quantityMilli, note = "由批次 $batchId 拆分并转为${storageText(target)}", changedAtEpochMillis = now))
        childId
    }

    suspend fun setCustomDate(batchId: Long, dateEpochDay: Long) = database.withTransaction {
        val current = dao.batch(batchId) ?: return@withTransaction
        val now = Instant.now().toEpochMilli()
        val deadline = ExpiryCalculator.customDeadline(dateEpochDay, current.businessZoneId)
        dao.updateBatch(current.copy(customDeadlineEpochMillis = deadline, requiresDateReview = false, deadlineRevision = current.deadlineRevision + 1, updatedAtEpochMillis = now).withRecalculatedEffective(now))
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "DATE", note = "设置自己的计划日期 ${java.time.LocalDate.ofEpochDay(dateEpochDay)}", changedAtEpochMillis = now))
    }

    suspend fun discard(batchId: Long) = database.withTransaction {
        val current = dao.batch(batchId) ?: return@withTransaction
        val now = Instant.now().toEpochMilli()
        dao.updateBatch(current.copy(status = "DISCARDED", updatedAtEpochMillis = now))
        dao.insertChange(BatchChangeEntity(batchId = batchId, kind = "DISCARD", quantityDeltaMilli = -current.quantityMilli, note = "标记丢弃", changedAtEpochMillis = now))
    }

    suspend fun replaceAll(batches: List<FoodBatchEntity>, customFoods: List<CustomFoodEntity>) = database.withTransaction { dao.replaceAll(batches, customFoods) }

    private fun buildCandidates(draft: AddBatchDraft, profile: StorageProfile): List<DeadlineCandidate> {
        val candidates = mutableListOf<DeadlineCandidate>()
        draft.labelDateEpochDay?.let { day ->
            candidates += DeadlineCandidate(DeadlineKind.LABEL, draft.stageStartedEpochMillis, ExpiryCalculator.labelDeadline(day, draft.businessZoneId), day, "包装到期日")
        }
        draft.customDateEpochDay?.let { day ->
            candidates += DeadlineCandidate(DeadlineKind.CUSTOM, draft.stageStartedEpochMillis, ExpiryCalculator.customDeadline(day, draft.businessZoneId), day, "自己设定")
        }
        if (draft.dateBasis == DateBasis.SUGGESTED) {
            ExpiryCalculator.suggestedWindow(draft, profile)?.let { window ->
                val deadline = ExpiryCalculator.deadlineFromWindow(draft.stageStartedEpochMillis, draft.businessZoneId, window)
                candidates += DeadlineCandidate(DeadlineKind.SUGGESTED, draft.stageStartedEpochMillis, deadline, ExpiryCalculator.displayDate(deadline, draft.businessZoneId), "储存参考")
            }
        }
        return candidates
    }

    private fun formatQuantity(quantityMilli: Long): String =
        if (quantityMilli % 1000L == 0L) (quantityMilli / 1000L).toString() else "%.3f".format(quantityMilli / 1000.0).trimEnd('0').trimEnd('.')

    private fun movedCopy(batch: FoodBatchEntity, target: StorageLocation, now: Long): FoodBatchEntity {
        val remainingSeconds = batch.effectiveDeadlineEpochMillis?.minus(now)?.div(1000)?.takeIf { it > 0 }
        val labelApplies = batch.labelStorageScope == target.name
        val base = batch.copy(
            storageLocation = target.name,
            storageSection = when (target) { StorageLocation.REFRIGERATED -> "UPPER"; StorageLocation.FROZEN -> "FREEZER_DRAWER"; StorageLocation.PANTRY -> "SHELF" },
            stageStartedAtEpochMillis = now,
            thawedAtEpochMillis = if (batch.storageLocation == StorageLocation.FROZEN.name && target == StorageLocation.REFRIGERATED) now else batch.thawedAtEpochMillis,
            suggestedDeadlineEpochMillis = null,
            openedDeadlineEpochMillis = if (target == StorageLocation.REFRIGERATED) batch.openedDeadlineEpochMillis else null,
            effectiveDeadlineEpochMillis = null,
            effectiveAnchorEpochMillis = null,
            effectiveDeadlineKind = null,
            effectiveDisplayDateEpochDay = null,
            preFreezeRemainingSeconds = if (target == StorageLocation.FROZEN) remainingSeconds else batch.preFreezeRemainingSeconds,
            requiresDateReview = true,
            deadlineRevision = batch.deadlineRevision + 1,
        )
        return base.withRecalculatedEffective(now, includeLabel = labelApplies)
    }

    private fun FoodBatchEntity.withRecalculatedEffective(anchor: Long, includeLabel: Boolean = true): FoodBatchEntity {
        val choices = mutableListOf<DeadlineCandidate>()
        if (includeLabel && labelDateEpochDay != null) choices += DeadlineCandidate(DeadlineKind.LABEL, purchasedAtEpochMillis, ExpiryCalculator.labelDeadline(labelDateEpochDay, businessZoneId), labelDateEpochDay, "包装到期日")
        customDeadlineEpochMillis?.let { choices += DeadlineCandidate(DeadlineKind.CUSTOM, stageStartedAtEpochMillis, it, ExpiryCalculator.displayDate(it, businessZoneId), "自己设定") }
        suggestedDeadlineEpochMillis?.let { choices += DeadlineCandidate(DeadlineKind.SUGGESTED, stageStartedAtEpochMillis, it, ExpiryCalculator.displayDate(it, businessZoneId), "储存参考") }
        openedDeadlineEpochMillis?.let { choices += DeadlineCandidate(DeadlineKind.OPENED, openedAtEpochMillis ?: anchor, it, ExpiryCalculator.displayDate(it, businessZoneId), "开封后期限") }
        thawedDeadlineEpochMillis?.let { choices += DeadlineCandidate(DeadlineKind.THAWED, thawedAtEpochMillis ?: anchor, it, ExpiryCalculator.displayDate(it, businessZoneId), "解冻后参考") }
        val effective = ExpiryCalculator.chooseEffective(choices)
        return copy(
            effectiveDeadlineEpochMillis = effective?.deadlineEpochMillis,
            effectiveAnchorEpochMillis = effective?.anchorEpochMillis,
            effectiveDeadlineKind = effective?.kind?.name,
            effectiveDisplayDateEpochDay = effective?.displayDateEpochDay,
        )
    }

    private fun storageText(storage: StorageLocation) = when (storage) { StorageLocation.REFRIGERATED -> "冷藏"; StorageLocation.FROZEN -> "冷冻"; StorageLocation.PANTRY -> "常温" }
}
