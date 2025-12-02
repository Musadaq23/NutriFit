package com.example.nutrifit.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.nutrifit.R
import com.example.nutrifit.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var goToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            openMainAndFinish()
            return
        }

        email = findViewById(R.id.emailField)
        password = findViewById(R.id.passwordField)
        loginBtn = findViewById(R.id.loginBtn)
        goToRegister = findViewById(R.id.goToRegister)

        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnSuccessListener {
                    openMainAndFinish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        e.localizedMessage ?: "Login failed. Check your email or password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        goToRegister.setOnClickListener {
            val intent = Intent(
                this@LoginActivity,
                com.example.nutrifit.ui.RegisterActivity::class.java  // fully qualified
            )
            startActivity(intent)
        }
    }
    private fun openMainAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()


    }

}