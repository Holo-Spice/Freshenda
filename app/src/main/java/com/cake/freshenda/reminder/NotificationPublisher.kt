package com.cake.freshenda.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cake.freshenda.MainActivity
import com.cake.freshenda.R
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import java.time.Instant

class NotificationPublisher(private val context: Context) {
    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_description)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    fun canPublish(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }

    @SuppressLint("MissingPermission")
    fun publishSummary(batches: List<FoodBatchEntity>, test: Boolean = false): Boolean {
        createChannel()
        if (!canPublish()) return false
        val now = Instant.now().toEpochMilli()
        val title = if (test) "鲜序测试提醒" else if (batches.size == 1) "${batches.first().displayName}${ExpiryCalculator.result(batches.first(), now).statusText}" else "有 ${batches.size} 份食材需要安排"
        val summary = if (test) "通知渠道工作正常；正式提醒允许由系统延迟送达" else batches.take(3).joinToString("、") { it.displayName } + if (batches.size > 3) "等" else ""
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, if (batches.size == 1) "detail" else "due")
            batches.singleOrNull()?.let { putExtra(MainActivity.EXTRA_BATCH_ID, it.id) }
        }
        val pendingIntent = PendingIntent.getActivity(context, if (test) 2 else 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_freshness)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "food_expiry_reminders"
        const val NOTIFICATION_ID = 1042
    }
}
