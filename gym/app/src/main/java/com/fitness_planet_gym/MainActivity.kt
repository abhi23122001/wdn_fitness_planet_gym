package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("logged_in", false)
        val savedRole = prefs.getString("user_role", null)

        if (loggedIn && !savedRole.isNullOrBlank()) {
            openDashboard()
            return
        }

        if (loggedIn && savedRole.isNullOrBlank()) {
            prefs.edit().clear().apply()
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
                    val uid = auth.currentUser?.uid.orEmpty()
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(
                            mapOf(
                                "uid" to uid,
                                "role" to "member",
                                "name" to name,
                                "email" to email
                            )
                        )
                        .addOnCompleteListener {
                            saveSession(name, email, "member")
                        }
                }
                .addOnFailureListener {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.message ?: "Account creation failed", Toast.LENGTH_LONG).show()
                }
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    loadRoleAndOpen()
                }
                .addOnFailureListener {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadRoleAndOpen() {
        val user = auth.currentUser ?: run {
            loginButton.isEnabled = true
            Toast.makeText(this, "Login session not found", Toast.LENGTH_LONG).show()
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    auth.signOut()
                    loginButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "Login succeeded, but your users profile was not found in Firestore.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val role = document.getString("role")?.trim()?.lowercase().orEmpty()
                if (role != "admin" && role != "member") {
                    auth.signOut()
                    loginButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "Invalid user role in Firestore. Set role to admin or member.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val name = document.getString("name")
                    ?: user.displayName?.takeIf { it.isNotBlank() }
                    ?: "Member"

                saveSession(name, user.email.orEmpty(), role)
            }
            .addOnFailureListener { error ->
                loginButton.isEnabled = true
                Toast.makeText(
                    this,
                    "Firestore role read failed: " + (error.message ?: "permission denied"),
                    Toast.LENGTH_LONG
                ).show()
            }
    private fun saveSession(name: String, email: String, role: String) {
        getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putString("member_name", name)
            .putString("member_email", email)
            .putString("user_role", role)
            .apply()
        openDashboard()
    }

    private fun openDashboard() {
        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val role = prefs.getString("user_role", "member")?.lowercase()

        val intent = if (role == "admin") {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, DashboardActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
