package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AdminMembershipActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private var selectedUid: String? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_membership)
        container = findViewById(R.id.membershipContainer)
        findViewById<Button>(R.id.saveMembershipButton).setOnClickListener { saveMembership() }
        findViewById<Button>(R.id.clearMembershipButton).setOnClickListener { clearForm() }
        loadMembers()
    }

    private fun saveMembership() {
        val uid = selectedUid
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "Select a member first", Toast.LENGTH_SHORT).show()
            return
        }
        val plan = findViewById<EditText>(R.id.membershipPlan).text.toString().trim()
        val feeText = findViewById<EditText>(R.id.membershipFee).text.toString().trim()
        val startText = findViewById<EditText>(R.id.membershipStart).text.toString().trim()
        val endText = findViewById<EditText>(R.id.membershipEnd).text.toString().trim()
        if (plan.isBlank() || feeText.isBlank() || startText.isBlank() || endText.isBlank()) {
            Toast.makeText(this, "Fill all membership fields", Toast.LENGTH_SHORT).show()
            return
        }
        val fee = feeText.toDoubleOrNull()
        if (fee == null || fee < 0) {
            Toast.makeText(this, "Enter a valid fee", Toast.LENGTH_SHORT).show()
            return
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val start = fmt.parse(startText) ?: throw IllegalArgumentException()
            val end = fmt.parse(endText) ?: throw IllegalArgumentException()
            if (end.before(start)) {
                Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Use date format YYYY-MM-DD", Toast.LENGTH_SHORT).show()
            return
        }
        val data = hashMapOf<String, Any>(
            "plan" to plan,
            "fee" to fee,
            "startDate" to startText,
            "endDate" to endText,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("members").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Membership saved", Toast.LENGTH_SHORT).show()
                clearForm()
                loadMembers()
            }
            .addOnFailureListener { Toast.makeText(this, "Save failed: " + it.message, Toast.LENGTH_LONG).show() }
    }

    private fun loadMembers() {
        db.collection("users").get().addOnSuccessListener { snap ->
            container.removeAllViews()
            snap.documents.filter { it.getString("role")?.lowercase() != "admin" }
                .sortedBy { it.getString("name").orEmpty().lowercase() }
                .forEach { user ->
                    val uid = user.id
                    val card = TextView(this).apply {
                        text = user.getString("name").orEmpty().ifBlank { "Member" } + " • " + user.getString("email").orEmpty()
                        textSize = 15f
                        setTextColor(resources.getColor(R.color.fitness_text, theme))
                        setPadding(18, 18, 18, 18)
                        setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
                        setOnClickListener { selectMember(uid) }
                    }
                    container.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Could not load members", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectMember(uid: String) {
        selectedUid = uid
        db.collection("members").document(uid).get().addOnSuccessListener { doc ->
            findViewById<EditText>(R.id.membershipPlan).setText(doc.getString("plan").orEmpty())
            findViewById<EditText>(R.id.membershipFee).setText(doc.getDouble("fee")?.toString().orEmpty())
            findViewById<EditText>(R.id.membershipStart).setText(doc.getString("startDate").orEmpty())
            findViewById<EditText>(R.id.membershipEnd).setText(doc.getString("endDate").orEmpty())
            findViewById<TextView>(R.id.selectedMember).text = "Selected: " + uid
        }
    }

    private fun clearForm() {
        selectedUid = null
        findViewById<TextView>(R.id.selectedMember).text = "No member selected"
        findViewById<EditText>(R.id.membershipPlan).text.clear()
        findViewById<EditText>(R.id.membershipFee).text.clear()
        findViewById<EditText>(R.id.membershipStart).text.clear()
        findViewById<EditText>(R.id.membershipEnd).text.clear()
    }
}