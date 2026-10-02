package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var modeButton: TextView
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getSharedPreferences("fitness_planet", MODE_PRIVATE).getBoolean("logged_in", false)) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_main)

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        modeButton = findViewById(R.id.modeButton)

        try {
            auth = FirebaseAuth.getInstance()
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Firebase setup pending: add google-services.json", Toast.LENGTH_LONG).show()
            return
        }

        modeButton.setOnClickListener {
            isSignUpMode = !isSignUpMode
            nameInput.visibility = if (isSignUpMode) android.view.View.VISIBLE else android.view.View.GONE
            loginButton.text = if (isSignUpMode) "CREATE ACCOUNT" else "LOGIN"
            modeButton.text = if (isSignUpMode) "Already have an account? Login" else "New member? Create account"
        }

        loginButton.setOnClickListener {
            authenticate()
        }
    }

    private fun authenticate() {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email"
            return
        }
        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return
        }

        loginButton.isEnabled = false

        if (isSignUpMode) {
            val name = nameInput.text?.toString()?.trim().orEmpty().ifBlank { "Member" }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    saveSession(name, email)
                }
                .addOnFailureListener {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.message ?: "Account creation failed", Toast.LENGTH_LONG).show()
                }
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val name = auth.currentUser?.displayName?.ifBlank { null } ?: "Member"
                    saveSession(name, email)
                }
                .addOnFailureListener {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveSession(name: String, email: String) {
        getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putString("member_name", name)
            .putString("member_email", email)
            .apply()
        openDashboard()
    }

    private fun openDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}