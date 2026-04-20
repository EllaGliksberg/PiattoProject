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

    fun refreshPosts() {
        firestore.collection("posts")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                val postList = mutableListOf<Post>()
                for (document in documents) {
                    val post = Post(
                        id = document.id,
                        recipeTitle = document.getString("recipeTitle") ?: "",
                        description = document.getString("description") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        creatorName = document.getString("creatorName") ?: "",
                        lastUpdated = document.getLong("lastUpdated") ?: 0L
                    )
                    postList.add(post)
                }

                Thread {
                    postList.forEach { db.postDao().insert(it) }
                    _posts.postValue(postList)
                }.start()
            }
    }
}