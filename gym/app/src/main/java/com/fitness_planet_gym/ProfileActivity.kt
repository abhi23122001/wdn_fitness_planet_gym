package com.fitness_planet_gym

import android.os.Bundle
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var name: TextInputEditText
    private lateinit var weight: TextInputEditText
    private lateinit var height: TextInputEditText
    private lateinit var goal: TextInputEditText
    private lateinit var status: TextView
    private lateinit var saveButton: Button
    private lateinit var profileImage: android.widget.ImageView
    private val firebaseRepository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("fitness_planet", MODE_PRIVATE)
        name = findViewById(R.id.profileName)
        weight = findViewById(R.id.profileWeight)
        height = findViewById(R.id.profileHeight)
        goal = findViewById(R.id.profileGoal)
        status = findViewById(R.id.saveStatus)
        saveButton = findViewById(R.id.saveProfileButton)
        profileImage = findViewById(R.id.profileImage)

        name.setText(prefs.getString("member_name", "Member"))
        weight.setText(prefs.getString("weight", ""))
        height.setText(prefs.getString("height", ""))
        goal.setText(prefs.getString("goal", ""))

        loadFromFirebase()
        firebaseRepository.loadMemberAvatar { bitmap ->
            if (bitmap != null) runOnUiThread { profileImage.setImageBitmap(bitmap) }
        }

        findViewById<Button>(R.id.changePhotoButton).setOnClickListener {
            startActivityForResult(android.content.Intent(android.content.Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 2001)
        }

        findViewById<Button>(R.id.addWeightButton).setOnClickListener { saveWeightEntry() }
        findViewById<Button>(R.id.accountInfoButton).setOnClickListener { showAccountInfo() }
        loadWeightHistory()

        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    @Deprecated("Use Activity Result API when this screen is modernized.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bitmap == null) {
                Toast.makeText(this, "Unable to read photo", Toast.LENGTH_SHORT).show()
                return
            }
            profileImage.setImageBitmap(bitmap)
            status.text = "UPLOADING PHOTO…"
            firebaseRepository.uploadMemberAvatar(bitmap) { url, error ->
                runOnUiThread {
                    if (url != null) status.text = "PROFILE PHOTO UPDATED ✓"
                    else Toast.makeText(this, error ?: "Upload failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAccountInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "Not available"
        val uid = user?.uid ?: "Not available"
        android.app.AlertDialog.Builder(this)
            .setTitle("ACCOUNT INFORMATION")
            .setMessage("Name: " + name.text?.toString().orEmpty() + "\nEmail: " + email + "\nAccount ID: " + uid)
            .setPositiveButton("OK", null)
            .show()
    }
    private fun loadFromFirebase() {
        status.text = "SYNCING PROFILE…"
        firebaseRepository.loadProfile { profile, error ->
            runOnUiThread {
                if (profile != null) {
                    name.setText(profile.name.ifBlank { name.text?.toString() })
                    weight.setText(profile.weightKg)
                    height.setText(profile.heightCm)
                    goal.setText(profile.goal)

                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putString("member_name", profile.name.ifBlank { "Member" })
                        .putString("weight", profile.weightKg)
                        .putString("height", profile.heightCm)
                        .putString("goal", profile.goal)
                        .apply()

                    status.text = "PROFILE SYNCED ✓"
                } else {
                    status.text = "OFFLINE PROFILE"
                    if (!error.isNullOrBlank()) {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveWeightEntry() {
        val value = weight.text?.toString()?.trim().orEmpty()
        if (value.toDoubleOrNull() == null || value.toDouble() <= 0) {
            weight.error = "Enter a valid weight"
            return
        }
        findViewById<Button>(R.id.addWeightButton).isEnabled = false
        status.text = "SAVING WEIGHT…"
        firebaseRepository.saveWeightEntry(value) { success, error ->
            runOnUiThread {
                findViewById<Button>(R.id.addWeightButton).isEnabled = true
                if (success) {
                    status.text = "WEIGHT ENTRY SAVED ✓"
                    loadWeightHistory()
                } else {
                    Toast.makeText(this, error ?: "Unable to save weight", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadWeightHistory() {
        firebaseRepository.loadWeightHistory { history, _ ->
            runOnUiThread {
                val historyText = findViewById<TextView>(R.id.weightHistory)
                if (history.isEmpty()) {
                    historyText.text = "No weight entries yet."
                } else {
                    val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    historyText.text = history.joinToString("\n") { entry ->
                        entry.first + " kg  •  " + format.format(java.util.Date(entry.second))
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val profile = MemberProfile(
            name = name.text?.toString()?.trim().orEmpty().ifBlank { "Member" },
            weightKg = weight.text?.toString()?.trim().orEmpty(),
            heightCm = height.text?.toString()?.trim().orEmpty(),
            goal = goal.text?.toString()?.trim().orEmpty()
        )

        saveButton.isEnabled = false
        status.text = "SAVING TO FIREBASE…"

        firebaseRepository.saveProfile(profile) { success, error ->
            runOnUiThread {
                saveButton.isEnabled = true
                if (success) {
                    getSharedPreferences("fitness_planet", MODE_PRIVATE).edit()
                        .putString("member_name", profile.name)
                        .putString("weight", profile.weightKg)
                        .putString("height", profile.heightCm)
                        .putString("goal", profile.goal)
                        .apply()
                    status.text = "PROFILE SAVED & SYNCED ✓"
                } else {
                    status.text = "SAVE FAILED"
                    Toast.makeText(this, error ?: "Unable to save profile", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}