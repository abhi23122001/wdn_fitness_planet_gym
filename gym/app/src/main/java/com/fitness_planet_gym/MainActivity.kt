package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getSharedPreferences("fitness_planet", MODE_PRIVATE).getBoolean("logged_in", false)) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_main)

        val nameInput = findViewById<TextInputEditText>(R.id.nameInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val name = nameInput.text?.toString()?.trim().orEmpty()
            getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                .putBoolean("logged_in", true)
                .putString("member_name", name.ifBlank { "Member" })
                .apply()
            openDashboard()
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}