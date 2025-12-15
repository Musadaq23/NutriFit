package com.example.nutrifit.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate
import java.time.LocalDateTime

class DashboardViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _todayCalories = MutableLiveData(0)
    val todayCalories: LiveData<Int> = _todayCalories

    private val _todayWorkoutMinutes = MutableLiveData(0)
    val todayWorkoutMinutes: LiveData<Int> = _todayWorkoutMinutes

    private val _weeklyWorkoutMinutes = MutableLiveData(0)
    val weeklyWorkoutMinutes: LiveData<Int> = _weeklyWorkoutMinutes

    private val _caloriesLast7Days = MutableLiveData<List<Int>>(List(7) { 0 })
    val caloriesLast7Days: LiveData<List<Int>> = _caloriesLast7Days

    private val _workoutsLast7Days = MutableLiveData<List<Int>>(List(7) { 0 })
    val workoutsLast7Days: LiveData<List<Int>> = _workoutsLast7Days

    private var mealsListener: ListenerRegistration? = null
    private var workoutsListener: ListenerRegistration? = null

    fun refresh() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        startMealsListener(uid)
        startWorkoutsListener(uid)
    }

    private fun startMealsListener(uid: String) {
        mealsListener?.remove()

        mealsListener = db.collection("users")
            .document(uid)
            .collection("meals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    _todayCalories.value = 0
                    _caloriesLast7Days.value = List(7) { 0 }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val today = LocalDate.now()
                val weekStart = today.minusDays(6)

                var todayTotal = 0
                val perDay = MutableList(7) { 0 }

                for (doc in snapshot.documents) {
                    val dtString = doc.getString("dateTime") ?: continue
                    val calories = doc.getLong("calories")?.toInt() ?: 0

                    try {
                        val dt = LocalDateTime.parse(dtString)
                        val d = dt.toLocalDate()

                        if (d == today) {
                            todayTotal += calories
                        }

                        if (!d.isBefore(weekStart) && !d.isAfter(today)) {
                            val index = (d.toEpochDay() - weekStart.toEpochDay()).toInt()
                            if (index in 0..6) {
                                perDay[index] += calories
                            }
                        }
                    } catch (_: Exception) {
                        // ignore parse error
                    }
                }

                _todayCalories.value = todayTotal
                _caloriesLast7Days.value = perDay
            }
    }

    private fun startWorkoutsListener(uid: String) {
        workoutsListener?.remove()

        workoutsListener = db.collection("users")
            .document(uid)
            .collection("workouts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    _todayWorkoutMinutes.value = 0
                    _weeklyWorkoutMinutes.value = 0
                    _workoutsLast7Days.value = List(7) { 0 }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val today = LocalDate.now()
                val weekStart = today.minusDays(6)

                var todayTotal = 0
                var weekTotal = 0
                val perDay = MutableList(7) { 0 }

                for (doc in snapshot.documents) {
                    val dateString = doc.getString("date") ?: continue
                    val duration = doc.getLong("durationMinutes")?.toInt() ?: 0

                    try {
                        val d = LocalDate.parse(dateString)

                        if (d == today) {
                            todayTotal += duration
                        }

                        if (!d.isBefore(weekStart) && !d.isAfter(today)) {
                            weekTotal += duration
                            val index = (d.toEpochDay() - weekStart.toEpochDay()).toInt()
                            if (index in 0..6) {
                                perDay[index] += duration
                            }
                        }
                    } catch (_: Exception) {

                    }
                }

                _todayWorkoutMinutes.value = todayTotal
                _weeklyWorkoutMinutes.value = weekTotal
                _workoutsLast7Days.value = perDay
            }
    }

    override fun onCleared() {
        super.onCleared()
        mealsListener?.remove()
        workoutsListener?.remove()
    }
}


