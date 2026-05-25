package com.example.piattoproject.ui.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun loadProfile(): FirebaseProfile {
        val uid = ensureSignedInUserId()
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        if (!snapshot.exists()) {
            val defaultProfile = defaultProfile(auth.currentUser?.email)
            saveProfile(defaultProfile.fullName, defaultProfile.username, defaultProfile.bio)
            return defaultProfile
        }
        return FirebaseProfile(
            fullName = snapshot.getString(FIELD_FULL_NAME).orEmpty(),
            username = snapshot.getString(FIELD_USERNAME).orEmpty(),
            bio = snapshot.getString(FIELD_BIO).orEmpty(),
            imageUrl = snapshot.getString(FIELD_IMAGE_URL),
        )
    }

    suspend fun saveProfile(
        fullName: String,
        username: String,
        bio: String,
    ): FirebaseProfile {
        val uid = ensureSignedInUserId()
        val payload = hashMapOf(
            FIELD_FULL_NAME to fullName,
            FIELD_USERNAME to username,
            FIELD_BIO to bio,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
        )
        firestore.collection(USERS_COLLECTION).document(uid).set(payload, SetOptions.merge()).await()
        return FirebaseProfile(
            fullName = fullName,
            username = username,
            bio = bio,
            imageUrl = loadProfile().imageUrl,
        )
    }

    suspend fun saveProfileImage(imageUrl: String?): FirebaseProfile {
        val uid = ensureSignedInUserId()
        val payload = hashMapOf<String, Any>(
            FIELD_IMAGE_URL to imageUrl.orEmpty(),
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
        )
        firestore.collection(USERS_COLLECTION).document(uid).update(payload).await()
        return loadProfile()
    }

    private suspend fun ensureSignedInUserId(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        error("User must be authenticated before accessing profile data")
    }

    private fun defaultProfile(email: String?): FirebaseProfile {
        val username = email?.trim().orEmpty().ifBlank { "@new_user" }
        return FirebaseProfile(
            fullName = "New User",
            username = username,
            bio = "",
            imageUrl = null,
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_USERNAME = "username"
        const val FIELD_BIO = "bio"
        const val FIELD_IMAGE_URL = "imageUrl"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
