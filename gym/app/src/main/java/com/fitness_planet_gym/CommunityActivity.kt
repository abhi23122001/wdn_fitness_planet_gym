package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CommunityActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var groups: LinearLayout
    private var selectedGroup: String? = null
    private lateinit var messages: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_community)
        groups = findViewById(R.id.communityGroups)
        messages = findViewById(R.id.communityMessages)
        findViewById<Button>(R.id.sendMessageButton).setOnClickListener { sendMessage() }
        loadGroups()
    }

    private fun loadGroups() {
        db.collection("communityGroups").get().addOnSuccessListener { snap ->
            groups.removeAllViews()
            snap.documents.sortedBy { it.getString("name").orEmpty().lowercase() }.forEach { doc ->
                val name = doc.getString("name").orEmpty().ifBlank { "Gym Community" }
                val b = Button(this).apply {
                    text = name
                    setOnClickListener { selectGroup(doc.id, name) }
                }
                groups.addView(b)
            }
            if (snap.isEmpty) {
                val empty = TextView(this).apply { text = "No community groups yet."; setTextColor(resources.getColor(R.color.fitness_text_muted, theme)) }
                groups.addView(empty)
            }
        }
    }

    private fun selectGroup(id: String, name: String) {
        selectedGroup = id
        findViewById<TextView>(R.id.selectedGroup).text = "GROUP: $name"
        loadMessages(id)
    }

    private fun loadMessages(id: String) {
        db.collection("communityGroups").document(id).collection("messages")
            .orderBy("createdAt").limitToLast(50).get().addOnSuccessListener { snap ->
                messages.removeAllViews()
                snap.documents.forEach { doc ->
                    val text = TextView(this).apply {
                        text = (doc.getString("senderName") ?: "Member") + ": " + doc.getString("text").orEmpty()
                        setTextColor(resources.getColor(R.color.fitness_text, theme))
                        textSize = 14f
                        setPadding(12, 10, 12, 10)
                    }
                    messages.addView(text)
                }
            }
    }

    private fun sendMessage() {
        val gid = selectedGroup ?: run { Toast.makeText(this, "Select a group first", Toast.LENGTH_SHORT).show(); return }
        val input = findViewById<EditText>(R.id.messageInput)
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "senderName" to (prefs.getString("member_name", "Member") ?: "Member"),
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("communityGroups").document(gid).collection("messages").add(data)
            .addOnSuccessListener { input.text.clear(); loadMessages(gid) }
            .addOnFailureListener { Toast.makeText(this, "Message failed", Toast.LENGTH_SHORT).show() }
    }
}