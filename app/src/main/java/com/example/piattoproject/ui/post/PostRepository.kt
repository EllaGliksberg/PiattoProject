package com.example.piattoproject.ui.post

import android.content.Context
import androidx.lifecycle.LiveData
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.piattoproject.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val postDao = AppLocalDbRepository.getInstance(context.applicationContext).postDao()

    val allPosts: LiveData<List<Post>> = postDao.getAll()
    private val syncPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun uploadImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            ImageUtils.encodeImageUriToBase64(context, imageUri)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "BASE64 ERROR: ${e.message}", e)
            throw Exception("Failed to process image: ${e.message}")
        }
    }

    suspend fun syncPostsDelta(): Result<Unit> = runCatching {
        val lastSync = getLastSync()
        val snapshot = firestore.collection(POSTS_COLLECTION)
            .whereGreaterThan(FIELD_LAST_UPDATED, lastSync)
            .orderBy(FIELD_LAST_UPDATED, Query.Direction.ASCENDING)
            .get()
            .await()

        val changedPosts = snapshot.documents.mapNotNull { doc -> doc.toPost() }
        Log.d("DeltaSync", "lastSync = $lastSync")
        Log.d("DeltaSync", "changed posts = ${changedPosts.size}")

        withContext(Dispatchers.IO) {
            if (changedPosts.isNotEmpty()) {
                postDao.insert(*changedPosts.toTypedArray())
            }
        }

        val newLastSync = changedPosts.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
        saveLastSync(newLastSync)
    }

    suspend fun loadMapPosts(limit: Int = MAP_POSTS_LIMIT): Result<List<Post>> = runCatching {
        val snapshot = firestore.collection(POSTS_COLLECTION)
            .orderBy(FIELD_LAST_UPDATED, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val postsWithLocation = snapshot.documents
            .mapNotNull { it.toPost() }
            .filter { it.latitude != null && it.longitude != null }

        withContext(Dispatchers.IO) {
            if (postsWithLocation.isNotEmpty()) {
                postDao.insert(*postsWithLocation.toTypedArray())
            }
        }

        postsWithLocation
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
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit> = runCatching {
        val updatedAt = System.currentTimeMillis()
        val payload = mutableMapOf<String, Any>(
            "recipeTitle" to title,
            "description" to description,
            "lastUpdated" to updatedAt
        )
        imageUrl?.let { payload["imageUrl"] = it }
        if (latitude != null && longitude != null) {
            payload["latitude"] = latitude
            payload["longitude"] = longitude
        }

        firestore.collection(POSTS_COLLECTION).document(postId).update(payload).await()
        
        withContext(Dispatchers.IO) {
            val existingPost = postDao.getPostById(postId) ?: return@withContext
            val updatedPost = existingPost.copy(
                recipeTitle = title,
                description = description,
                imageUrl = imageUrl ?: existingPost.imageUrl,
                latitude = latitude ?: existingPost.latitude,
                longitude = longitude ?: existingPost.longitude,
                lastUpdated = updatedAt
            )
            postDao.insert(updatedPost)
        }
    }

    suspend fun toggleSave(postId: String): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not logged in"))
        val postRef = firestore.collection(POSTS_COLLECTION).document(postId)
        val userSavedPostRef = firestore.collection("users").document(userId)
            .collection("savedPosts").document(postId)

        return try {
            val isNowSaved = firestore.runTransaction { transaction ->
                val savedSnapshot = transaction.get(userSavedPostRef)
                val postSnapshot = transaction.get(postRef)
                val currentSaves = postSnapshot.getLong("savesCount") ?: 0

                if (savedSnapshot.exists()) {
                    transaction.delete(userSavedPostRef)
                    transaction.update(postRef, "savesCount", (currentSaves - 1).coerceAtLeast(0))
                    false
                } else {
                    transaction.set(userSavedPostRef, mapOf("timestamp" to System.currentTimeMillis()))
                    transaction.update(postRef, "savesCount", currentSaves + 1)
                    true
                }
            }.await()

            val updatedSnapshot = postRef.get().await()
            val updatedPost = updatedSnapshot.toPost()
            if (updatedPost != null) {
                withContext(Dispatchers.IO) {
                    postDao.insert(updatedPost)
                }
            }
            Result.success(isNowSaved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPostSaved(postId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection("users").document(userId)
                .collection("savedPosts").document(postId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        val localPost = postDao.getPostById(postId)
        if (localPost != null) {
            return@withContext localPost
        }

        try {
            val remotePost = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .get()
                .await()
                .takeIf { it.exists() }
                ?.toPost()
            if (remotePost != null) {
                postDao.insert(remotePost)
            }
            remotePost
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteCachedPost(postId: String) = withContext(Dispatchers.IO) {
        postDao.deleteById(postId)
    }

    suspend fun getSavedPostsForCurrentUser(): List<Post> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            val savedSnapshot = firestore.collection("users").document(userId)
                .collection("savedPosts").get().await()
            val savedIds = savedSnapshot.documents.map { it.id }
            if (savedIds.isEmpty()) return@withContext emptyList()

            val localPosts = postDao.getPostsByIds(savedIds)
            if (localPosts.size == savedIds.size) return@withContext localPosts

            val remotePosts = firestore.collection(POSTS_COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), savedIds)
                .get().await()
                .documents.mapNotNull { it.toPost() }
            
            postDao.insert(*remotePosts.toTypedArray())
            remotePosts
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPost(): Post? {
        return try {
            Post(
                id = id,
                recipeTitle = getString("recipeTitle") ?: "",
                description = getString("description") ?: "",
                imageUrl = getString("imageUrl") ?: "",
                creatorName = getString("creatorName") ?: "",
                creatorUid = getString("creatorUid") ?: "",
                latitude = getDouble("latitude"),
                longitude = getDouble("longitude"),
                lastUpdated = getLong("lastUpdated") ?: 0L,
                savesCount = getLong("savesCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Post.toFirestorePayload(): Map<String, Any> {
        val payload = hashMapOf<String, Any>(
            "recipeTitle" to recipeTitle,
            "description" to description,
            "imageUrl" to imageUrl,
            "creatorName" to creatorName,
            "creatorUid" to creatorUid,
            FIELD_LAST_UPDATED to lastUpdated,
            "savesCount" to savesCount,
        )
        if (latitude != null && longitude != null) {
            payload["latitude"] = latitude
            payload["longitude"] = longitude
        }
        return payload
    }

    private fun getLastSync(): Long {
        return syncPreferences.getLong(KEY_LAST_SYNC, 0L)
    }

    private fun saveLastSync(timestamp: Long) {
        syncPreferences.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
        private const val PREFS_NAME = "post_sync_prefs"
        private const val KEY_LAST_SYNC = "last_posts_sync"
        private const val FIELD_LAST_UPDATED = "lastUpdated"
        private const val MAP_POSTS_LIMIT = 100
    }
}
