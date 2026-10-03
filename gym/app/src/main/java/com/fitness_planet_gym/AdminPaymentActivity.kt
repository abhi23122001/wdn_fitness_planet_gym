package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminPaymentActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private var selectedUid: String? = null
    private var selectedMembershipFee = 0.0

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_payment)
        container = findViewById(R.id.paymentMemberContainer)
        findViewById<Button>(R.id.savePaymentButton).setOnClickListener { savePayment() }
        findViewById<Button>(R.id.clearPaymentButton).setOnClickListener { clearForm() }
        loadMembers()
    }

    private fun loadMembers() {
        db.collection("users").get().addOnSuccessListener { snap ->
            container.removeAllViews()
            snap.documents.filter { it.getString("role")?.lowercase() != "admin" }
                .sortedBy { it.getString("name").orEmpty().lowercase() }
                .forEach { user ->
                    val uid = user.id
                    val card = TextView(this).apply {
                        text = (user.getString("name").orEmpty().ifBlank { "Member" }) + " • " + user.getString("email").orEmpty()
                        textSize = 15f
                        setTextColor(resources.getColor(R.color.fitness_text, theme))
                        setPadding(18, 18, 18, 18)
                        setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
                        setOnClickListener { selectMember(uid) }
                    }
                    container.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
                }
        }
    }

    private fun selectMember(uid: String) {
        selectedUid = uid
        findViewById<TextView>(R.id.selectedPaymentMember).text = "Selected: " + uid
        db.collection("members").document(uid).get().addOnSuccessListener { doc ->
            selectedMembershipFee = doc.getDouble("fee") ?: 0.0
            loadSummary(uid)
        }.addOnFailureListener { loadSummary(uid) }
    }

    private fun loadSummary(uid: String) {
        db.collection("members").document(uid).collection("payments").get()
            .addOnSuccessListener { snap ->
                var paid = 0.0
                snap.documents.forEach { paid += it.getDouble("amount") ?: 0.0 }
                val due = (selectedMembershipFee - paid).coerceAtLeast(0.0)
                val status = when { selectedMembershipFee <= 0.0 -> "MEMBERSHIP FEE NOT SET"; paid >= selectedMembershipFee -> "PAID"; paid > 0.0 -> "PARTIAL"; else -> "DUE" }
                findViewById<TextView>(R.id.paymentSummary).text = "Fee: ₹" + String.format(Locale.US, "%.2f", selectedMembershipFee) + " • Paid: ₹" + String.format(Locale.US, "%.2f", paid) + " • Due: ₹" + String.format(Locale.US, "%.2f", due) + " • " + status
            }
    }

    private fun savePayment() {
        val uid = selectedUid
        if (uid.isNullOrBlank()) { Toast.makeText(this, "Select a member first", Toast.LENGTH_SHORT).show(); return }
        val amount = findViewById<EditText>(R.id.paymentAmount).text.toString().trim().toDoubleOrNull()
        val date = findViewById<EditText>(R.id.paymentDate).text.toString().trim()
        val mode = findViewById<EditText>(R.id.paymentMode).text.toString().trim()
        val note = findViewById<EditText>(R.id.paymentNote).text.toString().trim()
        if (amount == null || amount <= 0 || date.isBlank() || mode.isBlank()) { Toast.makeText(this, "Enter amount, date and payment mode", Toast.LENGTH_SHORT).show(); return }
        try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: throw IllegalArgumentException() } catch (_: Exception) { Toast.makeText(this, "Use date format YYYY-MM-DD", Toast.LENGTH_SHORT).show(); return }
        val data = hashMapOf<String, Any>("amount" to amount, "date" to date, "mode" to mode, "note" to note, "createdAt" to System.currentTimeMillis(), "status" to "paid")
        db.collection("members").document(uid).collection("payments").add(data)
            .addOnSuccessListener { Toast.makeText(this, "Payment saved", Toast.LENGTH_SHORT).show(); loadSummary(uid); clearForm(false) }
            .addOnFailureListener { Toast.makeText(this, "Payment failed: " + it.message, Toast.LENGTH_LONG).show() }
    }

    private fun clearForm(resetMember: Boolean = true) {
        if (resetMember) { selectedUid = null; findViewById<TextView>(R.id.selectedPaymentMember).text = "No member selected"; selectedMembershipFee = 0.0
            findViewById<TextView>(R.id.paymentSummary).text = "Fee: ₹0.00 • Paid: ₹0.00 • Due: ₹0.00 • NO MEMBER" }
        findViewById<EditText>(R.id.paymentAmount).text.clear()
        findViewById<EditText>(R.id.paymentDate).text.clear()
        findViewById<EditText>(R.id.paymentMode).text.clear()
        findViewById<EditText>(R.id.paymentNote).text.clear()
    }
}