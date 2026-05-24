package com.example.piattoproject.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)
    
    // ה-UI יקשיב לזה - זה מגיע מ-Room ותמיד מעודכן
    val posts: LiveData<List<Post>> = repository.allPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun refreshPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.refreshPosts()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sync with server. Showing offline data."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
