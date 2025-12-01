package com.example.nutrifit.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.nutrifit.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var enableSwitch: Switch
    private lateinit var mealSwitch: Switch
    private lateinit var workoutSwitch: Switch
    private lateinit var timeButton: Button
    private lateinit var timeLabel: TextView

    private var hour = 20
    private var minute = 0

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                ReminderScheduler.scheduleDailyReminder(requireContext())
            } else {
                timeLabel.text = "Notifications disabled"
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableSwitch = view.findViewById(R.id.switchEnableReminder)
        mealSwitch = view.findViewById(R.id.switchMealReminder)
        workoutSwitch = view.findViewById(R.id.switchWorkoutReminder)
        timeButton = view.findViewById(R.id.buttonPickTime)
        timeLabel = view.findViewById(R.id.textReminderTime)

        val settings = ReminderPrefs.loadSettings(requireContext())
        enableSwitch.isChecked = settings.remindersEnabled
        mealSwitch.isChecked = settings.mealEnabled
        workoutSwitch.isChecked = settings.workoutEnabled
        hour = settings.hour
        minute = settings.minute
        updateTimeLabel()

        enableSwitch.setOnCheckedChangeListener { _, _ ->
            saveAndApply()
        }

        mealSwitch.setOnCheckedChangeListener { _, _ ->
            saveAndApply()
        }

        workoutSwitch.setOnCheckedChangeListener { _, _ ->
            saveAndApply()
        }

        timeButton.setOnClickListener {
            val picker = TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    hour = h
                    minute = m
                    updateTimeLabel()
                    saveAndApply()
                },
                hour,
                minute,
                true
            )
            picker.show()
        }
    }

    private fun updateTimeLabel() {
        timeLabel.text = String.format("Reminder time: %02d:%02d", hour, minute)
    }

    private fun saveAndApply() {
        ReminderPrefs.saveSettings(
            requireContext(),
            remindersEnabled = enableSwitch.isChecked,
            mealEnabled = mealSwitch.isChecked,
            workoutEnabled = workoutSwitch.isChecked,
            hour = hour,
            minute = minute
        )

        if (enableSwitch.isChecked) {
            ensurePermissionThenSchedule()
        } else {
            ReminderScheduler.cancelDailyReminder(requireContext())
        }
    }

    private fun ensurePermissionThenSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        ReminderScheduler.scheduleDailyReminder(requireContext())
    }
}

