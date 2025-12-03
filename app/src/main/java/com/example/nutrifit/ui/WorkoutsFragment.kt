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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrifit.WorkoutEntity
import com.example.nutrifit.databinding.FragmentWorkoutsBinding
import com.example.nutrifit.databinding.ItemWorkoutRowBinding
import java.time.LocalDate

class WorkoutsFragment : Fragment() {

    private var _binding: FragmentWorkoutsBinding? = null
    private val binding get() = _binding!!

    private val workouts = mutableListOf<WorkoutEntity>()
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

        adapter = WorkoutAdapter(workouts)
        binding.rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkouts.adapter = adapter

        updateEmptyState()

        binding.fabAddWorkout.setOnClickListener {
            showAddWorkoutDialog()
        }
    }

    private fun showAddWorkoutDialog() {
        val context = requireContext()

        val etType = EditText(context).apply {
            hint = "Workout type (e.g., Upper Body)"
        }
        val etDuration = EditText(context).apply {
            hint = "Duration (minutes)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(etType)
            addView(etDuration)
        }

        AlertDialog.Builder(context)
            .setTitle("Add Workout"
            )
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val type = etType.text.toString().trim()
                val durationText = etDuration.text.toString().trim()

                if (type.isEmpty() || durationText.isEmpty()) {
                    return@setPositiveButton
                }

                val duration = durationText.toIntOrNull() ?: return@setPositiveButton
                val date = LocalDate.now().toString()

                val workout = WorkoutEntity(
                    id = 0L,
                    date = date,
                    type = type,
                    durationMinutes = duration
                )

                workouts.add(0, workout)
                adapter.notifyItemInserted(0)
                binding.rvWorkouts.scrollToPosition(0)
                updateEmptyState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        if (workouts.isEmpty()) {
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
        private val items: List<WorkoutEntity>
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
        }

        override fun getItemCount(): Int = items.size
    }
}