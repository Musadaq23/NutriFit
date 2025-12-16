package com.example.nutrifit.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
    private lateinit var prefs: SharedPreferences

    // Canonical stored values
    private var heightCm: Int? = null
    private var weightKg: Double? = null

    private var suppressToggle = false
    private var draftDirty = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = requireContext().getSharedPreferences("profile_prefs", 0)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser ?: return goToLogin()
        binding.tvEmail.text = user.email ?: ""

        // restore toggle
        restoreUnitPrefs()
        restoreDraftIfAny()

        setupTogglesAndInputs()
        setupDraftListeners()

        val uid = user.uid
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("displayName") ?: ""
                val age = snapshot.getLong("age")?.toInt()
                val gender = snapshot.getString("gender") ?: ""

                val loadedHeight =
                    (snapshot.getDouble("height") ?: snapshot.getLong("height")?.toDouble())
                        ?.roundToInt()

                val loadedWeight =
                    snapshot.getDouble("weight") ?: snapshot.getLong("weight")?.toDouble()

                binding.tvName.text = if (name.isNotBlank()) name else "User"

                // Only overwrite inputs if user doesn't have an unsaved draft
                if (!draftDirty) {
                    binding.etDisplayName.setText(name)
                    if (age != null) binding.etAge.setText(age.toString()) else binding.etAge.setText("")
                    binding.etGender.setText(gender)

                    heightCm = loadedHeight
                    weightKg = loadedWeight
                    populateInputsFromCanonical()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun setupDraftListeners() {
        binding.etDisplayName.doAfterTextChanged { markDraftDirtyAndCache() }
        binding.etAge.doAfterTextChanged { markDraftDirtyAndCache() }
        binding.etGender.doAfterTextChanged { markDraftDirtyAndCache() }

        binding.etHeightCm.doAfterTextChanged { markDraftDirtyAndCache() }
        binding.etHeightFt.doAfterTextChanged { markDraftDirtyAndCache() }
        binding.etHeightIn.doAfterTextChanged { markDraftDirtyAndCache() }

        binding.etWeight.doAfterTextChanged { markDraftDirtyAndCache() }
    }

    private fun markDraftDirtyAndCache() {
        draftDirty = true
        cacheDraftFromInputs()
    }

    private fun setupTogglesAndInputs() {
        // Height toggle
        binding.tgHeightUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressToggle) return@addOnButtonCheckedListener

            if (checkedId == binding.btnHeightCm.id) {
                // ft/in -> cm
                val ft = parseInt(binding.etHeightFt.text?.toString().orEmpty())
                val inch = parseInt(binding.etHeightIn.text?.toString().orEmpty())
                if (ft != null && inch != null) {
                    val cm = ftInToCm(ft, inch).roundToInt()
                    heightCm = cm
                    binding.etHeightCm.setText(cm.toString())
                }
                binding.tilHeightCm.visibility = View.VISIBLE
                binding.groupHeightFtIn.visibility = View.GONE
            } else {
                // cm -> ft/in
                val cm = parseInt(binding.etHeightCm.text?.toString().orEmpty())
                if (cm != null) {
                    heightCm = cm
                    val (ft, inch) = cmToFtIn(cm.toDouble())
                    binding.etHeightFt.setText(ft.toString())
                    binding.etHeightIn.setText(inch.toString())
                }
                binding.tilHeightCm.visibility = View.GONE
                binding.groupHeightFtIn.visibility = View.VISIBLE
            }

            saveUnitPrefs()
            markDraftDirtyAndCache()
        }

        // Weight toggle
        binding.tgWeightUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressToggle) return@addOnButtonCheckedListener

            val currentInt = parseInt(binding.etWeight.text?.toString().orEmpty()) ?: run {
                saveUnitPrefs()
                markDraftDirtyAndCache()
                return@addOnButtonCheckedListener
            }

            if (checkedId == binding.btnWeightKg.id) {
                // lb -> kg
                val kg = currentInt.toDouble() / LB_PER_KG
                weightKg = kg
                binding.etWeight.setText(kg.roundToInt().toString())
            } else {
                // kg -> lb
                val kg = currentInt.toDouble()
                weightKg = kg
                val lb = (kg * LB_PER_KG).roundToInt()
                binding.etWeight.setText(lb.toString())
            }

            saveUnitPrefs()
            markDraftDirtyAndCache()
        }

        // apply height visibility based on current checked toggle
        if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
            binding.tilHeightCm.visibility = View.VISIBLE
            binding.groupHeightFtIn.visibility = View.GONE
        } else {
            binding.tilHeightCm.visibility = View.GONE
            binding.groupHeightFtIn.visibility = View.VISIBLE
        }
    }

    private fun populateInputsFromCanonical() {
        val cm = heightCm ?: 170
        val kg = weightKg ?: 75.0

        // Height display depends on toggle
        if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
            binding.tilHeightCm.visibility = View.VISIBLE
            binding.groupHeightFtIn.visibility = View.GONE
            binding.etHeightCm.setText(cm.toString())
        } else {
            binding.tilHeightCm.visibility = View.GONE
            binding.groupHeightFtIn.visibility = View.VISIBLE
            val (ft, inch) = cmToFtIn(cm.toDouble())
            binding.etHeightFt.setText(ft.toString())
            binding.etHeightIn.setText(inch.toString())
        }

        // Weight display depends on toggle
        if (binding.tgWeightUnit.checkedButtonId == binding.btnWeightKg.id) {
            binding.etWeight.setText(kg.roundToInt().toString())
        } else {
            val lb = (kg * LB_PER_KG).roundToInt()
            binding.etWeight.setText(lb.toString())
        }
    }

    private fun saveProfile(uid: String) {
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Saving..."

        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (_binding != null && !binding.btnSaveProfile.isEnabled) {
                binding.btnSaveProfile.text = "Save Profile"
                binding.btnSaveProfile.isEnabled = true
                Toast.makeText(requireContext(), "Save timed out. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 5000)

        fun cancelTimeout() {
            timeoutHandler.removeCallbacks(timeoutRunnable)
        }

        fun resetButton() {
            if (_binding == null) return
            binding.btnSaveProfile.text = "Save Profile"
            binding.btnSaveProfile.isEnabled = true
        }

        val name = binding.etDisplayName.text.toString().trim()
        val age = binding.etAge.text.toString().trim().toIntOrNull()
        val gender = binding.etGender.text.toString().trim()

        val cmValue: Int? =
            if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
                parseInt(binding.etHeightCm.text?.toString().orEmpty())
            } else {
                val ft = parseInt(binding.etHeightFt.text?.toString().orEmpty())
                val inch = parseInt(binding.etHeightIn.text?.toString().orEmpty())
                if (ft != null && inch != null) ftInToCm(ft, inch).roundToInt() else null
            }

        val kgValue: Double? =
            if (binding.tgWeightUnit.checkedButtonId == binding.btnWeightKg.id) {
                parseInt(binding.etWeight.text?.toString().orEmpty())?.toDouble()
            } else {
                parseInt(binding.etWeight.text?.toString().orEmpty())?.toDouble()?.div(LB_PER_KG)
            }

        if (cmValue == null) {
            cancelTimeout()
            Toast.makeText(requireContext(), "Please enter a valid height.", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }
        if (kgValue == null) {
            cancelTimeout()
            Toast.makeText(requireContext(), "Please enter a valid weight.", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        heightCm = cmValue
        weightKg = kgValue

        val data = mutableMapOf<String, Any>(
            "displayName" to name,
            "gender" to gender,
            "height" to cmValue,
            "weight" to kgValue
        )
        if (age != null) data["age"] = age

        db.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                cancelTimeout()

                if (_binding == null) return@addOnSuccessListener

                binding.tvName.text = if (name.isNotBlank()) name else binding.tvName.text
                Toast.makeText(requireContext(), "Profile saved.", Toast.LENGTH_SHORT).show()

                binding.btnSaveProfile.text = "Profile Saved"

                clearDraft()
                populateInputsFromCanonical()
                saveUnitPrefs()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null) resetButton()
                }, 1200)
            }
            .addOnFailureListener { e ->
                cancelTimeout()
                Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                resetButton()
            }
    }

    private fun resetSaveButton() {
        binding.btnSaveProfile.text = "Save Profile"
        binding.btnSaveProfile.isEnabled = true
    }

    private fun saveUnitPrefs() {
        prefs.edit()
            .putInt("height_unit_id", binding.tgHeightUnit.checkedButtonId)
            .putInt("weight_unit_id", binding.tgWeightUnit.checkedButtonId)
            .apply()
    }

    private fun restoreUnitPrefs() {
        val heightId = prefs.getInt("height_unit_id", binding.btnHeightCm.id)
        val weightId = prefs.getInt("weight_unit_id", binding.btnWeightKg.id)

        suppressToggle = true
        binding.tgHeightUnit.check(heightId)
        binding.tgWeightUnit.check(weightId)
        suppressToggle = false

        if (heightId == binding.btnHeightCm.id) {
            binding.tilHeightCm.visibility = View.VISIBLE
            binding.groupHeightFtIn.visibility = View.GONE
        } else {
            binding.tilHeightCm.visibility = View.GONE
            binding.groupHeightFtIn.visibility = View.VISIBLE
        }
    }



    private fun cacheDraftFromInputs() {
        val editor = prefs.edit()

        // store canonical height cm if possible
        val cmValue: Int? =
            if (binding.tgHeightUnit.checkedButtonId == binding.btnHeightCm.id) {
                parseInt(binding.etHeightCm.text?.toString().orEmpty())
            } else {
                val ft = parseInt(binding.etHeightFt.text?.toString().orEmpty())
                val inch = parseInt(binding.etHeightIn.text?.toString().orEmpty())
                if (ft != null && inch != null) ftInToCm(ft, inch).roundToInt() else null
            }

        // store canonical weight kg if possible
        val kgValue: Double? =
            if (binding.tgWeightUnit.checkedButtonId == binding.btnWeightKg.id) {
                parseInt(binding.etWeight.text?.toString().orEmpty())?.toDouble()
            } else {
                parseInt(binding.etWeight.text?.toString().orEmpty())?.toDouble()?.div(LB_PER_KG)
            }

        editor.putBoolean("draft_dirty", true)
        editor.putString("draft_name", binding.etDisplayName.text?.toString() ?: "")
        editor.putString("draft_gender", binding.etGender.text?.toString() ?: "")
        editor.putString("draft_age", binding.etAge.text?.toString() ?: "")

        if (cmValue != null) editor.putInt("draft_height_cm", cmValue)
        if (kgValue != null) editor.putFloat("draft_weight_kg", kgValue.toFloat())

        editor.apply()
    }

    private fun restoreDraftIfAny() {
        draftDirty = prefs.getBoolean("draft_dirty", false)
        if (!draftDirty) return

        binding.etDisplayName.setText(prefs.getString("draft_name", "") ?: "")
        binding.etGender.setText(prefs.getString("draft_gender", "") ?: "")
        binding.etAge.setText(prefs.getString("draft_age", "") ?: "")

        if (prefs.contains("draft_height_cm")) {
            heightCm = prefs.getInt("draft_height_cm", 170)
        }
        if (prefs.contains("draft_weight_kg")) {
            weightKg = prefs.getFloat("draft_weight_kg", 75f).toDouble()
        }

        populateInputsFromCanonical()
    }

    private fun clearDraft() {
        draftDirty = false
        prefs.edit()
            .putBoolean("draft_dirty", false)
            .remove("draft_name")
            .remove("draft_gender")
            .remove("draft_age")
            .remove("draft_height_cm")
            .remove("draft_weight_kg")
            .apply()
    }

    override fun onPause() {
        super.onPause()
        saveUnitPrefs()
        if (draftDirty) cacheDraftFromInputs()
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

    private fun parseInt(s: String): Int? = s.trim().toIntOrNull()

    private fun cmToFtIn(cm: Double): Pair<Int, Int> {
        val totalInches = (cm / CM_PER_IN).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return feet to inches
    }

    private fun ftInToCm(ft: Int, inch: Int): Double {
        val totalInches = ft * 12 + inch
        return totalInches * CM_PER_IN
    }

    companion object {
        private const val CM_PER_IN = 2.54
        private const val LB_PER_KG = 2.2046226218
    }
}