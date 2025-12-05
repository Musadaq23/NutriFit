package com.example.nutrifit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutrifit.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDashboardTitle.text = "Progress Dashboard"

        viewModel.todayCalories.observe(viewLifecycleOwner) { updateText() }
        viewModel.todayWorkoutMinutes.observe(viewLifecycleOwner) { updateText() }
        viewModel.weeklyWorkoutMinutes.observe(viewLifecycleOwner) { updateText() }

        viewModel.refresh()
    }

    private fun updateText() {
        val calories = viewModel.todayCalories.value ?: 0
        val todayMins = viewModel.todayWorkoutMinutes.value ?: 0
        val weekMins = viewModel.weeklyWorkoutMinutes.value ?: 0

        val status = if (todayMins > 0 || calories > 0) "Right on Track!" else "No data.."

        binding.tvDashboardData.text = """
            Calories for today: $calories kcal
            Workout for today: $todayMins min
            Workout in the last 7 days: $weekMins min
            Status: $status
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
