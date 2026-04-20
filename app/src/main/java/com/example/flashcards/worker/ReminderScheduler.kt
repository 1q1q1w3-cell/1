package com.example.flashcards.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(intervalMinutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "cards_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
