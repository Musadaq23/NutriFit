package com.example.nutrifit.ui

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.github.mikephil.charting.components.Description
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nutrifit.CalorieGoalEntity
import com.example.nutrifit.MealEntity
import com.example.nutrifit.ReminderScheduler
import com.example.nutrifit.WorkoutEntity
import com.example.nutrifit.WorkoutGoalEntity
import com.example.nutrifit.databinding.FragmentGoalsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Calendar

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val workoutsViewModel: WorkoutsViewModel by activityViewModels()
    private val mealsViewModel: MealsViewModel by activityViewModels()

    private var weeklyWorkoutGoal: Int = 0
    private var dailyCalorieGoal: Int = 0

    private var selectedReminderDate: LocalDate? = null
    private var selectedReminderTime: LocalTime? = null

    // Cache of today's calories so the progress bar updates correctly when the goal changes
    private var todayCaloriesTotal: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        getCalorieGoal()
        getWorkoutGoal()

        // Listen for workout updates (drives workout progress + weekly chart)
        workoutsViewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            updateWorkoutFromData(workouts)
        }

        // Listen for meal updates (drives calorie progress)
        mealsViewModel.meals.observe(viewLifecycleOwner) { meals ->
            updateCaloriesFromData(meals)
        }

        // Initialize chart with empty data
        createChart(List(7) { 0 })

        // Chart description label
        val desc = Description().apply {
            text = "Minutes worked out this week"
            textSize = 12f
        }
        binding.goalChart.description = desc

        // On click listener for time
        binding.reminderTime.setOnClickListener{
            showTimePicker()
        }

        // On click listener for date
        binding.reminderDate.setOnClickListener{
            showDatePicker()
        }

        // On click listener to update the calorie goal
        binding.btnEditGoal.setOnClickListener{
            calorieGoalInputDialog()
        }

        // On click listener for reminder button
        binding.btnDailyReminder.setOnClickListener {

            if (!hasExactAlarmPermission()) {
                requestExactAlarmPermission()
                return@setOnClickListener
            }

            val date = selectedReminderDate
            val time = selectedReminderTime

            if (date == null || time == null) {
                Toast.makeText(requireContext(), "Please select a date and time first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ReminderScheduler.schedule(
                context = requireContext(),
                date = date,
                time = time,
                title = "NutriFit Check-in",
                text = "Don’t forget to log today’s meals and workouts!"
            )

            Toast.makeText(requireContext(), "Reminder scheduled.", Toast.LENGTH_SHORT).show()
        }

        // On click listener for reminder cancel button
        binding.btnCancelReminder.setOnClickListener {
            ReminderScheduler.cancel(requireContext())
            Toast.makeText(requireContext(), "Reminders canceled", Toast.LENGTH_SHORT).show()
        }

        // On click listener for Workout goal button
        binding.btnWorkoutGoal.setOnClickListener{
            workoutGoalInputDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Go to Login function pulled from Profile Fragment
    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    ////////////////////////////////////
    //[*]REMINDER RELATED FUNCTIONS[*]//
    ////////////////////////////////////

    // Check if we’re allowed to schedule exact alarms (Android 12+)
    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        return alarmManager?.canScheduleExactAlarms() == true
    }

    // Open system settings to request exact-alarm permission
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
            Toast.makeText(
                requireContext(),
                "Enable exact alarms for NutriFit, then tap the button again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    ///////////////////////////////////
    //[*]WORKOUT RELATED FUNCTIONS[*]//
    ///////////////////////////////////

    // Update goal dialog function
    private fun workoutGoalInputDialog(){
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter workout goal"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Input your workout goal!")
            .setView(editText)
            .setPositiveButton("Save") {dialog, _ ->

                val input = editText.text.toString().trim()

                // Input validation
                // Empty
                if (input.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a number.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Exceeds value
                val inpValue = input.toInt()
                if (inpValue > 840) {
                    Toast.makeText(requireContext(), "Goal cannot exceed 840 minutes", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Update goal textview
                binding.tvWorkoutGoal.text = "Your current workout goal this week is: $inpValue minutes"

                dialog.dismiss()
                weeklyWorkoutGoal = inpValue
                workoutsViewModel.workouts.value?.let { updateWorkoutFromData(it) }

                val goal = WorkoutGoalEntity(
                    goal = inpValue
                )

                setWorkoutGoal(goal)
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Adds workout goal to DB
    fun setWorkoutGoal(workoutGoal: WorkoutGoalEntity){
        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "workoutGoal" to workoutGoal.goal,
        )

        db.collection("users")
            .document(user.uid)
            .collection("Goal")
            .document("workoutGoal")
            .set(data)
    }

    // Retrieves workout goal
    fun getWorkoutGoal(){
        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val uid = user.uid

        db.collection("users")
            .document(uid)
            .collection("Goal")
            .document("workoutGoal")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    println("Successfully retrieved DB collection")
                    val goalValue: Int = doc.getLong("workoutGoal")?.toInt() ?: 0
                    println("retrieved stored value: $goalValue")

                    weeklyWorkoutGoal = goalValue

                    // Update goal textview
                    binding.tvWorkoutGoal.text =
                        if (goalValue > 0) "Weekly workout goal: $goalValue"
                        else "No weekly workout goal set."

                    workoutsViewModel.workouts.value?.let { updateWorkoutFromData(it) }
                } else {
                    println("Document does not exist")
                }
            }
    }

    // Calculates the total workout time for the day (currently unused, kept for future expansion)
    fun dailyWorkoutTotal(callback: (Int)-> Unit) {
        val user = auth.currentUser ?: return callback(0)
        val uid = user.uid

        db.collection("users")
            .document(uid)
            .collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                val totalDuration = snapshot.documents.sumOf { doc ->
                    doc.getLong("durationMinutes")?.toInt() ?: 0
                }
                callback(totalDuration)
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    ///////////////////////////////////
    //[*]CALORIE RELATED FUNCTIONS[*]//
    ///////////////////////////////////

    // Update calorie goal dialog function
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

                // Input validation
                // Empty
                if (input.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a number.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Exceeds value
                val inpValue = input.toInt()
                if (inpValue > 4000) {
                    Toast.makeText(requireContext(), "Goal cannot exceed 4000 calories.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Update goal textview
                binding.tvGoalsData.text = "Daily Calorie goal: $inpValue"

                dialog.dismiss()

                dailyCalorieGoal = inpValue
                calcCaloriePercent(todayCaloriesTotal, dailyCalorieGoal)

                val goal = CalorieGoalEntity(
                    goal = inpValue
                )

                setCalorieGoal(goal)
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Adds calorie goal to DB
    fun setCalorieGoal(caloricGoal: CalorieGoalEntity){
        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "caloricGoal" to caloricGoal.goal,
        )

        db.collection("users")
            .document(user.uid)
            .collection("Goal")
            .document("caloricGoal")
            .set(data)
    }

    // Retrieves calorie goal and updates UI
    fun getCalorieGoal() {

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val uid = user.uid

        db.collection("users")
            .document(uid)
            .collection("Goal")
            .document("caloricGoal")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    println("Successfully retrieved DB collection")
                    val calorieGoal: Int = doc.getLong("caloricGoal")?.toInt() ?: 0
                    println("retrieved stored value: $calorieGoal")

                    dailyCalorieGoal = calorieGoal

                    // Update goal textview
                    binding.tvGoalsData.text =
                        if (calorieGoal > 0) "Daily Calorie goal: $calorieGoal"
                        else "Set a goal to start \ntracking your progress."

                    calcCaloriePercent(todayCaloriesTotal, dailyCalorieGoal)
                } else {
                    println("Document does not exist")
                }
            }
    }

    /////////////////////////////////
    //[*]TIME AND DATE SELECTORS[*]//
    /////////////////////////////////

    // Date Picker
    private fun showDatePicker(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "${selectedMonth+1}/${selectedDay}/${selectedYear}"
                binding.reminderDate.text = formattedDate

                selectedReminderDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            },
            year,
            month,
            day
        )

        datePicker.show()
    }

    // Time Picker
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

                selectedReminderTime = LocalTime.of(selectedHour, selectedMinute)
            },
            hour,
            minute,
            false
        )

        timePicker.show()
    }

    ///////////////////////////////
    //[*]CALCULATION FUNCTIONS[*]//
    ///////////////////////////////

    // Calculate percentage to goal for Calorie Bar
    private fun calcCaloriePercent(c: Int, g: Int ){
        val current: Int = c
        val goal: Int = g

        // Prevent divide-by-zero when no goal is set.
        val percent: Int =
            if (goal <= 0) 0 else (100 * current / goal).coerceIn(0, 100)

        // send update to progress bar
        lifecycleScope.launch {
            calorieProgressBar(percent)
        }
    }

    // Calculate percentage to goal for Workout Bar
    private fun calcWorkoutPercent(current: Int, goal: Int) {
        val percent = if (goal <= 0) 0 else (current * 100 / goal).coerceIn(0, 100)

        // send update to progress bar
        lifecycleScope.launch {
            workoutProgressBar(percent)
        }
    }

    /////////////////////////////////
    //[*]CHART RELATED FUNCTIONS[*]//
    /////////////////////////////////

    // Updates calorie progress bar
    private suspend fun calorieProgressBar(progress: Int){
        for (i in 1..progress){
            binding.calorieProgressBar.setProgress(i, true)
            delay(35)
        }
    }

    // Updates workout progress bar
    private suspend fun workoutProgressBar(progress: Int){
        for (i in 1..progress){
            binding.workoutProgressBar.setProgress(i, true)
            delay(35)
        }
    }

    // For creation of the workout chart at the bottom of the fragment
    private fun createChart(weekMinutes: List<Int>) {
        binding.goalChart.axisRight.setDrawLabels(false)
        binding.goalChart.axisLeft.axisMinimum = 0f
        binding.goalChart.setScaleEnabled(false)

        val entries = ArrayList<BarEntry>()
        weekMinutes.forEachIndexed { index, minutes ->
            entries.add(BarEntry(index.toFloat(), minutes.toFloat()))
        }

        // Assign data to the chart
        val dataSet = BarDataSet(entries, "Minutes worked out")
        val barData: BarData = BarData(dataSet)

        binding.goalChart.data = barData
        binding.goalChart.invalidate()
    }

    private fun updateWorkoutFromData(workouts: List<WorkoutEntity>) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)

        var weekTotal = 0
        val perDay = MutableList(7) { 0 }

        for (w in workouts) {
            try {
                val d = LocalDate.parse(w.date)
                if (!d.isBefore(weekStart) && !d.isAfter(today)) {
                    weekTotal += w.durationMinutes
                    val index = (d.toEpochDay() - weekStart.toEpochDay()).toInt()
                    if (index in 0..6) {
                        perDay[index] += w.durationMinutes
                    }
                }
            } catch (_: Exception) {
                // ignore bad date formats
            }
        }

        // update
        calcWorkoutPercent(weekTotal, weeklyWorkoutGoal)

        // chart
        createChart(perDay)
    }

    private fun updateCaloriesFromData(meals: List<MealEntity>) {
        val today = LocalDate.now()
        var total = 0

        for (m in meals) {
            try {
                val dt = LocalDateTime.parse(m.dateTime)
                if (dt.toLocalDate() == today) {
                    total += m.calories
                }
            } catch (_: Exception) {
                // ignore bad datetime formats
            }
        }

        todayCaloriesTotal = total

        // update
        calcCaloriePercent(todayCaloriesTotal, dailyCalorieGoal)
    }
}



