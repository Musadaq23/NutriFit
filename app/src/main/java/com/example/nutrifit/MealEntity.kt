package com.example.nutrifit

import java.io.Serializable

data class MealEntity(
    val id: String = "",              //doc id
    val dateTime: String = "",
    val mealType: String = "",        // lunch.etc
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val notes: String? = null
) : Serializable
