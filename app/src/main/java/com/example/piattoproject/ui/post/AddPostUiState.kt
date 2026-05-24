package com.example.piattoproject.ui.post

data class AddPostUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val editingPost: Post? = null,
)
