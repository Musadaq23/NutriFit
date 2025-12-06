package com.example.nutrifit

import java.io.Serializable

data class WorkoutEntity(
    val id: String = "",              // docid
    val date: String = "",
    val type: String = "",
    val durationMinutes: Int = 0,
    val intensity: String? = null,
    val notes: String? = null,
    val caloriesBurned: Int? = null
) : Serializable
