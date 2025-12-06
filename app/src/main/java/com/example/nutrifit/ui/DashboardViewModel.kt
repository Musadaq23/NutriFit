package com.example.nutrifit.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    fun refresh() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        loadTodayCalories(uid)
        loadWorkoutMinutes(uid)
    }

    private fun loadTodayCalories(uid: String) {
        val today = LocalDate.now()

        db.collection("users")
            .document(uid)
            .collection("meals")
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0
                for (doc in snapshot.documents) {
                    val dtString = doc.getString("dateTime") ?: continue
                    val calories = doc.getLong("calories")?.toInt() ?: 0

                    try {
                        val dt = LocalDateTime.parse(dtString)
                        if (dt.toLocalDate() == today) {
                            total += calories
                        }
                    } catch (_: Exception) {
                        // ignore parse error
                    }
                }
                _todayCalories.value = total
            }
            .addOnFailureListener {
                _todayCalories.value = 0
            }
    }

    private fun loadWorkoutMinutes(uid: String) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)

        db.collection("users")
            .document(uid)
            .collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                var todayTotal = 0
                var weekTotal = 0

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
                        }
                    } catch (_: Exception) {

                    }
                }

                _todayWorkoutMinutes.value = todayTotal
                _weeklyWorkoutMinutes.value = weekTotal
            }
            .addOnFailureListener {
                _todayWorkoutMinutes.value = 0
                _weeklyWorkoutMinutes.value = 0
            }
    }
}
