package com.fitness_planet_gym

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WorkoutSessionActivity : AppCompatActivity() {
    private var exerciseIndex = 0
    private var timer: CountDownTimer? = null
    private var completionSaved = false
    private lateinit var completeButton: Button

    private val exercises = listOf(
        "Barbell Bench Press" to "4 sets × 10 reps",
        "Incline Dumbbell Press" to "3 sets × 10 reps",
        "Cable Chest Fly" to "3 sets × 12 reps",
        "Rope Triceps Pushdown" to "3 sets × 12 reps",
        "Overhead Triceps Extension" to "3 sets × 10 reps"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_session)
        completeButton = findViewById(R.id.completeExerciseButton)
        showExercise()

        completeButton.setOnClickListener {
            exerciseIndex++
            if (exerciseIndex >= exercises.size) {
                finishWorkout()
                return@setOnClickListener
            }
            startRestTimer()
            showExercise()
        }

        findViewById<Button>(R.id.skipRestButton).setOnClickListener {
            timer?.cancel()
            findViewById<TextView>(R.id.restTimer).text = "READY"
        }
    }

    private fun showExercise() {
        val item = exercises[exerciseIndex]
        findViewById<TextView>(R.id.progressText).text = "EXERCISE " + (exerciseIndex + 1) + " / " + exercises.size
        findViewById<TextView>(R.id.exerciseTitle).text = item.first
        findViewById<TextView>(R.id.exerciseMeta).text = item.second
        findViewById<TextView>(R.id.restTimer).text = "READY"
    }

    private fun finishWorkout() {
        if (completionSaved) return
        completionSaved = true
        completeButton.isEnabled = false
        findViewById<TextView>(R.id.exerciseTitle).text = "SAVING WORKOUT…"

        val title = intent.getStringExtra("title") ?: "Workout"
        val duration = intent.getStringExtra("duration")?.filter { it.isDigit() }?.toIntOrNull() ?: 45

        FirebaseRepository().completeWorkout(title, exercises.size, duration) { success, error ->
            runOnUiThread {
                if (success) {
                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putInt("workouts_completed", getSharedPreferences("fitness_planet", MODE_PRIVATE).getInt("workouts_completed", 0) + 1)
                        .apply()
                    findViewById<TextView>(R.id.exerciseTitle).text = "WORKOUT COMPLETE 🎉"
                    findViewById<TextView>(R.id.exerciseMeta).text = "Workout saved to your progress."
                    completeButton.text = "FINISH"
                    completeButton.isEnabled = true
                    completeButton.setOnClickListener { finish() }
                } else {
                    completionSaved = false
                    completeButton.isEnabled = true
                    findViewById<TextView>(R.id.exerciseTitle).text = "SAVE FAILED"
                    Toast.makeText(this, error ?: "Unable to save workout", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startRestTimer() {
        timer?.cancel()
        findViewById<TextView>(R.id.restTimer).text = "REST 60s"
        timer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(ms: Long) {
                findViewById<TextView>(R.id.restTimer).text = "REST " + (ms / 1000) + "s"
            }
            override fun onFinish() {
                findViewById<TextView>(R.id.restTimer).text = "READY"
            }
        }.start()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}