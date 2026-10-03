package com.fitness_planet_gym

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminProgressActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var box: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_progress)
        box = findViewById(R.id.progressAdminContainer)
        load()
    }

    private fun load() {
        db.collection("users").get().addOnSuccessListener { users ->
            val members = users.documents.filter { it.getString("role")?.lowercase() != "admin" }
            var done = 0
            var weights = 0
            if (members.isEmpty()) { show("No members found"); return@addOnSuccessListener }
            members.forEach { user ->
                db.collection("members").document(user.id).collection("weightHistory").get()
                    .addOnSuccessListener {
                        weights += it.size()
                        done++
                        if (done == members.size) show("Members: " + members.size + "\nWeight entries: " + weights)
                    }
                    .addOnFailureListener {
                        done++
                        if (done == members.size) show("Members: " + members.size + "\nWeight entries: " + weights)
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Could not load progress", Toast.LENGTH_LONG).show()
        }
    }

    private fun show(text: String) {
        box.removeAllViews()
        val t = TextView(this)
        t.text = text
        t.textSize = 18f
        t.setTextColor(resources.getColor(R.color.fitness_text, theme))
        t.setPadding(18, 18, 18, 18)
        box.addView(t)
    }
}