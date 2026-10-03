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

class AdminMembersActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var membersContainer: LinearLayout
    private var members = emptyList<MemberRow>()

    data class MemberRow(
        val uid: String,
        val name: String,
        val email: String,
        val role: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_members)

        membersContainer = findViewById(R.id.membersContainer)
        val search = findViewById<EditText>(R.id.memberSearch)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderMembers(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadMembers()
    }

    private fun loadMembers() {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                members = snapshot.documents.map { doc ->
                    MemberRow(
                        uid = doc.id,
                        name = doc.getString("name")?.ifBlank { "Member" } ?: "Member",
                        email = doc.getString("email").orEmpty(),
                        role = doc.getString("role")?.lowercase() ?: "member"
                    )
                }.filter { it.role != "admin" }
                    .sortedBy { it.name.lowercase() }

                renderMembers("")
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Could not load members. Check Firestore rules.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun renderMembers(query: String) {
        membersContainer.removeAllViews()
        val q = query.trim().lowercase()
        val filtered = members.filter {
            it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
        }

        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (members.isEmpty()) "No members found." else "No matching members."
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 14f
                setPadding(8, 20, 8, 20)
                gravity = Gravity.CENTER
            }
            membersContainer.addView(empty)
            return
        }

        filtered.forEach { item ->
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                setCardBackgroundColor(resources.getColor(R.color.fitness_surface, theme))
                radius = 20f
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                layoutParams = params
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 16, 18, 16)
            }

            val title = TextView(this).apply {
                text = item.name
                setTextColor(resources.getColor(R.color.fitness_text, theme))
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val email = TextView(this).apply {
                text = if (item.email.isBlank()) "Email not available" else item.email
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 13f
                setPadding(0, 5, 0, 0)
            }

            val uid = TextView(this).apply {
                text = "UID: " + item.uid
                setTextColor(resources.getColor(R.color.fitness_text_muted, theme))
                textSize = 10f
                setPadding(0, 8, 0, 0)
            }

            box.addView(title)
            box.addView(email)
            box.addView(uid)
            card.addView(box)
            membersContainer.addView(card)
        }
    }
}
