package com.fitness_planet_gym

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import com.google.firebase.storage.FirebaseStorage

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun saveProfile(profile: MemberProfile, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(false, "User is not authenticated")
            return
        }
        val data = hashMapOf(
            "uid" to uid,
            "name" to profile.name,
            "weightKg" to profile.weightKg,
            "heightCm" to profile.heightCm,
            "goal" to profile.goal
        )
        firestore.collection("members").document(uid).set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun loadProfile(onComplete: (MemberProfile?, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onComplete(null, "User is not authenticated")
            return
        }
        firestore.collection("members").document(user.uid).get()
            .addOnSuccessListener { snapshot ->
                onComplete(
                    if (snapshot.exists()) MemberProfile(
                        uid = user.uid,
                        name = snapshot.getString("name").orEmpty(),
                        weightKg = snapshot.getString("weightKg").orEmpty(),
                        heightCm = snapshot.getString("heightCm").orEmpty(),
                        goal = snapshot.getString("goal").orEmpty()
                    ) else MemberProfile(uid = user.uid, name = user.displayName.orEmpty()),
                    null
                )
            }
            .addOnFailureListener { onComplete(null, it.message) }
    }

    fun uploadMemberAvatar(bitmap: Bitmap, onComplete: (String?, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(null, "User is not authenticated")
            return
        }
        val bytes = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, this)
        }.toByteArray()
        val ref = storage.reference.child("profile/$uid/avatar.jpg")
        ref.putBytes(bytes)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw (task.exception ?: Exception("Avatar upload failed"))
                ref.downloadUrl
            }
            .addOnSuccessListener { url -> onComplete(url.toString(), null) }
            .addOnFailureListener { onComplete(null, it.message) }
    }

    fun loadMemberAvatar(onComplete: (Bitmap?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(null)
            return
        }
        storage.reference.child("profile/$uid/avatar.jpg").getBytes(5L * 1024L * 1024L)
            .addOnSuccessListener { bytes ->
                onComplete(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
            .addOnFailureListener { onComplete(null) }
    }

    fun isAttendanceMarkedToday(onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(false, "User is not authenticated")
            return
        }
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        firestore.collection("members").document(uid)
            .collection("attendance").document(today).get()
            .addOnSuccessListener { onComplete(it.exists(), null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun saveWeightEntry(weightKg: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(false, "User is not authenticated")
            return
        }
        val now = System.currentTimeMillis()
        val data = hashMapOf("weightKg" to weightKg, "timestamp" to now)
        firestore.collection("members").document(uid).collection("weightHistory").add(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun loadWeightHistory(onComplete: (List<Pair<String, Long>>, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(emptyList(), "User is not authenticated")
            return
        }
        firestore.collection("members").document(uid).collection("weightHistory")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10).get()
            .addOnSuccessListener { snapshot ->
                val history = snapshot.documents.mapNotNull { doc ->
                    val weight = doc.getString("weightKg") ?: return@mapNotNull null
                    val time = doc.getLong("timestamp") ?: 0L
                    weight to time
                }
                onComplete(history, null)
            }
            .addOnFailureListener { onComplete(emptyList(), it.message) }
    }

    fun loadProgressData(onComplete: (List<Pair<String, Long>>, List<Pair<String, Long>>, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(emptyList(), emptyList(), "User is not authenticated")
            return
        }
        val ref = firestore.collection("members").document(uid)
        ref.collection("weightHistory")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limit(50).get()
            .addOnSuccessListener { weights ->
                val weightList = weights.documents.mapNotNull { doc ->
                    val value = doc.getString("weightKg") ?: return@mapNotNull null
                    value to (doc.getLong("timestamp") ?: 0L)
                }
                ref.collection("workouts")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20).get()
                    .addOnSuccessListener { workouts ->
                        val workoutList = workouts.documents.mapNotNull { doc ->
                            val title = doc.getString("title") ?: return@mapNotNull null
                            title to (doc.getLong("timestamp") ?: 0L)
                        }
                        onComplete(weightList, workoutList, null)
                    }
                    .addOnFailureListener { onComplete(weightList, emptyList(), it.message) }
            }
            .addOnFailureListener { onComplete(emptyList(), emptyList(), it.message) }
    }

    fun loadDashboardStats(onComplete: (Int, Int, Int, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(0, 0, 0, "User is not authenticated")
            return
        }

        val memberRef = firestore.collection("members").document(uid)
        memberRef.collection("workouts").get()
            .addOnSuccessListener { workouts ->
                memberRef.collection("attendance").get()
                    .addOnSuccessListener { attendance ->
                        onComplete(workouts.size(), attendance.size(), 0, null)
                    }
                    .addOnFailureListener { onComplete(workouts.size(), 0, 0, it.message) }
            }
            .addOnFailureListener { onComplete(0, 0, 0, it.message) }
    }

    fun completeWorkout(title: String, exerciseCount: Int, durationMinutes: Int, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(false, "User is not authenticated")
            return
        }
        val timestamp = System.currentTimeMillis()
        val data = hashMapOf(
            "uid" to uid,
            "title" to title,
            "exerciseCount" to exerciseCount,
            "durationMinutes" to durationMinutes,
            "timestamp" to timestamp
        )
        firestore.collection("members").document(uid)
            .collection("workouts").add(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun uploadAttendanceSelfie(bitmap: Bitmap, timestamp: Long, onComplete: (String?, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(null, "User is not authenticated")
            return
        }
        val bytes = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, this)
        }.toByteArray()
        val fileName = timestamp.toString() + ".jpg"
        val ref = storage.reference.child("attendance/selfies/$uid/$fileName")
        ref.putBytes(bytes)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw (task.exception ?: Exception("Selfie upload failed"))
                ref.downloadUrl
            }
            .addOnSuccessListener { onComplete(it.toString(), null) }
            .addOnFailureListener { onComplete(null, it.message) }
    }

    fun markAttendance(timestamp: Long, selfieUrl: String?, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete(false, "User is not authenticated")
            return
        }
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp))
        val displayTime = java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        val data = hashMapOf(
            "uid" to uid,
            "date" to today,
            "timestamp" to timestamp,
            "displayTime" to displayTime,
            "type" to "gym_attendance",
            "selfieUrl" to (selfieUrl ?: "")
        )
        firestore.collection("members").document(uid)
            .collection("attendance").document(today).set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }
}