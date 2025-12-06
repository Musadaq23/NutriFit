package com.example.nutrifit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutrifit.MealEntity
import com.example.nutrifit.databinding.FragmentAddEditMealBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AddEditMealFragment : Fragment() {

    private var _binding: FragmentAddEditMealBinding? = null
    private val binding get() = _binding!!

    private val mealsViewModel: MealsViewModel by activityViewModels()

    private var existingMeal: MealEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("Deprecation")
        existingMeal = arguments?.getSerializable(ARG_MEAL) as? MealEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        if (existingMeal == null) {
            val now = LocalDateTime.now()
            binding.tvAddEditMealTitle.text = "Add"
            binding.tvMealDateTime.text = now.format(formatter)
        } else {
            val meal = existingMeal!!
            binding.tvAddEditMealTitle.text = "Edit"
            binding.etMealType.setText(meal.mealType)
            binding.etCalories.setText(meal.calories.toString())
            binding.etProtein.setText(meal.protein.toString())
            binding.etCarbs.setText(meal.carbs.toString())
            binding.etFats.setText(meal.fats.toString())
            binding.etNotes.setText(meal.notes ?: "")

            val parsed = try {
                LocalDateTime.parse(meal.dateTime)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
            binding.tvMealDateTime.text = parsed.format(formatter)
        }

        binding.btnSaveMeal.setOnClickListener {
            saveMeal()
        }

        binding.btnCancelMeal.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveMeal() {
        val mealType = binding.etMealType.text.toString().trim()
        val caloriesText = binding.etCalories.text.toString().trim()
        val proteinText = binding.etProtein.text.toString().trim()
        val carbsText = binding.etCarbs.text.toString().trim()
        val fatsText = binding.etFats.text.toString().trim()
        val notesText = binding.etNotes.text.toString().trim()

        if (mealType.isEmpty() || caloriesText.isEmpty()) {
            Toast.makeText(requireContext(), "You must add meal type and calories.", Toast.LENGTH_SHORT).show()
            return
        }

        val calories = caloriesText.toIntOrNull()
        if (calories == null) {
            Toast.makeText(requireContext(), "Number needed.", Toast.LENGTH_SHORT).show()
            return
        }

        val protein = proteinText.toIntOrNull() ?: 0
        val carbs = carbsText.toIntOrNull() ?: 0
        val fats = fatsText.toIntOrNull() ?: 0
        val notes = notesText.ifBlank { null }

        val now = LocalDateTime.now()
        val dateTimeString = existingMeal?.dateTime ?: now.toString()

        val meal = MealEntity(
            id = existingMeal?.id ?: "",
            dateTime = dateTimeString,
            mealType = mealType,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats,
            notes = notes
        )

        if (existingMeal == null) {
            mealsViewModel.addMeal(meal)
            Toast.makeText(requireContext(), "Added meal.", Toast.LENGTH_SHORT).show()
        } else {
            mealsViewModel.updateMeal(meal)
            Toast.makeText(requireContext(), "Updated meal.", Toast.LENGTH_SHORT).show()
        }

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEAL = "arg_meal"

        fun newInstance(meal: MealEntity?): AddEditMealFragment {
            val fragment = AddEditMealFragment()
            if (meal != null) {
                val args = Bundle()
                args.putSerializable(ARG_MEAL, meal)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
