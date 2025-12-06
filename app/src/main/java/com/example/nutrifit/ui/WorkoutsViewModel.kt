package com.example.nutrifit.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nutrifit.WorkoutEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class WorkoutsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _workouts = MutableLiveData<List<WorkoutEntity>>(emptyList())
    val workouts: LiveData<List<WorkoutEntity>> = _workouts

    private var listenerRegistration: ListenerRegistration? = null

    init {
        val user = auth.currentUser
        if (user != null) startListening(user.uid) else _workouts.value = emptyList()
    }

    private fun startListening(uid: String) {
        listenerRegistration?.remove()
        listenerRegistration = db.collection("users")
            .document(uid)
            .collection("workouts")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    WorkoutEntity(
                        id = doc.id,
                        date = doc.getString("date") ?: return@mapNotNull null,
                        exerciseName = doc.getString("exerciseName") ?: "",
                        muscleGroup = doc.getString("muscleGroup") ?: "",
                        sets = doc.getLong("sets")?.toInt() ?: 0,
                        repsPerSet = doc.getLong("repsPerSet")?.toInt() ?: 0,
                        weightPerRep = doc.getDouble("weightPerRep")?.toFloat() ?: 0f,
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        intensity = doc.getString("intensity"),
                        notes = doc.getString("notes"),
                        caloriesBurned = doc.getLong("caloriesBurned")?.toInt()
                    )
                }
                _workouts.value = list
            }
    }

    fun addWorkout(workout: WorkoutEntity) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(
            "date" to workout.date,
            "exerciseName" to workout.exerciseName,
            "muscleGroup" to workout.muscleGroup,
            "sets" to workout.sets,
            "repsPerSet" to workout.repsPerSet,
            "weightPerRep" to workout.weightPerRep,
            "durationMinutes" to workout.durationMinutes,
            "intensity" to workout.intensity,
            "notes" to workout.notes,
            "caloriesBurned" to workout.caloriesBurned,
            "type" to workout.exerciseName
        )
        db.collection("users")
            .document(user.uid)
            .collection("workouts")
            .add(data)
    }

    fun updateWorkout(workout: WorkoutEntity) {
        val user = auth.currentUser ?: return
        if (workout.id.isBlank()) return
        val data = hashMapOf(
            "date" to workout.date,
            "exerciseName" to workout.exerciseName,
            "muscleGroup" to workout.muscleGroup,
            "sets" to workout.sets,
            "repsPerSet" to workout.repsPerSet,
            "weightPerRep" to workout.weightPerRep,
            "durationMinutes" to workout.durationMinutes,
            "intensity" to workout.intensity,
            "notes" to workout.notes,
            "caloriesBurned" to workout.caloriesBurned,
            "type" to workout.exerciseName
        )
        db.collection("users")
            .document(user.uid)
            .collection("workouts")
            .document(workout.id)
            .set(data)
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        val user = auth.currentUser ?: return
        if (workout.id.isBlank()) return
        db.collection("users")
            .document(user.uid)
            .collection("workouts")
            .document(workout.id)
            .delete()
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}