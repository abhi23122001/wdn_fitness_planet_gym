package com.fitness_planet_gym

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun saveProfile(profile: MemberProfile, onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
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

        firestore.collection("members").document(uid)
            .set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }
}