package com.fitness_planet_gym

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminMemberDetailsActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_member_details)

        val uid = intent.getStringExtra("uid").orEmpty()
        val name = intent.getStringExtra("name").orEmpty().ifBlank { "Member" }
        val email = intent.getStringExtra("email").orEmpty()

        findViewById<TextView>(R.id.memberName).text = name
        findViewById<TextView>(R.id.memberEmail).text = email.ifBlank { "Email not available" }
        findViewById<TextView>(R.id.memberUid).text = "UID: $uid"

        if (uid.isBlank()) {
            Toast.makeText(this, "Member UID is missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadProfile(uid)
        loadAttendance(uid)
        loadWorkouts(uid)
        loadWeight(uid)
    }

    private fun loadProfile(uid: String) {
        firestore.collection("members").document(uid).get()
            .addOnSuccessListener { doc ->
                findViewById<TextView>(R.id.memberGoal).text =
                    "Goal: " + (doc.getString("goal").orEmpty().ifBlank { "Not set" })
                findViewById<TextView>(R.id.memberBody).text =
                    "Weight: " + (doc.getString("weightKg").orEmpty().ifBlank { "—" }) +
                    " kg  •  Height: " + (doc.getString("heightCm").orEmpty().ifBlank { "—" }) + " cm"
            }
    }

    private fun loadAttendance(uid: String) {
        firestore.collection("members").document(uid).collection("attendance")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20).get()
            .addOnSuccessListener { snapshot ->
                val format = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
                val rows = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    format.format(Date(timestamp))
                }
                findViewById<TextView>(R.id.attendanceHistory).text =
                    if (rows.isEmpty()) "No attendance records." else rows.joinToString("\n")
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.attendanceHistory).text = "Unable to load attendance."
            }
    }

    private fun loadWorkouts(uid: String) {
        firestore.collection("members").document(uid).collection("workouts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20).get()
            .addOnSuccessListener { snapshot ->
                val format = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
                val rows = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    title + "  •  " + format.format(Date(timestamp))
                }
                findViewById<TextView>(R.id.workoutHistory).text =
                    if (rows.isEmpty()) "No completed workouts." else rows.joinToString("\n")
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.workoutHistory).text = "Unable to load workouts."
            }
    }

    private fun loadWeight(uid: String) {
        firestore.collection("members").document(uid).collection("weightHistory")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10).get()
            .addOnSuccessListener { snapshot ->
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val rows = snapshot.documents.mapNotNull { doc ->
                    val weight = doc.getString("weightKg") ?: return@mapNotNull null
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    weight + " kg  •  " + format.format(Date(timestamp))
                }
                findViewById<TextView>(R.id.weightHistory).text =
                    if (rows.isEmpty()) "No weight records." else rows.joinToString("\n")
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.weightHistory).text = "Unable to load weight history."
            }
    }
}
