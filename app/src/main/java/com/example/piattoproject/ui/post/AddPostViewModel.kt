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

    private val _isLocationEnabled = MutableLiveData(false)
    val isLocationEnabled: LiveData<Boolean> = _isLocationEnabled

    private val _locationStatusText = MutableLiveData("(Optional)")
    val locationStatusText: LiveData<String> = _locationStatusText

    private var currentLat: Double? = null
    private var currentLon: Double? = null

    fun setLocationEnabled(enabled: Boolean) {
        if (_isLocationEnabled.value != enabled) {
            _isLocationEnabled.value = enabled
            if (!enabled) {
                currentLat = null
                currentLon = null
                _locationStatusText.value = "(Optional)"
            } else if (currentLat == null) {
                _locationStatusText.value = "Detecting location..."
            }
        }
    }

    fun setLocationCoords(lat: Double, lon: Double, address: String) {
        currentLat = lat
        currentLon = lon
        _locationStatusText.postValue(address)
        // Ensure switch stays ON if we got coords
        if (_isLocationEnabled.value == false) {
            _isLocationEnabled.postValue(true)
        }
    }

    fun setLocationStatus(status: String) {
        _locationStatusText.postValue(status)
    }

    fun hasValidCoordinates(): Boolean {
        return currentLat != null && currentLon != null
    }

    fun setSelectedImageUri(uri: Uri?) {
        selectedImageUri = uri
    }

    fun createPost(title: String, description: String) {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, isSuccess = false, errorMessage = null)
        
        viewModelScope.launch {
            try {
                var imageUrl = ""
                selectedImageUri?.let { uri ->
                    imageUrl = repository.uploadImage(uri)
                }

                repository.createPost(
                    title = title.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    latitude = if (_isLocationEnabled.value == true) currentLat else null,
                    longitude = if (_isLocationEnabled.value == true) currentLon else null,
                ).onSuccess {
                    _uiState.postValue(AddPostUiState(isSuccess = true))
                }.onFailure { error ->
                    _uiState.postValue(_uiState.value?.copy(errorMessage = error.message, isLoading = false))
                }
            } catch (e: Exception) {
                _uiState.postValue(_uiState.value?.copy(errorMessage = "Upload failed: ${e.message}", isLoading = false))
            }
        }
    }

    fun updatePost(postId: String, title: String, description: String) {
        if (title.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value?.copy(isLoading = true, isSuccess = false, errorMessage = null)

        viewModelScope.launch {
            try {
                var imageUrl: String? = null
                selectedImageUri?.let { uri ->
                    imageUrl = repository.uploadImage(uri)
                }

                repository.updatePost(
                    postId = postId,
                    title = title.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    latitude = if (_isLocationEnabled.value == true) currentLat else null,
                    longitude = if (_isLocationEnabled.value == true) currentLon else null,
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

    fun loadPostForEdit(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            val post = repository.getPostById(postId)
            if (post != null) {
                _uiState.postValue(_uiState.value?.copy(isLoading = false, editingPost = post))
                if (post.latitude != null && post.longitude != null) {
                    currentLat = post.latitude
                    currentLon = post.longitude
                    _isLocationEnabled.postValue(true)
                    _locationStatusText.postValue("Location attached")
                }
            } else {
                _uiState.postValue(_uiState.value?.copy(isLoading = false, errorMessage = "Post not found"))
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, isSuccess = false, isLoading = false)
    }
}
