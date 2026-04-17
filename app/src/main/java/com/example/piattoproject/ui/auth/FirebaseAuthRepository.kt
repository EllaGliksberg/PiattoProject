package com.example.piattoproject.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String, registerUsername: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        if (auth.currentUser == null) {
            auth.signInWithEmailAndPassword(email, password).await()
        }
        val uid = auth.currentUser?.uid ?: error("Registered user session is missing")
        val payload = hashMapOf(
            FIELD_FULL_NAME to registerUsername,
            FIELD_USERNAME to email,
            FIELD_BIO to "",
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
        )
        firestore.collection(USERS_COLLECTION).document(uid).set(payload).await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_USERNAME = "username"
        const val FIELD_BIO = "bio"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
