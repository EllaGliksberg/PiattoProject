package com.example.piattoproject.ui.post

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AddPostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableLiveData(AddPostUiState())
    val uiState: LiveData<AddPostUiState> = _uiState

    private var selectedImageUri: Uri? = null

    fun setSelectedImageUri(uri: Uri?) {
        selectedImageUri = uri
    }

    fun createPost(
        title: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, isSuccess = false, errorMessage = null)
        
        viewModelScope.launch {
            try {
                var imageUrl = ""
                // 1. העלאת תמונה לענן אם נבחרה
                selectedImageUri?.let { uri ->
                    imageUrl = repository.uploadImage(uri)
                }

                // 2. שמירה ב-Firestore וב-Room (מתבצע בתוך ה-Repository)
                repository.createPost(
                    title = title.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    latitude = latitude,
                    longitude = longitude,
                ).onSuccess {
                    _uiState.postValue(AddPostUiState(isSuccess = true))
                }.onFailure { error ->
                    _uiState.postValue(AddPostUiState(errorMessage = error.message, isLoading = false))
                }
            } catch (e: Exception) {
                _uiState.postValue(AddPostUiState(errorMessage = "Image upload failed: ${e.message}", isLoading = false))
            }
        }
    }

    fun resetState() {
        _uiState.value = AddPostUiState()
    }

    fun loadPostForEdit(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            val post = repository.getPostById(postId)
            if (post != null) {
                // We need a way to pass this data to the Fragment
                _uiState.postValue(_uiState.value?.copy(
                    isLoading = false,
                    editingPost = post
                ))
            } else {
                _uiState.postValue(_uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "Post not found"
                ))
            }
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, isSuccess = false, errorMessage = null)

        viewModelScope.launch {
            try {
                var imageUrl: String? = null
                // 1. Upload new image if selected
                selectedImageUri?.let { uri ->
                    imageUrl = repository.uploadImage(uri)
                }

                // 2. Update in Firestore and Room
                repository.updatePost(
                    postId = postId,
                    title = title.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    latitude = latitude,
                    longitude = longitude,
                ).onSuccess {
                    _uiState.postValue(AddPostUiState(isSuccess = true))
                }.onFailure { error ->
                    _uiState.postValue(_uiState.value?.copy(errorMessage = error.message, isLoading = false))
                }
            } catch (e: Exception) {
                _uiState.postValue(_uiState.value?.copy(errorMessage = "Update failed: ${e.message}", isLoading = false))
            }
        }
    }
}
