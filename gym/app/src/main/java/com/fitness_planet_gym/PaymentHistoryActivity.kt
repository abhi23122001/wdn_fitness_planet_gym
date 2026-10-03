package com.fitness_planet_gym

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class PaymentHistoryActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var summary: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(R.layout.activity_payment_history); container=findViewById(R.id.paymentHistoryContainer); summary=findViewById(R.id.paymentHistorySummary); loadHistory() }
    private fun loadHistory() {
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("members").document(uid).get().addOnSuccessListener { member ->
            val fee=member.getDouble("fee") ?: 0.0
            db.collection("members").document(uid).collection("payments").get().addOnSuccessListener { snap ->
                val docs=snap.documents.sortedByDescending { it.getString("date").orEmpty() }; val paid=docs.sumOf { it.getDouble("amount") ?: 0.0 }; val due=(fee-paid).coerceAtLeast(0.0)
                summary.text="Fee: ₹${String.format(Locale.US,"%.2f",fee)} • Paid: ₹${String.format(Locale.US,"%.2f",paid)} • Due: ₹${String.format(Locale.US,"%.2f",due)}"; container.removeAllViews()
                if(docs.isEmpty()){ container.addView(TextView(this).apply{text="No payment records yet.";setTextColor(resources.getColor(R.color.fitness_text_muted,theme));gravity=Gravity.CENTER;setPadding(8,28,8,28)});return@addOnSuccessListener }
                docs.forEach { doc -> val amount=doc.getDouble("amount")?:0.0; val date=doc.getString("date").orEmpty(); val mode=doc.getString("mode").orEmpty(); val note=doc.getString("note").orEmpty(); val card=com.google.android.material.card.MaterialCardView(this).apply{setCardBackgroundColor(resources.getColor(R.color.fitness_surface,theme));radius=20f;layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12}}; val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,16,18,16)}; addText(box,"₹${String.format(Locale.US,"%.2f",amount)}",19f,R.color.fitness_mint,true); addText(box,"$date • ${mode.ifBlank{"Not specified"}}",13f,R.color.fitness_text,true); if(note.isNotBlank())addText(box,note,12f,R.color.fitness_text_muted); box.addView(Button(this).apply{text="VIEW RECEIPT";setOnClickListener{showReceipt(date,mode,amount,note,fee)}});card.addView(box);container.addView(card) }
            }
        }.addOnFailureListener{Toast.makeText(this,"Could not load payment history",Toast.LENGTH_LONG).show()}
    }
    private fun addText(box:LinearLayout,value:String,size:Float,color:Int,bold:Boolean=false){box.addView(TextView(this).apply{text=value;textSize=size;setTextColor(resources.getColor(color,theme));if(bold)setTypeface(typeface,android.graphics.Typeface.BOLD);setPadding(0,3,0,3)})}
    private fun showReceipt(date:String,mode:String,amount:Double,note:String,fee:Double){val dialog=android.app.Dialog(this);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,24,28,24);setBackgroundColor(resources.getColor(R.color.fitness_surface,theme))};addText(box,"FITNESS PLANET GYM",20f,R.color.fitness_mint,true);addText(box,"PAYMENT RECEIPT",16f,R.color.fitness_text,true);addText(box,"Amount: ₹${String.format(Locale.US,"%.2f",amount)}",15f,R.color.fitness_text);addText(box,"Date: $date",14f,R.color.fitness_text_muted);addText(box,"Mode: ${mode.ifBlank{"Not specified"}}",14f,R.color.fitness_text_muted);if(note.isNotBlank())addText(box,"Note: $note",13f,R.color.fitness_text_muted);addText(box,"Membership Fee: ₹${String.format(Locale.US,"%.2f",fee)}",13f,R.color.fitness_text_muted);box.addView(Button(this).apply{text="CLOSE";setOnClickListener{dialog.dismiss()}});dialog.setContentView(box);dialog.show()}
}