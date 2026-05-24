package com.example.piattoproject.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AddPostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableLiveData(AddPostUiState())
    val uiState: LiveData<AddPostUiState> = _uiState

    fun loadPostForEdit(postId: String) {
        _uiState.value = AddPostUiState(isLoading = true)
        viewModelScope.launch {
            repository.loadPost(postId)
                .onSuccess { post ->
                    _uiState.value = AddPostUiState(editingPost = post)
                }
                .onFailure { error ->
                    _uiState.value = AddPostUiState(errorMessage = error.toUserMessage())
                }
        }
    }

    fun createPost(
        title: String,
        description: String,
        imageUrl: String?,
        latitude: Double?,
        longitude: Double?,
    ) {
        if (!validateInput(title, description)) {
            return
        }
        _uiState.value = _uiState.value.orEmpty().copy(isLoading = true, isSuccess = false, errorMessage = null)
        viewModelScope.launch {
            repository.createPost(
                title = title.trim(),
                description = description.trim(),
                imageUrl = imageUrl?.trim(),
                latitude = latitude,
                longitude = longitude,
            ).handleSaveResult()
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        description: String,
        imageUrl: String?,
    ) {
        if (!validateInput(title, description)) {
            return
        }
        _uiState.value = _uiState.value.orEmpty().copy(isLoading = true, isSuccess = false, errorMessage = null)
        viewModelScope.launch {
            repository.updatePost(
                postId = postId,
                title = title.trim(),
                description = description.trim(),
                imageUrl = imageUrl?.trim(),
            ).handleSaveResult()
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.orEmpty().copy(isLoading = false, isSuccess = false, errorMessage = null)
    }

    private fun validateInput(title: String, description: String): Boolean {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value.orEmpty().copy(
                isLoading = false,
                isSuccess = false,
                errorMessage = "Please fill in all fields",
            )
            return false
        }
        return true
    }

    private fun Result<Unit>.handleSaveResult() {
        onSuccess {
            _uiState.value = _uiState.value.orEmpty().copy(isLoading = false, isSuccess = true, errorMessage = null)
        }.onFailure { error ->
            _uiState.value = _uiState.value.orEmpty().copy(
                isLoading = false,
                isSuccess = false,
                errorMessage = error.toUserMessage(),
            )
        }
    }

    private fun AddPostUiState?.orEmpty(): AddPostUiState = this ?: AddPostUiState()

    private fun Throwable.toUserMessage(): String {
        return message?.takeIf { it.isNotBlank() } ?: "Could not save post. Please try again."
    }
}
