package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val name = prefs.getString("member_name", "Admin") ?: "Admin"

        findViewById<TextView>(R.id.adminGreeting).text = "Welcome, $name"

        findViewById<Button>(R.id.membersButton).setOnClickListener {
            startActivity(Intent(this, AdminMembersActivity::class.java))
        }

        findViewById<Button>(R.id.attendanceButton).setOnClickListener {
            startActivity(Intent(this, AdminAttendanceActivity::class.java))
        }

        findViewById<Button>(R.id.workoutManagementButton).setOnClickListener {
            startActivity(Intent(this, AdminWorkoutManagementActivity::class.java))
        }

        findViewById<Button>(R.id.workoutMonitoringButton).setOnClickListener {
            startActivity(Intent(this, AdminWorkoutMonitoringActivity::class.java))
        }

        findViewById<Button>(R.id.memberDashboardButton).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        findViewById<Button>(R.id.reportsButton).setOnClickListener {
            startActivity(Intent(this, AdminReportsActivity::class.java))
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, AdminSettingsActivity::class.java))
        }

        findViewById<Button>(R.id.refreshAdminButton).setOnClickListener {
            loadStats()
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            prefs.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val members = snapshot.documents.count {
                    it.getString("role")?.lowercase() != "admin"
                }
                findViewById<TextView>(R.id.totalMembers).text = members.toString()
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.totalMembers).text = "—"
                Toast.makeText(this, "Could not load member count", Toast.LENGTH_SHORT).show()
            }

        firestore.collectionGroup("attendance").get()
            .addOnSuccessListener { snapshot ->
                findViewById<TextView>(R.id.totalAttendance).text = snapshot.size().toString()
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.totalAttendance).text = "—"
            }

        firestore.collectionGroup("workouts").get()
            .addOnSuccessListener { snapshot ->
                findViewById<TextView>(R.id.totalWorkouts).text = snapshot.size().toString()
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.totalWorkouts).text = "—"
            }
    }
}
