package com.example.nutrifit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutrifit.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

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

        setupCharts()

        viewModel.todayCalories.observe(viewLifecycleOwner) { updateText() }
        viewModel.todayWorkoutMinutes.observe(viewLifecycleOwner) { updateText() }
        viewModel.weeklyWorkoutMinutes.observe(viewLifecycleOwner) { updateText() }

        viewModel.caloriesLast7Days.observe(viewLifecycleOwner) { list ->
            renderBarChart(
                values = list,
                label = "Calories (last 7 days)",
                descriptionText = "Calories trend",
                target = binding.chartCalories7Days
            )
        }

        viewModel.workoutsLast7Days.observe(viewLifecycleOwner) { list ->
            renderBarChart(
                values = list,
                label = "Workout minutes (last 7 days)",
                descriptionText = "Workout trend",
                target = binding.chartWorkouts7Days
            )
        }

        viewModel.refresh()
    }

    private fun setupCharts() {
        binding.chartCalories7Days.axisRight.isEnabled = false
        binding.chartCalories7Days.description = Description().apply { text = "" }
        binding.chartCalories7Days.setScaleEnabled(false)

        binding.chartWorkouts7Days.axisRight.isEnabled = false
        binding.chartWorkouts7Days.description = Description().apply { text = "" }
        binding.chartWorkouts7Days.setScaleEnabled(false)
    }

    private fun renderBarChart(
        values: List<Int>,
        label: String,
        descriptionText: String,
        target: com.github.mikephil.charting.charts.BarChart
    ) {
        val entries = ArrayList<BarEntry>()
        values.forEachIndexed { index, v ->
            entries.add(BarEntry(index.toFloat(), v.toFloat()))
        }

        val dataSet = BarDataSet(entries, label)
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        target.data = data
        target.setFitBars(true)
        target.description = Description().apply { text = descriptionText }
        target.invalidate()
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

