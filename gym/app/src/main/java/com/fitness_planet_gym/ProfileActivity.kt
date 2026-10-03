package com.fitness_planet_gym

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var name: TextInputEditText
    private lateinit var weight: TextInputEditText
    private lateinit var height: TextInputEditText
    private lateinit var goal: TextInputEditText
    private lateinit var status: TextView
    private lateinit var saveButton: Button
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        name = findViewById(R.id.profileName)
        weight = findViewById(R.id.profileWeight)
        height = findViewById(R.id.profileHeight)
        goal = findViewById(R.id.profileGoal)
        status = findViewById(R.id.saveStatus)
        saveButton = findViewById(R.id.saveProfileButton)

        name.setText(prefs.getString("member_name", "Member"))
        weight.setText(prefs.getString("weight", ""))
        height.setText(prefs.getString("height", ""))
        goal.setText(prefs.getString("goal", ""))

        loadFromFirebase()

        findViewById<Button>(R.id.addWeightButton).setOnClickListener { saveWeightEntry() }
        loadWeightHistory()

        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadFromFirebase() {
        status.text = "SYNCING PROFILE…"
        firebaseRepository.loadProfile { profile, error ->
            runOnUiThread {
                if (profile != null) {
                    name.setText(profile.name.ifBlank { name.text?.toString() })
                    weight.setText(profile.weightKg)
                    height.setText(profile.heightCm)
                    goal.setText(profile.goal)

                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putString("member_name", profile.name.ifBlank { "Member" })
                        .putString("weight", profile.weightKg)
                        .putString("height", profile.heightCm)
                        .putString("goal", profile.goal)
                        .apply()

                    status.text = "PROFILE SYNCED ✓"
                } else {
                    status.text = "OFFLINE PROFILE"
                    if (!error.isNullOrBlank()) {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveWeightEntry() {
        val value = weight.text?.toString()?.trim().orEmpty()
        if (value.toDoubleOrNull() == null || value.toDouble() <= 0) {
            weight.error = "Enter a valid weight"
            return
        }
        findViewById<Button>(R.id.addWeightButton).isEnabled = false
        status.text = "SAVING WEIGHT…"
        firebaseRepository.saveWeightEntry(value) { success, error ->
            runOnUiThread {
                findViewById<Button>(R.id.addWeightButton).isEnabled = true
                if (success) {
                    status.text = "WEIGHT ENTRY SAVED ✓"
                    loadWeightHistory()
                } else {
                    Toast.makeText(this, error ?: "Unable to save weight", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadWeightHistory() {
        firebaseRepository.loadWeightHistory { history, _ ->
            runOnUiThread {
                val historyText = findViewById<TextView>(R.id.weightHistory)
                if (history.isEmpty()) {
                    historyText.text = "No weight entries yet."
                } else {
                    val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    historyText.text = history.joinToString("\n") { entry ->
                        entry.first + " kg  •  " + format.format(java.util.Date(entry.second))
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val profile = MemberProfile(
            name = name.text?.toString()?.trim().orEmpty().ifBlank { "Member" },
            weightKg = weight.text?.toString()?.trim().orEmpty(),
            heightCm = height.text?.toString()?.trim().orEmpty(),
            goal = goal.text?.toString()?.trim().orEmpty()
        )

        saveButton.isEnabled = false
        status.text = "SAVING TO FIREBASE…"

        firebaseRepository.saveProfile(profile) { success, error ->
            runOnUiThread {
                saveButton.isEnabled = true
                if (success) {
                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putString("member_name", profile.name)
                        .putString("weight", profile.weightKg)
                        .putString("height", profile.heightCm)
                        .putString("goal", profile.goal)
                        .apply()
                    status.text = "PROFILE SAVED & SYNCED ✓"
                } else {
                    status.text = "SAVE FAILED"
                    Toast.makeText(this, error ?: "Unable to save profile", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}