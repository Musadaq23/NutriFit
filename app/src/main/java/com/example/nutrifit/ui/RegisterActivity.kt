package com.example.nutrifit.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrifit.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.register(email, password)
        }

        binding.txtGoToLogin.setOnClickListener {
            finish()
        }

        viewModel.state.observe(this) {
            when (it) {
                is AuthState.Loading -> binding.progressRegister.visibility = View.VISIBLE
                is AuthState.Success -> {
                    binding.progressRegister.visibility = View.GONE
                    Toast.makeText(this, "Account created. Please login.", Toast.LENGTH_LONG).show()
                    finish()
                }
                is AuthState.Error -> {
                    binding.progressRegister.visibility = View.GONE
                    Toast.makeText(this, it.msg, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {}
            }
        }
    }
}
