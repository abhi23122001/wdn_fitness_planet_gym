package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
        }
        saveFcmToken()

        findViewById<Button>(R.id.communityButton).setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }
        findViewById<Button>(R.id.notificationsButton).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<Button>(R.id.attendanceButton).setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
        findViewById<Button>(R.id.progressButton).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }
        findViewById<Button>(R.id.startWorkoutButton).setOnClickListener {
            startActivity(Intent(this, WorkoutPlansActivity::class.java))
        }

        loadDashboard()
    }

    private fun loadMembershipSummary() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("members").document(uid).get().addOnSuccessListener { member ->
            val fee = member.getDouble("fee") ?: 0.0
            val end = member.getString("endDate").orEmpty()
            FirebaseFirestore.getInstance().collection("members").document(uid).collection("payments").get().addOnSuccessListener { payments ->
                val paid = payments.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                val due = (fee - paid).coerceAtLeast(0.0)
                findViewById<TextView>(R.id.membershipSummary).text = if (fee > 0.0) "Membership: ₹${String.format(java.util.Locale.US, "%.0f", fee)} • Paid: ₹${String.format(java.util.Locale.US, "%.0f", paid)} • Due: ₹${String.format(java.util.Locale.US, "%.0f", due)}\nValid till: ${end.ifBlank { "Not set" }}" else "Membership not set"
            }
        }
    }

    private fun saveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken", token)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val name = prefs.getString("member_name", "Member") ?: "Member"
        findViewById<TextView>(R.id.memberGreeting).text = "Good to see you, $name"

        firebaseRepository.loadDashboardStats { workouts, attendance, _, _ ->
            runOnUiThread {
                findViewById<TextView>(R.id.workoutsCount).text = workouts.toString()
                findViewById<TextView>(R.id.attendancePercent).text =
                    if (attendance > 0) "100%" else "0%"
            }
        }
    }
}