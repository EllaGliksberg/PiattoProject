package com.example.piattoproject.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    val posts: LiveData<List<Post>> = repository.allPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadInitialFeed() {
        viewModelScope.launch {
            syncPosts(showLoading = false)
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            syncPosts(showLoading = true)
        }
    }

    private suspend fun syncPosts(showLoading: Boolean) {
        if (showLoading) {
            _isLoading.value = true
        }
        _errorMessage.value = null

        val result = repository.syncPostsDelta()
        if (result.isFailure) {
            _errorMessage.value = "Failed to refresh posts"
        }

        if (showLoading) {
            _isLoading.value = false
        }
    }
}
