package com.example.nutrifit.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrifit.R
import com.example.nutrifit.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var db: FirebaseFirestore
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        email = findViewById(R.id.regEmail)
        password = findViewById(R.id.regPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)

        btnRegister.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        Toast.makeText(this, "User not created.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }


                    val uid = user.uid
                    val profile = hashMapOf(
                        "email" to emailText,
                        "displayName" to "",
                        "age" to null,
                        "gender" to "",
                        "height" to null,
                        "weight" to null
                    )

                    db.collection("users")
                        .document(uid)
                        .set(profile)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show()
                            openMainAndFinish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                e.localizedMessage ?: "Failed to save profile.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        e.localizedMessage ?: "Registration failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun openMainAndFinish() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
