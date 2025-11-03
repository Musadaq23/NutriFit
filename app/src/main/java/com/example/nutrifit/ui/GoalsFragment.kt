package com.example.nutrifit.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrifit.ReminderReceiver
import com.example.nutrifit.databinding.FragmentGoalsBinding
import java.util.Calendar

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)

        binding.tvGoalsTitle?.text = "Goals"
        binding.tvGoalsData?.text = "Daily calories: 2200\nWorkouts per week: 4"

        binding.btnDemoReminder.setOnClickListener {
            scheduleInSeconds(10, "Log your meal", "Add your dinner to NutriFit.")
            Toast.makeText(requireContext(), "Reminder in 10 seconds", Toast.LENGTH_SHORT).show()
        }

        binding.btnDailyReminder.setOnClickListener {
            scheduleDaily(20, 0, "Evening check-in", "Log today’s meals and workouts.")
            Toast.makeText(requireContext(), "Daily reminder set for 8:00 PM", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelReminder.setOnClickListener {
            cancelReminders()
            Toast.makeText(requireContext(), "Reminders canceled", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    private fun pendingIntent(title: String, text: String): PendingIntent {
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmMgr(): AlarmManager =
        requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // DEMO: fires once in N seconds (easy for presentation)
    private fun scheduleInSeconds(seconds: Int, title: String, text: String) {
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        alarmMgr().setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(title, text)
        )
    }

    // daily at fixed hour/minute
    private fun scheduleDaily(hour: Int, minute: Int, title: String, text: String) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // tomorrow if time already passed
            }
        }
        alarmMgr().setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pendingIntent(title, text)
        )
    }

    private fun cancelReminders() {
        alarmMgr().cancel(pendingIntent("", "")) // same PI signature cancels
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

