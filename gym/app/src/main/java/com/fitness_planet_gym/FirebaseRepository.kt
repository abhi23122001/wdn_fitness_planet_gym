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

    fun loadProfile(onComplete: (MemberProfile?, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(null, "User is not authenticated")
            return
        }

        firestore.collection("members").document(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onComplete(
                        MemberProfile(
                            uid = user.uid,
                            name = user.displayName.orEmpty()
                        ),
                        null
                    )
                    return@addOnSuccessListener
                }

                onComplete(
                    MemberProfile(
                        uid = user.uid,
                        name = snapshot.getString("name").orEmpty(),
                        weightKg = snapshot.getString("weightKg").orEmpty(),
                        heightCm = snapshot.getString("heightCm").orEmpty(),
                        goal = snapshot.getString("goal").orEmpty()
                    ),
                    null
                )
            }
            .addOnFailureListener { onComplete(null, it.message) }
    }
}