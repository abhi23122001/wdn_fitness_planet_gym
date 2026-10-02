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
    private lateinit var markButton: Button
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        selfiePreview = findViewById(R.id.selfiePreview)
        statusText = findViewById(R.id.attendanceStatus)
        dateTimeText = findViewById(R.id.attendanceDateTime)
        markButton = findViewById(R.id.markAttendanceButton)

        findViewById<Button>(R.id.captureSelfieButton).setOnClickListener { openCamera() }
        markButton.setOnClickListener { markAttendance() }
        loadTodayStatus()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

        markButton.isEnabled = false
        statusText.text = "SAVING ATTENDANCE…"

        val nowMillis = System.currentTimeMillis()
        firebaseRepository.markAttendance(nowMillis) { success, error ->
            runOnUiThread {
                if (success) {
                    val now = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(nowMillis))
                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putString("last_attendance", now)
                        .putString("attendance_date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis)))
                        .putBoolean("attendance_today", true)
                        .apply()
                    dateTimeText.text = now
                    statusText.text = "ATTENDANCE MARKED ✓"
                    statusText.setTextColor(getColor(R.color.fitness_mint))
                    Toast.makeText(this, "Attendance synced to Firebase.", Toast.LENGTH_SHORT).show()
                } else {
                    markButton.isEnabled = true
                    statusText.text = "SAVE FAILED"
                    Toast.makeText(this, error ?: "Unable to save attendance", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadTodayStatus() {
        firebaseRepository.isAttendanceMarkedToday { marked, error ->
            runOnUiThread {
                if (marked) {
                    val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
                    val last = prefs.getString("last_attendance", null)
                    statusText.text = "ATTENDANCE MARKED ✓"
                    if (last != null) dateTimeText.text = last
                    statusText.setTextColor(getColor(R.color.fitness_mint))
                    markButton.isEnabled = false
                } else {
                    markButton.isEnabled = true
                    if (!error.isNullOrBlank()) {
                        statusText.text = "READY"
                    } else {
                        statusText.text = "READY FOR TODAY"
                    }
                }
            }
        }
    }
}