package com.example.piattoproject.ui.profile

class ProfileMockDataSource {
    fun getProfileUiState(): ProfileUiState {
        val displayName = "Ella Gliksberg"
        val username = "@ella_cooks"
        val bio = "Recipe lover sharing quick and healthy Mediterranean dishes."
        return ProfileUiState(
            displayName = displayName,
            username = username,
            bio = bio,
            profileImageUri = null,
            editedDisplayName = displayName,
            editedUsername = username,
            editedBio = bio,
            isEditing = false,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
        )
    }
}
