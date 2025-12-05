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
import com.example.nutrifit.CalorieGoalEntity
import com.example.nutrifit.ReminderReceiver
import com.example.nutrifit.WorkoutGoalEntity
import com.example.nutrifit.databinding.FragmentGoalsBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.fragment.app.activityViewModels
import com.example.nutrifit.WorkoutEntity
import com.example.nutrifit.MealEntity
import java.time.LocalDate
import java.time.LocalDateTime


class GoalsFragment : Fragment() {

    private val workoutsViewModel: WorkoutsViewModel by activityViewModels()
    private val mealsViewModel: MealsViewModel by activityViewModels()

    private var weeklyWorkoutGoal: Int = 0
    private var dailyCalorieGoal: Int = 0

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


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
        workoutsViewModel.workouts.observe(viewLifecycleOwner) { workouts ->
            updateWorkoutFromData(workouts)
        }

        mealsViewModel.meals.observe(viewLifecycleOwner) { meals ->
            updateCaloriesFromData(meals)
        }


        dailyWorkoutTotal {
            totalDuration -> println("Total duration = $totalDuration")
        }

        createChart(List(7) { 0 })

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

        //On click listener for reminder button.
        binding.btnDailyReminder.setOnClickListener {
            scheduleDaily(20, 0, "Evening check-in", "Log today’s meals and workouts.")
            Toast.makeText(requireContext(), "Daily reminder set for 8:00 PM", Toast.LENGTH_SHORT).show()
        }

        //On click listener for reminder cancel button
        binding.btnCancelReminder.setOnClickListener {
            cancelReminders()
            Toast.makeText(requireContext(), "Reminders canceled", Toast.LENGTH_SHORT).show()
        }

        //On click listener for Workout goal button
        binding.btnWorkoutGoal.setOnClickListener{
            workoutGoalInputDialog()
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Go to Login function pulled from Profile Fragment
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

    // DEMO: fires once in N seconds (easy for presentation)
    private fun scheduleInSeconds(seconds: Int, title: String, text: String) {
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        alarmMgr().setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(title, text)
        )
    }

    //Alarm manager
    private fun alarmMgr(): AlarmManager =
        requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

    //Cancel reminders
    private fun cancelReminders() {
        alarmMgr().cancel(pendingIntent("", "")) // same PI signature cancels
    }


    ///////////////////////////////////
    //[*]WORKOUT RELATED FUNCTIONS[*]//
    ///////////////////////////////////

    //Update goal dialog function
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

                //Input validation
                //Empty
                if (input.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a number.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                //Exceeds value
                val inpValue = input.toInt()
                if (inpValue > 840) {
                    Toast.makeText(requireContext(), "Goal cannot exceed 840 minutes", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                //Update goal textview
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

    //Adds workout goal to DB
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

    //Retrieves list of workouts
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
                    val Goal: Int = doc.getLong("workoutGoal")?.toInt() ?: 0
                    println("retrieved stored value: $Goal")

                    weeklyWorkoutGoal = Goal

                    //Update goal textview
                    binding.tvWorkoutGoal.text = if (Goal > 0) "Weekly workout goal: $Goal"
                    else "No workouts logged this week."
                    workoutsViewModel.workouts.value?.let { updateWorkoutFromData(it) }
                }

                else {
                    println("Document does not exist")
                }
            }
    }

    //Calculates the total workout time for the day
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

                dailyCalorieGoal = inpValue

                val goal = CalorieGoalEntity(
                    goal = inpValue
                )

                mealsViewModel.meals.value?.let { updateCaloriesFromData(it) }


                setCalorieGoal(goal)
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    //Adds calorie goal to DB
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

    //retrieves calorie data and updates text accordingly.
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

                    binding.tvGoalsData.text =
                        if (calorieGoal > 0) "Daily Calorie goal: $calorieGoal"
                        else "Set a goal to start \ntracking your progress."

                    mealsViewModel.meals.value?.let { updateCaloriesFromData(it) }
                }


                else {
                    println("Document does not exist")
                }
            }
    }


    /////////////////////////////////
    //[*]TIME AND DATE SELECTORS[*]//
    /////////////////////////////////

    //Date Picker
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


    ///////////////////////////////
    //[*]CALCULATION FUNCTIONS[*]//
    ///////////////////////////////


    //[REQUIRES NON-STATIC INTAKE FROM MEALS WHEN FUNCTIONAL]
    //Calculate percentage to goal for Calorie Bar
    private fun calcCaloriePercent(current: Int, goal: Int) {
        val percent = if (goal <= 0) 0 else (current * 100 / goal).coerceIn(0, 100)
        lifecycleScope.launch {
            calorieProgressBar(percent)
        }
    }


    //[ACCRUED WEEKLY VALUE TO AS C:]
    //Calculate percentage to goal for Workout Bar
    private fun calcWorkoutPercent(current: Int, goal: Int) {
        val percent = if (goal <= 0) 0 else (current * 100 / goal).coerceIn(0, 100)
        lifecycleScope.launch {
            workoutProgressBar(percent)
        }
    }


    /////////////////////////////////
    //[*]CHART RELATED FUNCTIONS[*]//
    /////////////////////////////////

    //Updates calorie progress bar
    private suspend fun calorieProgressBar(progress: Int){
        for (i in 1..progress){
                binding.calorieProgressBar.setProgress(i, true)
                delay(35)
        }
    }

    //Updates workout progress bar
    private suspend fun workoutProgressBar(progress: Int){
        for (i in 1..progress){
            binding.workoutProgressBar.setProgress(i, true)
            delay(35)
        }
    }

    //[USE THIS METHOD TO INSERT AND UPDATE VALUES IN THE CHART]
    //Creates the chart [WIP]
    private fun createChart(weekMinutes: List<Int>) {
        binding.goalChart.axisRight.setDrawLabels(false)
        binding.goalChart.description.isEnabled = false
        binding.goalChart.axisLeft.axisMinimum = 0f
        binding.goalChart.setScaleEnabled(false)

        val entries = ArrayList<BarEntry>()
        weekMinutes.forEachIndexed { index, minutes ->
            entries.add(BarEntry(index.toFloat(), minutes.toFloat()))
        }


        //Assigns  to variables to be pushed to chart
        val dataSet = BarDataSet(entries, "Minutes worked out")
        val barData: BarData = BarData(dataSet)

        binding.goalChart.setData(barData)
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

            }
        }

        // update
        calcWorkoutPercent(weekTotal, weeklyWorkoutGoal)

        //  chart
        createChart(perDay)
    }

    private fun updateCaloriesFromData(meals: List<MealEntity>) {
        val today = LocalDate.now()
        var todayCalories = 0

        for (m in meals) {
            try {
                val dt = LocalDateTime.parse(m.dateTime)
                if (dt.toLocalDate() == today) {
                    todayCalories += m.calories
                }
            } catch (_: Exception) {

            }
        }

        // update
        calcCaloriePercent(todayCalories, dailyCalorieGoal)
    }

}

