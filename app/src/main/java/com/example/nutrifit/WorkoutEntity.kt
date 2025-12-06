package com.example.nutrifit

import java.io.Serializable

data class WorkoutEntity(
    val id: String = "",
    val date: String = "",
    val exerciseName: String = "",
    val muscleGroup: String = "",
    val sets: Int = 0,
    val repsPerSet: Int = 0,
    val weightPerRep: Float = 0f,
    val durationMinutes: Int = 0,
    val intensity: String? = null,
    val notes: String? = null,
    val caloriesBurned: Int? = null
) : Serializable {

    val totalVolume: Float
        get() = sets * repsPerSet * weightPerRep
}