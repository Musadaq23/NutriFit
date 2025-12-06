package com.example.nutrifit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrifit.databinding.FragmentMealsBinding

// model for a meal
data class Meal(val name: String, val calories: Int)

class MealsFragment : Fragment() {

    private var _binding: FragmentMealsBinding? = null
    private val binding get() = _binding!!

    // list of meals for this session
    private val meals = mutableListOf<Meal>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealsBinding.inflate(inflater, container, false)

        binding.tvMealsTitle.text = "Meals"

        meals.add(Meal("Breakfast", 350))
        meals.add(Meal("Lunch", 600))
        updateMealsText()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddMeal.setOnClickListener {
            showAddMealDialog()
        }
        binding.fabAddMeal.setOnClickListener {
            showAddMealDialog()
        }
    }


    // Build and show a dialog where user enters name + calories
    private fun showAddMealDialog() {
        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(context).apply {
            hint = "Meal name (e.g. Dinner)"
        }

        val caloriesInput = EditText(context).apply {
            hint = "Calories (e.g. 700)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(nameInput)
        layout.addView(caloriesInput)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Add Meal")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val calories = caloriesInput.text.toString().trim().toIntOrNull() ?: -1

                if (name.isEmpty() || calories <= 0) {
                    Toast.makeText(context, "Enter a valid name and calories.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                meals.add(Meal(name, calories))
                updateMealsText()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Update the TextView from the meals list
    private fun updateMealsText() {
        if (meals.isEmpty()) {
            binding.tvMealsData.text = "No meals logged yet."
            return
        }

        val builder = StringBuilder()
        var total = 0

        for (meal in meals) {
            builder.append("${meal.name}: ${meal.calories} kcal\n")
            total += meal.calories
        }

        builder.append("\nTotal: $total kcal")

        binding.tvMealsData.text = builder.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


