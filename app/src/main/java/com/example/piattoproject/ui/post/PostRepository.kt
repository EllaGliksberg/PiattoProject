package com.example.piattoproject.ui.post

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val postDao = AppLocalDbRepository.getInstance(context.applicationContext).postDao()

    suspend fun loadPost(postId: String): Result<Post> = runCatching {
        firestore.collection(POSTS_COLLECTION).document(postId).get().await().toPost()
    }

    suspend fun createPost(
        title: String,
        description: String,
        imageUrl: String?,
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("You must be signed in to post")
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "User"

        val id = UUID.randomUUID().toString()
        val updatedAt = System.currentTimeMillis()
        val newPost = Post(
            id = id,
            recipeTitle = title,
            description = description,
            imageUrl = imageUrl.orEmpty(),
            creatorName = displayName,
            creatorUid = user.uid,
            latitude = latitude,
            longitude = longitude,
            lastUpdated = updatedAt,
        )

        firestore.collection(POSTS_COLLECTION).document(id).set(newPost.toFirestorePayload()).await()
        withContext(Dispatchers.IO) {
            postDao.insert(newPost)
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        imageUrl: String?,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("You must be signed in to post")
        val snapshot = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
        if (!snapshot.exists()) {
            throw IllegalStateException("Post not found")
        }

        val existingPost = snapshot.toPost()
        if (existingPost.creatorUid != user.uid) {
            throw IllegalStateException("Cannot update this post")
        }

        val updatedPost = existingPost.copy(
            recipeTitle = title,
            description = description,
            imageUrl = imageUrl.orEmpty(),
            latitude = latitude ?: existingPost.latitude,
            longitude = longitude ?: existingPost.longitude,
            lastUpdated = System.currentTimeMillis(),
        )

        firestore.collection(POSTS_COLLECTION).document(postId).set(updatedPost.toFirestorePayload()).await()
        withContext(Dispatchers.IO) {
            postDao.insert(updatedPost)
        }
    }

    private fun Post.toFirestorePayload(): Map<String, Any> {
        val payload = hashMapOf<String, Any>(
            "recipeTitle" to recipeTitle,
            "description" to description,
            "imageUrl" to imageUrl,
            "creatorName" to creatorName,
            "creatorUid" to creatorUid,
            "lastUpdated" to lastUpdated,
        )
        if (latitude != null && longitude != null) {
            payload["latitude"] = latitude
            payload["longitude"] = longitude
        }
        return payload
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
    }
}
