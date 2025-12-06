package com.example.nutrifit

fun calculateProgress(current: Int, target: Int): Int {
    if (target <= 0) return 0
    val ratio = current.toFloat() / target.toFloat()
    val clamped = ratio.coerceIn(0f, 1f)
    return (clamped * 100).toInt()
}