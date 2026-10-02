package com.fitness_planet_gym

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.sessionStatus)
        val startButton = findViewById<Button>(R.id.startSessionButton)

        startButton.setOnClickListener {
            status.text = "Session started • Let's train"
            startButton.text = "SESSION ACTIVE"
            startButton.isEnabled = false
        }
    }
}