package com.example.piattoproject.ui.auth

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: FirebaseAuthRepository = FirebaseAuthRepository(),
) : ViewModel() {
    private val _authUiState = MutableLiveData(AuthUiState())
    val authUiState: LiveData<AuthUiState> = _authUiState

    fun onModeToggleClicked() {
        val currentState = _authUiState.value ?: AuthUiState()
        _authUiState.value = currentState.copy(
            isRegisterMode = !currentState.isRegisterMode,
            registerUsername = "",
            password = "",
            confirmPassword = "",
            registerUsernameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            errorMessage = null,
        )
    }

    fun onRegisterUsernameChanged(value: String) {
        val currentState = _authUiState.value ?: return
        _authUiState.value = currentState.copy(
            registerUsername = value,
            registerUsernameError = null,
            errorMessage = null,
        )
    }

    fun onEmailChanged(value: String) {
        val currentState = _authUiState.value ?: return
        _authUiState.value = currentState.copy(
            email = value,
            emailError = null,
            errorMessage = null,
        )
    }

    fun onPasswordChanged(value: String) {
        val currentState = _authUiState.value ?: return
        _authUiState.value = currentState.copy(
            password = value,
            passwordError = null,
            errorMessage = null,
        )
    }

    fun onConfirmPasswordChanged(value: String) {
        val currentState = _authUiState.value ?: return
        _authUiState.value = currentState.copy(
            confirmPassword = value,
            confirmPasswordError = null,
            errorMessage = null,
        )
    }

    fun onSubmitClicked() {
        val currentState = _authUiState.value ?: return
        if (currentState.isLoading) {
            return
        }
        val email = currentState.email.trim()
        val registerUsername = currentState.registerUsername.trim()
        val password = currentState.password
        val confirmPassword = currentState.confirmPassword

        val registerUsernameError = if (currentState.isRegisterMode) {
            validateRegisterUsername(registerUsername)
        } else {
            null
        }
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val confirmPasswordError = if (currentState.isRegisterMode) {
            validateConfirmPassword(password, confirmPassword)
        } else {
            null
        }

        if (
            registerUsernameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null
        ) {
            _authUiState.value = currentState.copy(
                registerUsernameError = registerUsernameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
                errorMessage = null,
            )
            return
        }

        _authUiState.value = currentState.copy(
            registerUsername = registerUsername,
            email = email,
            isLoading = true,
            registerUsernameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            errorMessage = null,
        )

        viewModelScope.launch {
            runCatching {
                if (currentState.isRegisterMode) {
                    repository.register(
                        email = email,
                        password = password,
                        registerUsername = registerUsername,
                    )
                } else {
                    repository.signIn(email = email, password = password)
                }
            }.onSuccess {
                _authUiState.value = (_authUiState.value ?: currentState).copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null,
                )
            }.onFailure { throwable ->
                _authUiState.value = (_authUiState.value ?: currentState).copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = throwable.toReadableMessage(),
                )
            }
        }
    }

    fun onErrorMessageShown() {
        val currentState = _authUiState.value ?: return
        if (currentState.errorMessage.isNullOrBlank()) {
            return
        }
        _authUiState.value = currentState.copy(errorMessage = null)
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) {
            return "Email is required"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Enter a valid email address"
        }
        return null
    }

    private fun validateRegisterUsername(registerUsername: String): String? {
        if (registerUsername.isBlank()) {
            return "Username is required"
        }
        if (registerUsername.length > 40) {
            return "Username must be at most 40 characters"
        }
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) {
            return "Password is required"
        }
        if (password.length < 6) {
            return "Password must be at least 6 characters"
        }
        return null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        if (confirmPassword.isBlank()) {
            return "Confirm your password"
        }
        if (password != confirmPassword) {
            return "Passwords do not match"
        }
        return null
    }

    private fun Throwable.toReadableMessage(): String {
        val exception = this as? FirebaseAuthException
        return when (exception?.errorCode) {
            "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password"
            "ERROR_WRONG_PASSWORD" -> "Invalid email or password"
            "ERROR_USER_NOT_FOUND" -> "No account found for this email"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address"
            else -> localizedMessage ?: "Authentication failed. Please try again."
        }
    }
}
