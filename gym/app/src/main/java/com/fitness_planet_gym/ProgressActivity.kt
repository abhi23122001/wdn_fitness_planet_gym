package com.fitness_planet_gym

import android.os.Bundle
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        loadProgress()
    }

    private fun loadProgress() {
        repository.loadProgressData { weights, workouts, _ ->
            runOnUiThread {
                val current = findViewById<TextView>(R.id.currentWeight)
                val starting = findViewById<TextView>(R.id.startingWeight)
                val change = findViewById<TextView>(R.id.weightChange)
                val weightHistory = findViewById<TextView>(R.id.weightHistoryProgress)
                val workoutHistory = findViewById<TextView>(R.id.workoutHistory)
                findViewById<TextView>(R.id.totalWorkoutsProgress).text = workouts.size.toString()
                findViewById<TextView>(R.id.attendanceProgress).text = "Loading..."
                loadAttendanceAndMembership()

                if (weights.isNotEmpty()) {
                    val first = weights.first().first.toDoubleOrNull()
                    val last = weights.last().first.toDoubleOrNull()
                    current.text = weights.last().first + " kg"
                    starting.text = weights.first().first + " kg"
                    if (first != null && last != null) {
                        val diff = last - first
                        change.text = (if (diff >= 0) "+" else "") + String.format(Locale.US, "%.1f", diff) + " kg"
                    }
                    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    weightHistory.text = weights.asReversed().take(10).joinToString("\n") {
                        it.first + " kg  •  " + format.format(Date(it.second))
                    }
                } else {
                    current.text = "—"
                    starting.text = "—"
                    change.text = "—"
                    weightHistory.text = "No weight entries yet."
                }

                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                workoutHistory.text = if (workouts.isEmpty()) {
                    "No completed workouts yet."
                } else {
                    workouts.take(10).joinToString("\n") {
                        it.first + "  •  " + format.format(Date(it.second))
                    }
                }
            }
        }
    }

    private fun loadAttendanceAndMembership() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("members").document(uid).collection("attendance").get().addOnSuccessListener { attendance ->
            findViewById<TextView>(R.id.attendanceProgress).text = attendance.size().toString()
        }
        db.collection("members").document(uid).get().addOnSuccessListener { member ->
            val fee = member.getDouble("fee") ?: 0.0
            val end = member.getString("endDate").orEmpty()
            findViewById<TextView>(R.id.membershipProgress).text =
                if (fee > 0) "₹" + String.format(Locale.US, "%.0f", fee) + " • Valid till " + end.ifBlank { "Not set" }
                else "Membership not set"
        }
    }
}