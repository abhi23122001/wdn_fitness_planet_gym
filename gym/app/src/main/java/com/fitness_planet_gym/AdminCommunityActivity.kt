package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminCommunityActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_admin_community)
        findViewById<Button>(R.id.createGroupButton).setOnClickListener { createGroup() }
        findViewById<Button>(R.id.sendNotificationButton).setOnClickListener { sendNotification() }
    }
    private fun createGroup() {
        val name=findViewById<EditText>(R.id.groupName).text.toString().trim()
        if(name.isBlank()){Toast.makeText(this,"Enter group name",Toast.LENGTH_SHORT).show();return}
        db.collection("communityGroups").add(hashMapOf("name" to name,"createdAt" to System.currentTimeMillis()))
            .addOnSuccessListener{Toast.makeText(this,"Community group created",Toast.LENGTH_SHORT).show();findViewById<EditText>(R.id.groupName).text.clear()}
    }
    private fun sendNotification() {
        val title=findViewById<EditText>(R.id.notificationTitle).text.toString().trim()
        val body=findViewById<EditText>(R.id.notificationBody).text.toString().trim()
        if(title.isBlank()||body.isBlank()){Toast.makeText(this,"Enter title and message",Toast.LENGTH_SHORT).show();return}
        db.collection("notifications").add(hashMapOf("title" to title,"body" to body,"createdAt" to System.currentTimeMillis()))
            .addOnSuccessListener{Toast.makeText(this,"Notification published",Toast.LENGTH_SHORT).show();findViewById<EditText>(R.id.notificationTitle).text.clear();findViewById<EditText>(R.id.notificationBody).text.clear()}
    }
}