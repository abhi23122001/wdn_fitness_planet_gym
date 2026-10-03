package com.fitness_planet_gym

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminReportsActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_reports)
        container = findViewById(R.id.reportsContainer)
        loadReport()
    }

    private fun loadReport() {
        db.collection("users").get().addOnSuccessListener { users ->
            val members = users.documents.filter { it.getString("role")?.lowercase() != "admin" }
            val memberCount = members.size
            var attendance = 0
            var workouts = 0
            var weightEntries = 0
            var completed = 0

            if (members.isEmpty()) {
                render(0, 0, 0, 0)
                return@addOnSuccessListener
            }

            members.forEach { user ->
                val ref = db.collection("members").document(user.id)
                ref.collection("attendance").get().addOnSuccessListener { attendance += it.size() }.addOnCompleteListener {
                    completed++
                    if (completed == members.size * 3) render(memberCount, attendance, workouts, weightEntries)
                }
                ref.collection("workouts").get().addOnSuccessListener { workouts += it.size() }.addOnCompleteListener {
                    completed++
                    if (completed == members.size * 3) render(memberCount, attendance, workouts, weightEntries)
                }
                ref.collection("weightHistory").get().addOnSuccessListener { weightEntries += it.size() }.addOnCompleteListener {
                    completed++
                    if (completed == members.size * 3) render(memberCount, attendance, workouts, weightEntries)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Could not load reports", Toast.LENGTH_LONG).show()
        }
    }

    private fun render(members: Int, attendance: Int, workouts: Int, weights: Int) {
        container.removeAllViews()
        addMetric("TOTAL MEMBERS", members.toString())
        addMetric("ATTENDANCE RECORDS", attendance.toString())
        addMetric("COMPLETED WORKOUTS", workouts.toString())
        addMetric("WEIGHT ENTRIES", weights.toString())
    }

    private fun addMetric(label: String, value: String) {
        val card = com.google.android.material.card.MaterialCardView(this).apply {
            setCardBackgroundColor(resources.getColor(R.color.fitness_surface, theme))
            radius = 20f
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
        }
        val text = TextView(this).apply {
            text = label + "\n" + value
            textSize = 19f
            setTextColor(resources.getColor(R.color.fitness_text, theme))
            setPadding(20, 20, 20, 20)
        }
        card.addView(text)
        container.addView(card)
    }
}