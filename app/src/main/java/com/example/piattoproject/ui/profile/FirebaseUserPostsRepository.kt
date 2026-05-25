package com.example.piattoproject.ui.profile

import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.toPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUserPostsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun loadPostsForSignedInUser(): List<Post> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo(FIELD_CREATOR_UID, uid)
            .get()
            .await()
        return snapshot.documents
            .map { it.toPost() }
            .sortedByDescending { it.lastUpdated }
    }

    suspend fun deletePostForCurrentUser(postId: String) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val snapshot = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
        if (!snapshot.exists()) {
            throw IllegalStateException("Post not found")
        }
        val creatorUid = snapshot.getString(FIELD_CREATOR_UID).orEmpty()
        if (creatorUid != uid) {
            throw IllegalStateException("Cannot delete this post")
        }
        firestore.collection(POSTS_COLLECTION).document(postId).delete().await()
    }

    suspend fun updatePostForCurrentUser(post: Post) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        if (post.creatorUid != uid) {
            throw IllegalStateException("Cannot update this post")
        }
        val snapshot = firestore.collection(POSTS_COLLECTION).document(post.id).get().await()
        if (!snapshot.exists()) {
            throw IllegalStateException("Post not found")
        }
        val storedCreatorUid = snapshot.getString(FIELD_CREATOR_UID).orEmpty()
        if (storedCreatorUid != uid) {
            throw IllegalStateException("Cannot update this post")
        }
        val payload = hashMapOf<String, Any>(
            "recipeTitle" to post.recipeTitle,
            "description" to post.description,
            "imageUrl" to post.imageUrl,
            "creatorName" to post.creatorName,
            "creatorUid" to post.creatorUid,
            "lastUpdated" to post.lastUpdated,
        )
        if (post.latitude != null && post.longitude != null) {
            payload["latitude"] = post.latitude
            payload["longitude"] = post.longitude
        }
        firestore.collection(POSTS_COLLECTION).document(post.id).set(payload).await()
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
        const val FIELD_CREATOR_UID = "creatorUid"
    }
}
