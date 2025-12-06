package com.example.nutrifit.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrifit.R
import com.example.nutrifit.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.roundToInt

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var heightCm: Int? = null
    private var weightKg: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser ?: return goToLogin()
        binding.tvEmail.text = user.email ?: ""

        setupPickers()

        val uid = user.uid
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("displayName") ?: ""
                val age = snapshot.getLong("age")?.toInt()
                val gender = snapshot.getString("gender") ?: ""
                heightCm = snapshot.getLong("height")?.toInt()
                weightKg = snapshot.getLong("weight")?.toInt()

                binding.tvName.text = if (name.isNotBlank()) name else "User"
                binding.etDisplayName.setText(name)
                if (age != null) binding.etAge.setText(age.toString())
                binding.etGender.setText(gender)

                updateHeightPicker()
                updateWeightPicker()
            }

        binding.btnSaveProfile.setOnClickListener { saveProfile(uid) }

        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.btnHelp.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, HelpFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupPickers() {
        binding.npHeight.wrapSelectorWheel = false
        binding.npWeight.wrapSelectorWheel = false

        binding.tgHeightUnit.check(binding.btnHeightCm.id)
        binding.tgWeightUnit.check(binding.btnWeightKg.id)

        updateHeightPicker()
        updateWeightPicker()

        binding.tgHeightUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            heightCm = if (checkedId == binding.btnHeightFt.id) {
                binding.npHeight.value
            } else {
                val index = binding.npHeight.value
                val totalInches = 48 + index
                (totalInches * 2.54f).roundToInt()
            }
            updateHeightPicker()
        }

        binding.tgWeightUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            weightKg = if (checkedId == binding.btnWeightLb.id) {
                binding.npWeight.value
            } else {
                val pounds = binding.npWeight.value
                (pounds / 2.20462).roundToInt()
            }
            updateWeightPicker()
        }
    }

    private fun updateHeightPicker() {
        val picker = binding.npHeight
        val cm = heightCm ?: 170

        if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
            picker.displayedValues = null
            picker.minValue = 120
            picker.maxValue = 220
            picker.value = cm.coerceIn(picker.minValue, picker.maxValue)
        } else {
            val labels = mutableListOf<String>()
            for (inch in 48..84) {
                val ft = inch / 12
                val inchPart = inch % 12
                labels.add("${ft}ft ${inchPart}in")
            }
            picker.displayedValues = null
            picker.minValue = 0
            picker.maxValue = labels.size - 1
            picker.displayedValues = labels.toTypedArray()

            val currentInches = (cm / 2.54).roundToInt()
            val index = (currentInches - 48).coerceIn(0, labels.size - 1)
            picker.value = index
        }
    }

    private fun updateWeightPicker() {
        val picker = binding.npWeight
        val kg = weightKg ?: 75

        if (binding.tgWeightUnit.checkedButtonId == binding.btnWeightKg.id) {
            picker.displayedValues = null
            picker.minValue = 40
            picker.maxValue = 200
            picker.value = kg.coerceIn(picker.minValue, picker.maxValue)
        } else {
            picker.displayedValues = null
            picker.minValue = 90
            picker.maxValue = 440
            val lb = (kg * 2.20462).roundToInt()
            picker.value = lb.coerceIn(picker.minValue, picker.maxValue)
        }
    }

    private fun saveProfile(uid: String) {
        val name = binding.etDisplayName.text.toString().trim()
        val age = binding.etAge.text.toString().trim().toIntOrNull()
        val gender = binding.etGender.text.toString().trim()

        heightCm = if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
            binding.npHeight.value
        } else {
            val index = binding.npHeight.value
            val totalInches = 48 + index
            (totalInches * 2.54f).roundToInt()
        }

        weightKg = if (binding.tgWeightUnit.checkedButtonId == binding.btnWeightKg.id) {
            binding.npWeight.value
        } else {
            val pounds = binding.npWeight.value
            (pounds / 2.20462).roundToInt()
        }

        val data = hashMapOf(
            "displayName" to name,
            "age" to age,
            "gender" to gender,
            "height" to heightCm,
            "weight" to weightKg
        )

        db.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                binding.tvName.text = if (name.isNotBlank()) name else binding.tvName.text
                Toast.makeText(requireContext(), "Profile saved.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}