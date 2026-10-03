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

    private val pushExercises = listOf(
        "Barbell Bench Press" to "4 sets × 10 reps",
        "Incline Dumbbell Press" to "3 sets × 10 reps",
        "Cable Chest Fly" to "3 sets × 12 reps",
        "Dumbbell Shoulder Press" to "3 sets × 10 reps",
        "Rope Triceps Pushdown" to "3 sets × 12 reps",
        "Overhead Triceps Extension" to "3 sets × 10 reps"
    )

    private val pullExercises = listOf(
        "Lat Pulldown" to "4 sets × 10 reps",
        "Seated Cable Row" to "3 sets × 10 reps",
        "One-Arm Dumbbell Row" to "3 sets × 10 reps",
        "Face Pull" to "3 sets × 12 reps",
        "Barbell Curl" to "3 sets × 10 reps",
        "Hammer Curl" to "3 sets × 12 reps"
    )

    private val legExercises = listOf(
        "Barbell Squat" to "4 sets × 8 reps",
        "Leg Press" to "3 sets × 10 reps",
        "Romanian Deadlift" to "3 sets × 10 reps",
        "Leg Extension" to "3 sets × 12 reps",
        "Leg Curl" to "3 sets × 12 reps",
        "Standing Calf Raise" to "4 sets × 15 reps",
        "Walking Lunges" to "3 sets × 12 reps"
    )

    private val fullBodyExercises = listOf(
        "Goblet Squat" to "3 sets × 12 reps",
        "Push Ups" to "3 sets × 10 reps",
        "Lat Pulldown" to "3 sets × 10 reps",
        "Dumbbell Shoulder Press" to "3 sets × 10 reps",
        "Dumbbell Romanian Deadlift" to "3 sets × 10 reps"
    )

    private val exercises: List<Pair<String, String>>
        get() = when (intent.getStringExtra("workoutType")) {
            "Pull" -> pullExercises
            "Legs" -> legExercises
            "Full Body Starter", "Full Body" -> fullBodyExercises
            else -> pushExercises
        }

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
        findViewById<TextView>(R.id.progressText).text =
            "EXERCISE " + (exerciseIndex + 1) + " / " + exercises.size
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
        val duration = intent.getStringExtra("duration")
            ?.filter { it.isDigit() }?.toIntOrNull() ?: 45

        FirebaseRepository().completeWorkout(title, exercises.size, duration) { success, error ->
            runOnUiThread {
                if (success) {
                    val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
                    prefs.edit()
                        .putInt("workouts_completed", prefs.getInt("workouts_completed", 0) + 1)
                        .apply()

                    findViewById<TextView>(R.id.exerciseTitle).text = "WORKOUT COMPLETE 🎉"
                    findViewById<TextView>(R.id.exerciseMeta).text =
                        "Workout saved to your progress."
                    completeButton.text = "FINISH"
                    completeButton.isEnabled = true
                    completeButton.setOnClickListener { finish() }
                } else {
                    completionSaved = false
                    completeButton.isEnabled = true
                    findViewById<TextView>(R.id.exerciseTitle).text = "SAVE FAILED"
                    Toast.makeText(
                        this,
                        error ?: "Unable to save workout",
                        Toast.LENGTH_LONG
                    ).show()
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