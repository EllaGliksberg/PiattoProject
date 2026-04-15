package com.example.piattoproject.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel(
    private val profileMockDataSource: ProfileMockDataSource = ProfileMockDataSource(),
) : ViewModel() {
    private val _profileUiState = MutableLiveData<ProfileUiState>()
    val profileUiState: LiveData<ProfileUiState> = _profileUiState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        _profileUiState.value = profileMockDataSource.getProfileUiState()
    }

    fun onEditClicked() {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            isEditing = true,
            editedDisplayName = currentState.displayName,
            editedUsername = currentState.username,
            editedBio = currentState.bio,
            displayNameError = null,
            usernameError = null,
        )
    }

    fun onCancelClicked() {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            isEditing = false,
            editedDisplayName = currentState.displayName,
            editedUsername = currentState.username,
            editedBio = currentState.bio,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
        )
    }

    fun onEditedDisplayNameChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            editedDisplayName = value,
            displayNameError = null,
        )
    }

    fun onEditedUsernameChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            editedUsername = value,
            usernameError = null,
        )
    }

    fun onEditedBioChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(editedBio = value)
    }

    fun onSaveClicked() {
        val currentState = _profileUiState.value ?: return
        val displayName = currentState.editedDisplayName.trim()
        val username = currentState.editedUsername.trim()

        val displayNameError = validateDisplayName(displayName)
        val usernameError = validateUsername(username)
        if (displayNameError != null || usernameError != null) {
            _profileUiState.value = currentState.copy(
                displayNameError = displayNameError,
                usernameError = usernameError,
                isSaving = false,
            )
            return
        }

        _profileUiState.value = currentState.copy(isSaving = true)
        _profileUiState.value = currentState.copy(
            displayName = displayName,
            username = username,
            bio = currentState.editedBio.trim(),
            editedDisplayName = displayName,
            editedUsername = username,
            editedBio = currentState.editedBio.trim(),
            isEditing = false,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
        )
    }

    private fun validateDisplayName(displayName: String): String? {
        if (displayName.isBlank()) {
            return "Name is required"
        }
        if (displayName.length > 40) {
            return "Name must be at most 40 characters"
        }
        return null
    }

    private fun validateUsername(username: String): String? {
        if (username.isBlank()) {
            return "Username is required"
        }
        if (!username.startsWith("@")) {
            return "Username must start with @"
        }
        if (username.contains(' ')) {
            return "Username cannot contain spaces"
        }
        if (username.length < 2 || username.length > 25) {
            return "Username must be 2-25 characters"
        }
        return null
    }
}
