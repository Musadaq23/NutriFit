package com.example.nutrifit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val REQ_CODE = 1001

    fun schedule(
        context: Context,
        date: LocalDate,
        time: LocalTime,
        title: String = "NutriFit Reminder",
        text: String = "Time to log your meal or workout."
    ) {
        NotificationHelper.ensureChannel(context)

        val zone = ZoneId.systemDefault()
        var triggerAtMillis = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) {
            triggerAtMillis = date.plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE,
            intent,
            flags
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // On Android 12+ (S), exact alarms may require user approval
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // If exact alarms are not permitted, do nothing
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE,
            intent,
            flags
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}


