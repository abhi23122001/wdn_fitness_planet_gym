package com.fitness_planet_gym

import android.content.Intent
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WorkoutDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)
        val title=intent.getStringExtra("title") ?: "Workout"
        val level=intent.getStringExtra("level") ?: "BEGINNER"
        val duration=intent.getStringExtra("duration") ?: "30 min"
        val count=intent.getIntExtra("exercises",5)
        val description=intent.getStringExtra("description") ?: ""
        findViewById<TextView>(R.id.detailTitle).text=title
        findViewById<TextView>(R.id.detailMeta).text="$level  •  $duration  •  $count exercises"
        findViewById<TextView>(R.id.detailDescription).text=description
        findViewById<Button>(R.id.beginWorkoutButton).setOnClickListener {
            startActivity(Intent(this, WorkoutSessionActivity::class.java).apply {
                putExtra("title", title)
                putExtra("count", count)
            })
        }
    }
}