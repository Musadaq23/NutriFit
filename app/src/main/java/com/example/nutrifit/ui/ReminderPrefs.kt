package com.example.nutrifit.ui

import android.content.Context

object ReminderPrefs {

    private const val PREFS_NAME = "reminders_prefs"

    data class ReminderSettings(
        val remindersEnabled: Boolean,
        val mealEnabled: Boolean,
        val workoutEnabled: Boolean,
        val hour: Int,
        val minute: Int
    )

    fun saveSettings(
        context: Context,
        remindersEnabled: Boolean,
        mealEnabled: Boolean,
        workoutEnabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("reminders_enabled", remindersEnabled)
            .putBoolean("meal_enabled", mealEnabled)
            .putBoolean("workout_enabled", workoutEnabled)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
    }

    fun loadSettings(context: Context): ReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettings(
            remindersEnabled = prefs.getBoolean("reminders_enabled", false),
            mealEnabled = prefs.getBoolean("meal_enabled", true),
            workoutEnabled = prefs.getBoolean("workout_enabled", true),
            hour = prefs.getInt("reminder_hour", 20),   // default 20:00
            minute = prefs.getInt("reminder_minute", 0)
        )
    }
}