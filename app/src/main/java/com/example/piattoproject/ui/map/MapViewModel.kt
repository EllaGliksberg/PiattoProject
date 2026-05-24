package com.example.piattoproject.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.toPost
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadMapPosts() {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val snapshot = firestore.collection(POSTS_COLLECTION)
                    .orderBy("lastUpdated", Query.Direction.DESCENDING)
                    .limit(MAP_POSTS_LIMIT.toLong())
                    .get()
                    .await()
                val withLocation = snapshot.documents
                    .map { it.toPost() }
                    .filter { it.latitude != null && it.longitude != null }
                _posts.value = withLocation
            } catch (_: Exception) {
                _errorMessage.value = "Failed to load map posts"
            }
        }
    }

    private companion object {
        private const val POSTS_COLLECTION = "posts"
        private const val MAP_POSTS_LIMIT = 100
    }
}
