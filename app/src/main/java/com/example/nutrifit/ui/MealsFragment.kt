package com.example.nutrifit.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrifit.MealEntity
import com.example.nutrifit.R
import com.example.nutrifit.databinding.FragmentMealsBinding
import com.example.nutrifit.databinding.ItemMealRowBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MealsFragment : Fragment() {

    private var _binding: FragmentMealsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MealsViewModel by activityViewModels()

    private val items = mutableListOf<MealEntity>()
    private lateinit var adapter: MealAdapter

    private var currentFilter: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMealsTitle.text = "Meals"

        adapter = MealAdapter(
            items,
            onItemClick = { meal -> openAddEditMeal(meal) },
            onItemLongClick = { meal -> confirmDeleteMeal(meal) }
        )

        binding.rvMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeals.adapter = adapter

        setupFilterSpinner()

        viewModel.meals.observe(viewLifecycleOwner) { list ->
            applyFilterAndUpdate(list)
        }

        binding.btnAddMeal.setOnClickListener {
            openAddEditMeal(null)
        }

        updateEmptyState()
    }

    private fun setupFilterSpinner() {
        val options = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        binding.spinnerMealFilter.adapter = spinnerAdapter
        binding.spinnerMealFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentFilter = options[position]
                applyFilterAndUpdate(viewModel.meals.value ?: emptyList())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }

    private fun applyFilterAndUpdate(allMeals: List<MealEntity>) {
        val filtered = when (currentFilter) {
            "Breakfast" -> allMeals.filter { it.mealType.equals("Breakfast", ignoreCase = true) }
            "Lunch" -> allMeals.filter { it.mealType.equals("Lunch", ignoreCase = true) }
            "Dinner" -> allMeals.filter { it.mealType.equals("Dinner", ignoreCase = true) }
            "Snack" -> allMeals.filter { it.mealType.equals("Snack", ignoreCase = true) }
            else -> allMeals
        }

        items.clear()
        items.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (items.isEmpty()) {
            binding.tvEmptyMeals.visibility = View.VISIBLE
            binding.rvMeals.visibility = View.GONE
        } else {
            binding.tvEmptyMeals.visibility = View.GONE
            binding.rvMeals.visibility = View.VISIBLE
        }
    }

    private fun openAddEditMeal(meal: MealEntity?) {
        val fragment = AddEditMealFragment.newInstance(meal)
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDeleteMeal(meal: MealEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete:")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMeal(meal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MealAdapter(
        private val items: List<MealEntity>,
        private val onItemClick: (MealEntity) -> Unit,
        private val onItemLongClick: (MealEntity) -> Unit
    ) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

        private val formatterInput = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private val formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        class MealViewHolder(val rowBinding: ItemMealRowBinding) :
            RecyclerView.ViewHolder(rowBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val rowBinding = ItemMealRowBinding.inflate(inflater, parent, false)
            return MealViewHolder(rowBinding)
        }

        override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
            val item = items[position]
            holder.rowBinding.tvMealTitle.text =
                "${item.mealType} – ${item.calories} kcal"

            val subtitle = try {
                val dt = LocalDateTime.parse(item.dateTime, formatterInput)
                dt.format(formatterOutput)
            } catch (e: Exception) {
                item.dateTime
            }

            holder.rowBinding.tvMealSubtitle.text = subtitle

            holder.rowBinding.root.setOnClickListener {
                onItemClick(item)
            }
            holder.rowBinding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
