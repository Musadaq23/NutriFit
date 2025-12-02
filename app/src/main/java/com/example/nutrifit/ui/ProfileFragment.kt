package com.example.nutrifit.ui


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrifit.R
import com.google.firebase.auth.FirebaseAuth
import com.example.nutrifit.databinding.FragmentProfileBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val uid = user.uid

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val displayName = snapshot.getString("displayName") ?: ""
                val email = snapshot.getString("email") ?: user.email.orEmpty()
                val age = snapshot.getLong("age")
                val gender = snapshot.getString("gender") ?: ""
                val height = snapshot.getLong("height")
                val weight = snapshot.getLong("weight")

                binding.tvName.text = if (displayName.isNotBlank()) displayName else "User"
                binding.tvEmail.text = email

                binding.etDisplayName.setText(displayName)
                binding.etAge.setText(age?.toString() ?: "")
                binding.etGender.setText(gender)
                binding.etHeight.setText(height?.toString() ?: "")
                binding.etWeight.setText(weight?.toString() ?: "")
            }

        binding.btnSaveProfile.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()
            val gender = binding.etGender.text.toString().trim()
            val heightText = binding.etHeight.text.toString().trim()
            val weightText = binding.etWeight.text.toString().trim()

            val age = ageText.toIntOrNull()
            val height = heightText.toIntOrNull()
            val weight = weightText.toIntOrNull()

            val updates = hashMapOf(
                "displayName" to displayName,
                "age" to age,
                "gender" to gender,
                "height" to height,
                "weight" to weight
            )

            db.collection("users")
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    binding.tvName.text =
                        if (displayName.isNotBlank()) displayName else binding.tvName.text
                    Toast.makeText(requireContext(), "Profile saved.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        e.localizedMessage ?: "Failed to save profile.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

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

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

