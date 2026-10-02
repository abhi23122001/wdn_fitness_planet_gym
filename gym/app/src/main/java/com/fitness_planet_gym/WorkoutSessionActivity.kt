package com.fitness_planet_gym

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WorkoutSessionActivity : AppCompatActivity() {
    private var exerciseIndex = 0
    private var timer: CountDownTimer? = null

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
        showExercise()

        findViewById<Button>(R.id.completeExerciseButton).setOnClickListener {
            exerciseIndex++
            if (exerciseIndex >= exercises.size) {
                findViewById<TextView>(R.id.exerciseTitle).text = "WORKOUT COMPLETE 🎉"
                findViewById<TextView>(R.id.exerciseMeta).text = "Great session. Progress saved."
                findViewById<Button>(R.id.completeExerciseButton).text = "FINISH"
                findViewById<Button>(R.id.completeExerciseButton).setOnClickListener { finish() }
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

    private fun startRestTimer() {
        timer?.cancel()
        findViewById<TextView>(R.id.restTimer).text = "REST 60s"
        timer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(ms: Long) { findViewById<TextView>(R.id.restTimer).text = "REST " + (ms / 1000) + "s" }
            override fun onFinish() { findViewById<TextView>(R.id.restTimer).text = "READY" }
        }.start()
    }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }
}