package com.example.nutrifit.ui

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nutrifit.ReminderReceiver
import com.example.nutrifit.databinding.FragmentGoalsBinding
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)

        createChart()
        calcPercent(1200, 0)

        binding.tvGoalsTitle.text = "Goals"
        binding.tvGoalsData.text = "Set a goal to start \ntracking your progress."
        //binding.tvGoalsData?.text = "Daily calories: 2200\nWorkouts per week: 4"

        //On click listener for time
        binding.reminderTime.setOnClickListener{
            showTimePicker()
        }

        //On click listener for date
        binding.reminderDate.setOnClickListener{
            showDatePicker()
        }

        //On click listener to update the goal
        binding.btnEditGoal.setOnClickListener{
            calorieGoalInputDialog()
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

    //Update goal dialog function
    private fun calorieGoalInputDialog(){

        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter calorie goal"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Input your calorie goal!")
            .setView(editText)
            .setPositiveButton("Save") {dialog, _ ->

                val input = editText.text.toString().trim()

                //Input validation
                //Empty
                if (input.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a number.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                //Exceeds value
                val inpValue = input.toInt()
                if (inpValue > 4000) {
                    Toast.makeText(requireContext(), "Goal cannot exceed 4000 calories.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                //Update goal textview
                binding.tvGoalsData.text = "Daily Calorie goal: $inpValue"

                dialog.dismiss()
                calcPercent(1200, inpValue)
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    //Date Picker
    private fun showDatePicker(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "${selectedMonth}/${selectedDay}/${selectedYear}"
                binding.reminderDate.text = formattedDate
            },
            year,
            month,
            day
        )

        datePicker.show()
    }

    //Time Picker
    private fun showTimePicker(){
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val formattedTime = String.format("%02d:%02d %s", hour12, selectedMinute, amPm)
                binding.reminderTime.text = formattedTime
            },
            hour,
            minute,
            false
        )

        timePicker.show()
    }

    //Calculate percentage to goal, apply to progress bar.
    private fun calcPercent(C: Int, G: Int ){
        val current: Int = C
        var goal: Int = G
        val percent: Int

        //No dividing by zero dumbass.
        if(goal == 0) {
            percent = 100
        }

        else {
            percent = 100 * current/goal
        }

        //send update to progress bar
        lifecycleScope.launch {
            progressBar(percent)
        }
    }

    //Updates progress bar overtime based on value given
    private suspend fun progressBar(progress: Int){
        for (i in 1..progress){
                binding.calorieProgressBar.setProgress(i, true)
                delay(35)
        }
    }

    //For creation of the "work out" chart at the bottom of the fragment
    private fun createChart(){
        binding.goalChart.getAxisRight().setDrawLabels(false)

        var goalEntries: ArrayList <BarEntry> = ArrayList<BarEntry>()

        //Test data entries
        goalEntries.add(0, BarEntry(1f,1f))
        goalEntries.add(1, BarEntry(2f,2f))
        goalEntries.add(2, BarEntry(3f,3f))
        goalEntries.add(3, BarEntry(4f,4f))
        goalEntries.add(4, BarEntry(5f,5f))
        goalEntries.add(5, BarEntry(6f,6f))
        goalEntries.add(6, BarEntry(7f,7f))

        //Assigns  to variables to be pushed to chart
        var dataSet: BarDataSet = BarDataSet(goalEntries, "Minutes worked out")
        var barData: BarData = BarData(dataSet)

        binding.goalChart.setData(barData)
    }
}

