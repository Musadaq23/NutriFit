package com.example.nutrifit.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nutrifit.WorkoutEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WorkoutsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _workouts = MutableLiveData<List<WorkoutEntity>>(emptyList())
    val workouts: LiveData<List<WorkoutEntity>> = _workouts

    private var listenerRegistration: ListenerRegistration? = null

    init {
        val user = auth.currentUser
        if (user != null) {
            startListening(user.uid)
        } else {
            _workouts.value = emptyList()
        }
    }

    private fun startListening(uid: String) {
        listenerRegistration?.remove()

        listenerRegistration = db.collection("users")
            .document(uid)
            .collection("workouts")
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val type = doc.getString("type") ?: return@mapNotNull null
                    val duration = doc.getLong("durationMinutes")?.toInt() ?: 0
                    val intensity = doc.getString("intensity")
                    val notes = doc.getString("notes")
                    val calories = doc.getLong("caloriesBurned")?.toInt()

                    WorkoutEntity(
                        id = 0L,
                        date = date,
                        type = type,
                        durationMinutes = duration,
                        intensity = intensity,
                        notes = notes,
                        caloriesBurned = calories
                    )
                }

                _workouts.value = list.reversed()
            }
    }

    fun addWorkout(workout: WorkoutEntity) {
        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "date" to workout.date,
            "type" to workout.type,
            "durationMinutes" to workout.durationMinutes,
            "intensity" to workout.intensity,
            "notes" to workout.notes,
            "caloriesBurned" to workout.caloriesBurned
        )

        db.collection("users")
            .document(user.uid)
            .collection("workouts")
            .add(data)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
