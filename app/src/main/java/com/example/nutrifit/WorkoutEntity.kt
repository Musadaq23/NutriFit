package com.example.nutrifit

data class WorkoutEntity(
    val id: Long = 0L,
    val date: String = "",
    val type: String = "",
    val durationMinutes: Int = 0,
    val intensity: String? = null,
    val notes: String? = null,
    val caloriesBurned: Int? = null
)