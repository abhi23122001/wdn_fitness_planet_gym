package com.fitness_planet_gym

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.view.ViewGroup
import com.google.firebase.storage.FirebaseStorage
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminAttendanceActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var summary: TextView
    private var records = emptyList<AttendanceRow>()

    data class AttendanceRow(
        val name: String,
        val email: String,
        val uid: String,
        val timestamp: Long,
        val selfieUrl: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_attendance)

        container = findViewById(R.id.attendanceContainer)
        summary = findViewById(R.id.attendanceSummary)
        val search = findViewById<EditText>(R.id.attendanceSearch)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadAttendance()
    }

    private fun loadAttendance() {
        firestore.collection("users").get()
            .addOnSuccessListener { users ->
                val members = users.documents
                    .filter { it.getString("role")?.lowercase() != "admin" }
                    .map {
                        Triple(
                            it.id,
                            it.getString("name").orEmpty().ifBlank { "Member" },
                            it.getString("email").orEmpty()
                        )
                    }

                if (members.isEmpty()) {
                    records = emptyList()
                    render("")
                    return@addOnSuccessListener
                }

                val loaded = mutableListOf<AttendanceRow>()
                var completed = 0

                members.forEach { member ->
                    firestore.collection("members").document(member.first)
                        .collection("attendance").get()
                        .addOnSuccessListener { snapshot ->
                            snapshot.documents.forEach { doc ->
                                val timestamp = doc.getLong("timestamp") ?: return@forEach
                                val selfieUrl = doc.getString("selfieUrl").orEmpty()
                                loaded.add(
                                    AttendanceRow(member.second, member.third, member.first, timestamp, selfieUrl)
                                )
                            }
                            completed++
                            if (completed == members.size) {
                                records = loaded.sortedByDescending { it.timestamp }
                                render("")
                            }
                        }
                        .addOnFailureListener {
                            completed++
                            if (completed == members.size) {
                                records = loaded.sortedByDescending { it.timestamp }
                                render("")
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load attendance", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSelfie(url: String) {
        val dialog = android.app.Dialog(this)
        val image = ImageView(this).apply {
            adjustViewBounds = true
            setPadding(16, 16, 16, 16)
        }
        dialog.setContentView(image)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        FirebaseStorage.getInstance().getReferenceFromUrl(url).getBytes(5L * 1024L * 1024L)
            .addOnSuccessListener { bytes -> image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) }
            .addOnFailureListener { Toast.makeText(this, "Unable to load selfie", Toast.LENGTH_SHORT).show() }
    }

    private fun render(query: String) {
        container.removeAllViews()
        val q = query.trim().lowercase()
        val filtered = records.filter {
            it.name.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.uid.lowercase().contains(q)
        }

        summary.text = "Total attendance records: " + records.size

        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (records.isEmpty()) "No attendance records found." else "No matching records."
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(8, 24, 8, 24)
            }
            container.addView(empty)
            return
        }

        val format = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())

        filtered.forEach { record ->
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                setCardBackgroundColor(resources.getColor(R.color.fitness_surface, theme))
                radius = 20f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 16, 18, 16)
            }

            val name = TextView(this).apply {
                text = record.name
                setTextColor(resources.getColor(R.color.fitness_text, theme))
                textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val time = TextView(this).apply {
                text = format.format(Date(record.timestamp))
                setTextColor(resources.getColor(R.color.fitness_mint, theme))
                textSize = 13f
                setPadding(0, 5, 0, 0)
            }

            val selfieButton = Button(this).apply {
                text = if (record.selfieUrl.isBlank()) "NO SELFIE" else "VIEW SELFIE"
                isEnabled = record.selfieUrl.isNotBlank()
                setOnClickListener { if (record.selfieUrl.isNotBlank()) showSelfie(record.selfieUrl) }
                setTextColor(resources.getColor(R.color.fitness_dark, theme))
                backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.fitness_mint, theme))
            }

            val email = TextView(this).apply {
                text = if (record.email.isBlank()) "Email not available" else record.email
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }

            box.addView(name)
            box.addView(time)
            box.addView(email)
            box.addView(selfieButton)
            card.addView(box)
            container.addView(card)
        }
    }
}
