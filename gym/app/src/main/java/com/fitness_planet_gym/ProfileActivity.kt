package com.fitness_planet_gym

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var name: TextInputEditText
    private lateinit var weight: TextInputEditText
    private lateinit var height: TextInputEditText
    private lateinit var goal: TextInputEditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        name = findViewById(R.id.profileName)
        weight = findViewById(R.id.profileWeight)
        height = findViewById(R.id.profileHeight)
        goal = findViewById(R.id.profileGoal)
        status = findViewById(R.id.saveStatus)

        name.setText(prefs.getString("member_name", "Member"))
        weight.setText(prefs.getString("weight", ""))
        height.setText(prefs.getString("height", ""))
        goal.setText(prefs.getString("goal", ""))

        findViewById<Button>(R.id.saveProfileButton).setOnClickListener {
            prefs.edit()
                .putString("member_name", name.text?.toString()?.trim().orEmpty().ifBlank { "Member" })
                .putString("weight", weight.text?.toString()?.trim().orEmpty())
                .putString("height", height.text?.toString()?.trim().orEmpty())
                .putString("goal", goal.text?.toString()?.trim().orEmpty())
                .apply()
            status.text = "PROFILE SAVED ✓"
        }
    }
}