package com.example.nutrifit.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.nutrifit.MealEntity
import com.example.nutrifit.R
import com.example.nutrifit.databinding.FragmentMealsBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MealsFragment : Fragment() {

    private var _binding: FragmentMealsBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel so meals stay consistent across fragments/activities.
    private val mealsViewModel: MealsViewModel by activityViewModels()

    // Local cache of the most recent meals list, used by the picker and detail dialog.
    private var currentMeals: List<MealEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealsBinding.inflate(inflater, container, false)

        // Title is set here so it always resets correctly when the fragment is recreated.
        binding.tvMealsTitle.text = "Meals"

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe Firestore-backed meals.
        // This updates automatically when data changes (add/edit/delete).
        mealsViewModel.meals.observe(viewLifecycleOwner) { list ->
            currentMeals = list
            updateMealsText(list)
        }

        // Open Add/Edit screen for creating a new meal.
        binding.btnAddMeal.setOnClickListener { openAddMeal() }
        binding.fabAddMeal.setOnClickListener { openAddMeal() }

        // Tapping the summary text opens the meal picker so the user can view details.
        // This avoids changing your layout too much while still enabling "View Meal Details".
        binding.tvMealsData.setOnClickListener { showMealPicker() }
    }

    private fun openAddMeal() {
        // Navigate to AddEditMealFragment with no existing meal (Add flow).
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, AddEditMealFragment.newInstance(null))
            .addToBackStack(null)
            .commit()
    }

    private fun openEditMeal(meal: MealEntity) {
        // Navigate to AddEditMealFragment with an existing meal (Edit flow).
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, AddEditMealFragment.newInstance(meal))
            .addToBackStack(null)
            .commit()
    }

    private fun showMealPicker() {
        // No meals to display, so do nothing.
        if (currentMeals.isEmpty()) {
            return
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        // Create a readable list label for each meal for the selection dialog.
        val labels = currentMeals.map { meal ->
            val dtLabel = try {
                LocalDateTime.parse(meal.dateTime).format(formatter)
            } catch (_: Exception) {
                meal.dateTime
            }
            "${meal.mealType} - ${meal.calories} kcal ($dtLabel)"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select a meal")
            .setItems(labels) { _, which ->
                val selected = currentMeals[which]
                showMealDetails(selected)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMealDetails(meal: MealEntity) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val dtLabel = try {
            LocalDateTime.parse(meal.dateTime).format(formatter)
        } catch (_: Exception) {
            meal.dateTime
        }

        // Notes are optional. If none were entered, show a clear default.
        val notesText = meal.notes?.takeIf { it.isNotBlank() } ?: "None"

        // Show full saved details so users can confirm macros and notes after logging.
        val message = """
            Date/Time: $dtLabel
            
            Meal: ${meal.mealType}
            Calories: ${meal.calories} kcal
            
            Protein: ${meal.protein} g
            Carbs: ${meal.carbs} g
            Fats: ${meal.fats} g
            
            Notes: $notesText
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Meal Details")
            .setMessage(message)
            // Edit uses the same AddEditMealFragment, pre-filled with the selected meal.
            .setPositiveButton("Edit") { _, _ ->
                openEditMeal(meal)
            }
            // Delete immediately removes the Firestore document and the listener updates the UI.
            .setNeutralButton("Delete") { _, _ ->
                mealsViewModel.deleteMeal(meal)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun updateMealsText(meals: List<MealEntity>) {
        // Keep the empty label in sync with the actual data.
        val isEmpty = meals.isEmpty()
        binding.tvMealsEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

        // RecyclerView exists in the layout but is not used in this version of MealsFragment.
        // Hide it so it does not conflict visually with the summary text approach.
        binding.rvMeals.visibility = View.GONE

        // Summary view: list meal types + calories and show a quick total.
        // Details are available by tapping to open the picker.
        if (isEmpty) {
            binding.tvMealsData.text = "No meals logged yet."
            return
        }

        val builder = StringBuilder()
        var total = 0

        meals.forEach { meal ->
            builder.append("${meal.mealType}: ${meal.calories} kcal\n")
            total += meal.calories
        }

        builder.append("\nTotal: $total kcal")
        builder.append("\n\nTap here to view meal details.")

        binding.tvMealsData.text = builder.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}