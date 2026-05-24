package com.example.piattoproject.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppLocalDbRepository.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun refreshPosts() {
        _isLoading.value = true
        _errorMessage.value = null
        firestore.collection("posts")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                val postList = mutableListOf<Post>()
                for (document in documents) {
                    postList.add(document.toPost())
                }

                Thread {
                    postList.forEach { db.postDao().insert(it) }
                    _posts.postValue(postList)
                    _isLoading.postValue(false)
                }.start()
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to refresh posts"
                _isLoading.value = false
            }
    }
}
