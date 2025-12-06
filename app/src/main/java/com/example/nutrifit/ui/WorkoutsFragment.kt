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
import java.util.Locale

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
            onDeleteClick = { workout -> confirmDeleteWorkout(workout) }
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

        val etExercise = EditText(context).apply {
            hint = "Exercise (e.g. Bench Press)"
        }
        val etMuscleGroup = EditText(context).apply {
            hint = "Muscle group (e.g. Chest)"
        }
        val etSets = EditText(context).apply {
            hint = "Sets (e.g. 4)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etReps = EditText(context).apply {
            hint = "Reps per set (e.g. 8)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etWeight = EditText(context).apply {
            hint = "Weight per rep (e.g. 135)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etDuration = EditText(context).apply {
            hint = "Duration (minutes)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val etIntensity = EditText(context).apply {
            hint = "Intensity (Low / Medium / High)"
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
            addView(etExercise)
            addView(etMuscleGroup)
            addView(etSets)
            addView(etReps)
            addView(etWeight)
            addView(etDuration)
            addView(etIntensity)
            addView(etCalories)
            addView(etNotes)
        }

        AlertDialog.Builder(context)
            .setTitle("Log Exercise")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val exerciseName = etExercise.text.toString().trim()
                val muscleGroup = etMuscleGroup.text.toString().trim()
                val sets = etSets.text.toString().trim().toIntOrNull() ?: 0
                val reps = etReps.text.toString().trim().toIntOrNull() ?: 0
                val weight = etWeight.text.toString().trim().toFloatOrNull() ?: 0f
                val duration = etDuration.text.toString().trim().toIntOrNull() ?: 0

                if (exerciseName.isEmpty() || sets <= 0 || reps <= 0 || weight <= 0f) {
                    android.widget.Toast.makeText(
                        context,
                        "Please enter exercise, sets, reps, and a weight.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val intensity = etIntensity.text.toString().trim().ifBlank { null }
                val calories = etCalories.text.toString().trim().toIntOrNull()
                val notes = etNotes.text.toString().trim().ifBlank { null }
                val date = LocalDate.now().toString()

                val workout = WorkoutEntity(
                    id = "",
                    date = date,
                    exerciseName = exerciseName,
                    muscleGroup = muscleGroup,
                    sets = sets,
                    repsPerSet = reps,
                    weightPerRep = weight,
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

        val etExercise = EditText(context).apply {
            hint = "Exercise"
            setText(workout.exerciseName)
        }
        val etMuscleGroup = EditText(context).apply {
            hint = "Muscle group"
            setText(workout.muscleGroup)
        }
        val etSets = EditText(context).apply {
            hint = "Sets"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(workout.sets.toString())
        }
        val etReps = EditText(context).apply {
            hint = "Reps per set"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(workout.repsPerSet.toString())
        }
        val etWeight = EditText(context).apply {
            hint = "Weight per rep"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(
                if (workout.weightPerRep == 0f) "" else workout.weightPerRep.toString()
            )
        }
        val etDuration = EditText(context).apply {
            hint = "Duration (minutes)"
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
            addView(etExercise)
            addView(etMuscleGroup)
            addView(etSets)
            addView(etReps)
            addView(etWeight)
            addView(etDuration)
            addView(etIntensity)
            addView(etCalories)
            addView(etNotes)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Exercise")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val exerciseName = etExercise.text.toString().trim()
                val muscleGroup = etMuscleGroup.text.toString().trim()
                val sets = etSets.text.toString().trim().toIntOrNull() ?: 0
                val reps = etReps.text.toString().trim().toIntOrNull() ?: 0
                val weight = etWeight.text.toString().trim().toFloatOrNull() ?: 0f
                val duration = etDuration.text.toString().trim().toIntOrNull() ?: 0

                if (exerciseName.isEmpty() || sets <= 0 || reps <= 0 || weight <= 0f) {
                    android.widget.Toast.makeText(
                        context,
                        "Please enter exercise, sets, reps, and a weight.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val intensity = etIntensity.text.toString().trim().ifBlank { null }
                val calories = etCalories.text.toString().trim().toIntOrNull()
                val notes = etNotes.text.toString().trim().ifBlank { null }

                val updated = workout.copy(
                    exerciseName = exerciseName,
                    muscleGroup = muscleGroup,
                    sets = sets,
                    repsPerSet = reps,
                    weightPerRep = weight,
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
            .setMessage("Are you sure you want to delete this workout?")
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
        private val onDeleteClick: (WorkoutEntity) -> Unit
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

            val weightText =
                if (item.weightPerRep % 1f == 0f)
                    String.format(Locale.US, "%d lb", item.weightPerRep.toInt())
                else
                    String.format(Locale.US, "%.1f lb", item.weightPerRep)

            val titleBuilder = StringBuilder().apply {
                append(item.exerciseName)
                if (item.sets > 0 && item.repsPerSet > 0) {
                    append(" – ")
                    append(item.sets)
                    append(" x ")
                    append(item.repsPerSet)
                    if (item.weightPerRep > 0f) {
                        append(" @ ")
                        append(weightText)
                    }
                }
            }

            val subtitleBuilder = StringBuilder().apply {
                append(item.date)

                if (item.muscleGroup.isNotBlank()) {
                    append(" • ")
                    append(item.muscleGroup)
                }

                if (item.durationMinutes > 0) {
                    append(" • ")
                    append(item.durationMinutes)
                    append(" min")
                }

                if (!item.intensity.isNullOrBlank()) {
                    append(" • ")
                    append(item.intensity)
                }

                if (item.caloriesBurned != null && item.caloriesBurned > 0) {
                    append(" • ")
                    append(item.caloriesBurned)
                    append(" kcal")
                }

                if (!item.notes.isNullOrBlank()) {
                    append(" • ")
                    append(item.notes)
                }
            }


            holder.rowBinding.tvWorkoutTitle.text = titleBuilder.toString()
            holder.rowBinding.tvWorkoutSubtitle.text = subtitleBuilder.toString()

            holder.rowBinding.root.setOnClickListener {
                onItemClick(item)
            }

            holder.rowBinding.btnDeleteWorkout.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}