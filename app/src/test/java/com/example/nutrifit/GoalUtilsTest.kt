package com.example.nutrifit

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalUtilsTest {

    @Test
    fun progress_zeroCurrent_returnsZero() {
        val result = calculateProgress(current = 0, target = 2000)
        assertEquals(0, result)
    }

    @Test
    fun progress_atTarget_returnsHundred() {
        val result = calculateProgress(current = 2000, target = 2000)
        assertEquals(100, result)
    }

    @Test
    fun progress_overTarget_isClampedToHundred() {
        val result = calculateProgress(current = 2500, target = 2000)
        assertEquals(100, result)
    }
}