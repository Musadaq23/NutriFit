package com.example.nutrifit.ui

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit


object ReminderScheduler {

    private const val UNIQUE_WORK_NAME = "daily_reminder_work"


    fun scheduleDailyReminder(context: Context) {
        val settings = ReminderPrefs.loadSettings(context)

        if (!settings.remindersEnabled) {
            cancelDailyReminder(context)
            return
        }

        val now = Calendar.getInstance()

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (next.before(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = next.timeInMillis - now.timeInMillis
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)

        val workRequest =
            PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}