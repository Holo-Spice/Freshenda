package com.cake.freshenda.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cake.freshenda.XianxuApplication
import com.cake.freshenda.data.local.ReminderDeliveryEntity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as XianxuApplication).container
        val settings = container.settingsRepository.settings.first()
        val batches = container.foodRepository.activeBatches()
        val now = Instant.now().toEpochMilli()
        val decision = ReminderPolicy.decide(batches, settings, now, ZoneId.systemDefault()) ?: return Result.success()
        val key = "normal:${decision.periodEpochDay}"
        val dao = container.database.foodDao()
        if (dao.deliveryExists(key)) return Result.success()
        val published = container.notificationPublisher.publishSummary(decision.batches)
        if (published) {
            dao.insertDelivery(ReminderDeliveryEntity(key, decision.periodEpochDay, decision.batches.joinToString(",") { "${it.id}:${it.deadlineRevision}" }.hashCode().toString(), now))
        }
        return Result.success()
    }
}
