package com.example.nutrifit.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrifit.WorkoutEntity
import com.example.nutrifit.databinding.FragmentWorkoutsBinding
import com.example.nutrifit.databinding.ItemWorkoutRowBinding
import java.time.LocalDate

class WorkoutsFragment : Fragment() {

    private var _binding: FragmentWorkoutsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutsViewModel by activityViewModels()

    private val items = mutableListOf<WorkoutEntity>()
    private lateinit var adapter: WorkoutAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvWorkoutsTitle.text = "Workouts"

        adapter = WorkoutAdapter(
            items,
            onItemClick = { workout -> showEditWorkoutDialog(workout) },
            onItemLongClick = { workout -> confirmDeleteWorkout(workout) }
        )

        binding.rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkouts.adapter = adapter

        viewModel.workouts.observe(viewLifecycleOwner) { list ->
            items.clear()
            items.addAll(list)
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }

        updateEmptyState()

        binding.fabAddWorkout.setOnClickListener {
            showAddWorkoutDialog()
        }
    }

    private fun showAddWorkoutDialog() {
        val context = requireContext()

        val etType = EditText(context).apply {
            hint = "Workout type (Ex. Full Body)"
        }
        val etDuration = EditText(context).apply {
            hint = "Duration (Minutes)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etIntensity = EditText(context).apply {
            hint = "Intensity (optional, Ex. High)"
        }
        val etCalories = EditText(context).apply {
            hint = "Calories burned (optional)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etNotes = EditText(context).apply {
            hint = "Notes (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(etType)
            addView(etDuration)
            addView(etIntensity)
            addView(etCalories)
            addView(etNotes)
        }

        AlertDialog.Builder(context)
            .setTitle("Add Workout")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val type = etType.text.toString().trim()
                val durationText = etDuration.text.toString().trim()

                if (type.isEmpty() || durationText.isEmpty()) return@setPositiveButton

                val duration = durationText.toIntOrNull() ?: return@setPositiveButton
                val intensity = etIntensity.text.toString().trim().ifBlank { null }
                val calories = etCalories.text.toString().trim().toIntOrNull()
                val notes = etNotes.text.toString().trim().ifBlank { null }
                val date = LocalDate.now().toString()

                val workout = WorkoutEntity(
                    id = "",
                    date = date,
                    type = type,
                    durationMinutes = duration,
                    intensity = intensity,
                    notes = notes,
                    caloriesBurned = calories
                )

                viewModel.addWorkout(workout)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditWorkoutDialog(workout: WorkoutEntity) {
        val context = requireContext()

        val etType = EditText(context).apply {
            hint = "Workout type"
            setText(workout.type)
        }
        val etDuration = EditText(context).apply {
            hint = "Duration"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(workout.durationMinutes.toString())
        }
        val etIntensity = EditText(context).apply {
            hint = "Intensity"
            setText(workout.intensity ?: "")
        }
        val etCalories = EditText(context).apply {
            hint = "Calories burned"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(workout.caloriesBurned?.toString() ?: "")
        }
        val etNotes = EditText(context).apply {
            hint = "Notes"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(workout.notes ?: "")
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(etType)
            addView(etDuration)
            addView(etIntensity)
            addView(etCalories)
            addView(etNotes)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val type = etType.text.toString().trim()
                val durationText = etDuration.text.toString().trim()

                if (type.isEmpty() || durationText.isEmpty()) return@setPositiveButton

                val duration = durationText.toIntOrNull() ?: return@setPositiveButton
                val intensity = etIntensity.text.toString().trim().ifBlank { null }
                val calories = etCalories.text.toString().trim().toIntOrNull()
                val notes = etNotes.text.toString().trim().ifBlank { null }

                val updated = workout.copy(
                    type = type,
                    durationMinutes = duration,
                    intensity = intensity,
                    caloriesBurned = calories,
                    notes = notes
                )

                viewModel.updateWorkout(updated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteWorkout(workout: WorkoutEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteWorkout(workout)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        if (items.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvWorkouts.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvWorkouts.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class WorkoutAdapter(
        private val items: List<WorkoutEntity>,
        private val onItemClick: (WorkoutEntity) -> Unit,
        private val onItemLongClick: (WorkoutEntity) -> Unit
    ) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

        class WorkoutViewHolder(val rowBinding: ItemWorkoutRowBinding) :
            RecyclerView.ViewHolder(rowBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val rowBinding = ItemWorkoutRowBinding.inflate(inflater, parent, false)
            return WorkoutViewHolder(rowBinding)
        }

        override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
            val item = items[position]
            holder.rowBinding.tvWorkoutTitle.text =
                "${item.type} – ${item.durationMinutes} min"
            holder.rowBinding.tvWorkoutSubtitle.text = item.date

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
