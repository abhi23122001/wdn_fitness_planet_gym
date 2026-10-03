package com.fitness_planet_gym

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSettingsActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_settings)

        val email = findViewById<TextView>(R.id.adminSettingsEmail)
        val name = findViewById<EditText>(R.id.adminSettingsName)
        val save = findViewById<Button>(R.id.saveAdminSettingsButton)
        val change = findViewById<Button>(R.id.changePasswordButton)

        email.text = auth.currentUser?.email ?: "Admin"

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener {
                name.setText(it.getString("name").orEmpty())
            }
        }

        save.setOnClickListener {
            val value = name.text.toString().trim()
            if (value.isBlank()) {
                Toast.makeText(this, "Enter admin name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(uid).update("name", value)
                .addOnSuccessListener {
                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit().putString("member_name", value).apply()
                    Toast.makeText(this, "Admin profile updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { Toast.makeText(this, "Update failed: " + it.message, Toast.LENGTH_LONG).show() }
        }

        change.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            val password = findViewById<EditText>(R.id.newAdminPassword).text.toString()
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            user.updatePassword(password).addOnSuccessListener {
                Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show()
                findViewById<EditText>(R.id.newAdminPassword).text.clear()
            }.addOnFailureListener {
                Toast.makeText(this, "Password change failed. Re-login may be required.", Toast.LENGTH_LONG).show()
            }
        }
    }
}