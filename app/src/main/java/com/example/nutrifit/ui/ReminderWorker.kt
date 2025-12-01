package com.example.nutrifit.ui

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nutrifit.NotificationHelper


class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = ReminderPrefs.loadSettings(applicationContext)

        if (!settings.remindersEnabled) {
            return Result.success()
        }

        if (settings.mealEnabled) {
            NotificationHelper.showMealReminder(applicationContext)
        }

        if (settings.workoutEnabled) {
            NotificationHelper.showWorkoutReminder(applicationContext)
        }

        return Result.success()
    }
}
