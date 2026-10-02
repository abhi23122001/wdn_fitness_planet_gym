package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val name = getSharedPreferences("fitness_planet", MODE_PRIVATE)
            .getString("member_name", "Member")
            ?: "Member"

        findViewById<TextView>(R.id.memberGreeting).text = "Good to see you, $name"

        findViewById<Button>(R.id.startWorkoutButton).setOnClickListener {
            startActivity(Intent(this, WorkoutPlansActivity::class.java))
        }
    }
}