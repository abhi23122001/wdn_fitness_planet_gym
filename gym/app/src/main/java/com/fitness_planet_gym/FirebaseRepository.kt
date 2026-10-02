package com.fitness_planet_gym

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    fun markAttendance(timestamp: Long, onComplete: (Boolean, String?) -> Unit) {
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
            "type" to "gym_attendance"
        )
        firestore.collection("members").document(uid)
            .collection("attendance").document(today).set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }
}