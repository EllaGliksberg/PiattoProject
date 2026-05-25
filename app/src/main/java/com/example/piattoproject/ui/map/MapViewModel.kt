package com.example.piattoproject.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.PostRepository
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadMapPosts() {
        viewModelScope.launch {
            _errorMessage.value = null
            repository.loadMapPosts()
                .onSuccess { postsWithLocation ->
                    _posts.value = postsWithLocation
                }
                .onFailure {
                    _errorMessage.value = "Failed to load map posts"
                }
        }
    }
}
