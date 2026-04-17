package com.example.piattoproject.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.ui.profile.FirebaseProfile
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileImageLocalStore: ProfileImageLocalStore,
    private val repository: FirebaseProfileRepository = FirebaseProfileRepository(),
) : ViewModel() {
    private val _profileUiState = MutableLiveData(createInitialState())
    val profileUiState: LiveData<ProfileUiState> = _profileUiState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val currentState = _profileUiState.value ?: createInitialState()
        _profileUiState.value = currentState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.loadProfile() }
                .onSuccess { profile ->
                    _profileUiState.value = createLoadedState(
                        profile = profile,
                        localImageUri = profileImageLocalStore.getProfileImageUri(),
                    )
                }
                .onFailure {
                    _profileUiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "Could not load profile. Check your connection and try again.",
                    )
                }
        }
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
            errorMessage = null,
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
            errorMessage = null,
        )
    }

    fun onEditedDisplayNameChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            editedDisplayName = value,
            displayNameError = null,
            errorMessage = null,
        )
    }

    fun onEditedUsernameChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            editedUsername = value,
            usernameError = null,
            errorMessage = null,
        )
    }

    fun onEditedBioChanged(value: String) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(
            editedBio = value,
            errorMessage = null,
        )
    }

    fun onSaveClicked() {
        val currentState = _profileUiState.value ?: return
        if (currentState.isSaving || currentState.isLoading) {
            return
        }
        val displayName = currentState.editedDisplayName.trim()
        val username = currentState.editedUsername.trim()
        val bio = currentState.editedBio.trim()

        val displayNameError = validateDisplayName(displayName)
        val usernameError = validateUsername(username)
        if (displayNameError != null || usernameError != null) {
            _profileUiState.value = currentState.copy(
                displayNameError = displayNameError,
                usernameError = usernameError,
                isSaving = false,
                errorMessage = null,
            )
            return
        }

        _profileUiState.value = currentState.copy(
            isSaving = true,
            displayNameError = null,
            usernameError = null,
            errorMessage = null,
        )
        viewModelScope.launch {
            runCatching {
                repository.saveProfile(
                    fullName = displayName,
                    username = username,
                    bio = bio,
                )
            }.onSuccess { savedProfile ->
                _profileUiState.value = createLoadedState(
                    profile = savedProfile,
                    localImageUri = currentState.profileImageUri,
                )
            }.onFailure {
                _profileUiState.value = currentState.copy(
                    isSaving = false,
                    displayNameError = null,
                    usernameError = null,
                    errorMessage = "Could not save profile. Please try again.",
                )
            }
        }
    }

    fun onProfileImageSelected(uri: String) {
        val currentState = _profileUiState.value ?: return
        profileImageLocalStore.saveProfileImageUri(uri)
        _profileUiState.value = currentState.copy(
            profileImageUri = uri,
            errorMessage = null,
        )
    }

    fun onProfileImageLoadFailed() {
        val currentState = _profileUiState.value ?: return
        if (currentState.profileImageUri == null) {
            return
        }
        profileImageLocalStore.saveProfileImageUri(null)
        _profileUiState.value = currentState.copy(profileImageUri = null)
    }

    fun onErrorMessageShown() {
        val currentState = _profileUiState.value ?: return
        if (currentState.errorMessage.isNullOrBlank()) {
            return
        }
        _profileUiState.value = currentState.copy(errorMessage = null)
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

    private fun createInitialState(): ProfileUiState {
        return ProfileUiState(
            displayName = "",
            username = "",
            bio = "",
            profileImageUri = null,
            editedDisplayName = "",
            editedUsername = "",
            editedBio = "",
            isEditing = false,
            isLoading = true,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
            errorMessage = null,
        )
    }

    private fun createLoadedState(profile: FirebaseProfile, localImageUri: String?): ProfileUiState {
        return ProfileUiState(
            displayName = profile.fullName,
            username = profile.username,
            bio = profile.bio,
            profileImageUri = localImageUri,
            editedDisplayName = profile.fullName,
            editedUsername = profile.username,
            editedBio = profile.bio,
            isEditing = false,
            isLoading = false,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
            errorMessage = null,
        )
    }
}
