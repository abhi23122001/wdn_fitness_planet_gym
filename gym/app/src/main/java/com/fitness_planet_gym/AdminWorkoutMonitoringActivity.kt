package com.fitness_planet_gym

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminWorkoutMonitoringActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var summary: TextView
    private var records = emptyList<WorkoutRow>()

    data class WorkoutRow(val name: String, val email: String, val uid: String, val title: String,
        val exerciseCount: Long, val duration: Long, val timestamp: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_workout_monitoring)
        container = findViewById(R.id.workoutContainer)
        summary = findViewById(R.id.workoutSummary)
        findViewById<EditText>(R.id.workoutSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        loadWorkouts()
    }

    private fun loadWorkouts() {
        firestore.collection("users").get().addOnSuccessListener { users ->
            val members = users.documents.filter { it.getString("role")?.lowercase() != "admin" }
            if (members.isEmpty()) { records = emptyList(); render(""); return@addOnSuccessListener }
            val map = members.associate { it.id to Pair(it.getString("name").orEmpty().ifBlank { "Member" }, it.getString("email").orEmpty()) }
            firestore.collectionGroup("workouts").get().addOnSuccessListener { snapshot ->
                records = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.reference.parent.parent?.id ?: doc.getString("uid") ?: return@mapNotNull null
                    val member = map[uid] ?: return@mapNotNull null
                    WorkoutRow(member.first, member.second, uid, doc.getString("title").orEmpty().ifBlank { "Workout" },
                        doc.getLong("exerciseCount") ?: 0L, doc.getLong("durationMinutes") ?: 0L,
                        doc.getLong("timestamp") ?: 0L)
                }.sortedByDescending { it.timestamp }
                render("")
            }.addOnFailureListener { Toast.makeText(this, "Could not load workouts", Toast.LENGTH_LONG).show() }
        }.addOnFailureListener { Toast.makeText(this, "Could not load members", Toast.LENGTH_LONG).show() }
    }

    private fun render(query: String) {
        container.removeAllViews()
        val q = query.trim().lowercase()
        val filtered = records.filter { it.name.lowercase().contains(q) || it.email.lowercase().contains(q) ||
            it.title.lowercase().contains(q) || it.uid.lowercase().contains(q) }
        summary.text = "Completed workouts: " + records.size
        if (filtered.isEmpty()) {
            container.addView(TextView(this).apply {
                text = if (records.isEmpty()) "No completed workouts found." else "No matching workouts."
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 14f; gravity = Gravity.CENTER; setPadding(8, 24, 8, 24)
            })
            return
        }
        val format = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
        filtered.forEach { record ->
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                setCardBackgroundColor(resources.getColor(R.color.fitness_surface, theme))
                radius = 20f
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }
            }
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18,16,18,16) }
            fun addText(value: String, size: Float, color: Int, bold: Boolean = false) {
                box.addView(TextView(this).apply {
                    text = value; textSize = size; setTextColor(resources.getColor(color, theme))
                    if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 3, 0, 3)
                })
            }
            addText(record.name, 17f, R.color.fitness_text, true)
            addText(record.title, 15f, R.color.fitness_mint, true)
            addText(record.exerciseCount.toString() + " exercises • " + record.duration + " min", 13f, R.color.fitness_text_muted)
            addText(format.format(Date(record.timestamp)), 12f, R.color.fitness_text_muted)
            addText(if (record.email.isBlank()) "Email not available" else record.email, 12f, R.color.fitness_text_muted)
            card.addView(box); container.addView(card)
        }
    }
}