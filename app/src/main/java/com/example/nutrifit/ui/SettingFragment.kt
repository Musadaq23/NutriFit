package com.example.nutrifit.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrifit.databinding.FragmentSettingsBinding
import com.example.nutrifit.ui.LoginActivity

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)

        binding.switchNotif.isChecked = prefs.getBoolean("notif", true)

        binding.switchNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif", isChecked).apply()
            Toast.makeText(requireContext(), "Notifications: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {

            prefs.edit().clear().apply()

            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
