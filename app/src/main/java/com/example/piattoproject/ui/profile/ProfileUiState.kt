package com.example.piattoproject.ui.profile

data class ProfileUiState(
    val displayName: String,
    val username: String,
    val bio: String,
    val editedDisplayName: String,
    val editedUsername: String,
    val editedBio: String,
    val isEditing: Boolean,
    val isSaving: Boolean,
    val displayNameError: String?,
    val usernameError: String?,
)
