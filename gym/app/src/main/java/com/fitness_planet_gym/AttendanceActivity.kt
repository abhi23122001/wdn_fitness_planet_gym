package com.fitness_planet_gym

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_REQUEST = 501
        private const val CAMERA_PERMISSION = 502
    }

    private lateinit var selfiePreview: ImageView
    private lateinit var statusText: TextView
    private lateinit var dateTimeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        selfiePreview = findViewById(R.id.selfiePreview)
        statusText = findViewById(R.id.attendanceStatus)
        dateTimeText = findViewById(R.id.attendanceDateTime)

        findViewById<Button>(R.id.captureSelfieButton).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.markAttendanceButton).setOnClickListener { markAttendance() }
        loadTodayStatus()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
            return
        }
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST)
    }

    @Deprecated("Kept for broad Android compatibility.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap ?: return
            selfiePreview.setImageBitmap(bitmap)
            selfiePreview.tag = "captured"
            statusText.text = "Selfie captured. Tap Mark Attendance."
            statusText.setTextColor(getColor(R.color.fitness_mint))
        }
    }

    private fun markAttendance() {
        if (selfiePreview.tag != "captured") {
            Toast.makeText(this, "Pehle selfie capture karo.", Toast.LENGTH_SHORT).show()
            return
        }

        val now = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date())
        getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
            .putString("last_attendance", now)
            .putBoolean("attendance_today", true)
            .apply()

        dateTimeText.text = now
        statusText.text = "ATTENDANCE MARKED ✓"
        statusText.setTextColor(getColor(R.color.fitness_mint))
        findViewById<Button>(R.id.markAttendanceButton).isEnabled = false
        Toast.makeText(this, "Attendance marked successfully.", Toast.LENGTH_SHORT).show()
    }

    private fun loadTodayStatus() {
        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        val marked = prefs.getBoolean("attendance_today", false)
        val last = prefs.getString("last_attendance", null)
        if (marked && last != null) {
            statusText.text = "ATTENDANCE MARKED ✓"
            dateTimeText.text = last
            statusText.setTextColor(getColor(R.color.fitness_mint))
            findViewById<Button>(R.id.markAttendanceButton).isEnabled = false
        }
    }
}