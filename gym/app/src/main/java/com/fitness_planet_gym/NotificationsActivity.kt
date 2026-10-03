package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_notifications)
        val container = findViewById<LinearLayout>(R.id.notificationContainer)
        FirebaseFirestore.getInstance().collection("notifications").orderBy("createdAt").limitToLast(50).get()
            .addOnSuccessListener { snap ->
                container.removeAllViews()
                snap.documents.reversed().forEach { doc ->
                    val title = doc.getString("title").orEmpty()
                    val body = doc.getString("body").orEmpty()
                    val item = TextView(this).apply {
                        text = title + "\n" + body
                        setTextColor(resources.getColor(R.color.fitness_text, theme))
                        textSize = 15f
                        setPadding(16, 16, 16, 16)
                    }
                    container.addView(item)
                }
            }
    }
}