package com.fitness_planet_gym

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAnalyticsActivity : AppCompatActivity() {
 private val db=FirebaseFirestore.getInstance(); private lateinit var summary:TextView; private lateinit var history:TextView
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_attendance_analytics);summary=findViewById(R.id.analyticsSummary);history=findViewById(R.id.analyticsHistory);load()}
 private fun load(){val uid=FirebaseAuth.getInstance().currentUser?.uid?:return;db.collection("members").document(uid).collection("attendance").get().addOnSuccessListener{snap->val now=Calendar.getInstance();val month=now.get(Calendar.MONTH);val year=now.get(Calendar.YEAR);val count=snap.documents.count{val ts=it.getLong("timestamp")?:0L;val c=Calendar.getInstance().apply{timeInMillis=ts};c.get(Calendar.MONTH)==month&&c.get(Calendar.YEAR)==year};val days=now.getActualMaximum(Calendar.DAY_OF_MONTH);val percent=(count*100.0/days).coerceAtMost(100.0);summary.text="This month: "+count+" visits • "+String.format(Locale.US,"%.1f",percent)+"% of calendar days";val fmt=SimpleDateFormat("dd MMM yyyy • hh:mm a",Locale.getDefault());history.text=if(snap.isEmpty)"No attendance records yet." else snap.documents.mapNotNull{it.getLong("timestamp")}.sortedDescending().take(20).joinToString("\n"){fmt.format(Date(it))}}}
}