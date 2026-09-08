package com.cake.freshenda

import android.app.Application
import com.cake.freshenda.reminder.ReminderScheduler

class XianxuApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.notificationPublisher.createChannel()
        ReminderScheduler.ensureScheduled(this)
    }
}
