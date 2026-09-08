package com.cake.freshenda

import android.content.Context
import androidx.room.Room
import com.cake.freshenda.data.BackupManager
import com.cake.freshenda.data.FoodRepository
import com.cake.freshenda.data.SettingsRepository
import com.cake.freshenda.data.catalog.CatalogLoader
import com.cake.freshenda.data.local.FreshendaDatabase
import com.cake.freshenda.reminder.NotificationPublisher

class AppContainer(context: Context) {
    val database: FreshendaDatabase = Room.databaseBuilder(context, FreshendaDatabase::class.java, "freshenda.db").build()
    val catalogLoader = CatalogLoader(context)
    val foodRepository = FoodRepository(database)
    val settingsRepository = SettingsRepository(context)
    val backupManager = BackupManager(context.contentResolver, foodRepository)
    val notificationPublisher = NotificationPublisher(context)
}
