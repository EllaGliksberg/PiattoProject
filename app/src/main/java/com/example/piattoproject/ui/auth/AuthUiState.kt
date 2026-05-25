package com.example.piattoproject.ui.auth

data class AuthUiState(
    val isRegisterMode: Boolean = false,
    val registerUsername: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val registerUsernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)
