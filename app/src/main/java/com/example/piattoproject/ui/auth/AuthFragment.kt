package com.example.piattoproject.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.piattoproject.MainActivity
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentAuthBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar

class AuthFragment : Fragment() {
    private var binding: FragmentAuthBinding? = null
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        setupListeners()
        observeAuthUiState()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupListeners() {
        val viewBinding = binding ?: return
        viewBinding.authSubmitButton.setOnClickListener {
            authViewModel.onSubmitClicked()
        }
        viewBinding.authModeToggleButton.setOnClickListener {
            authViewModel.onModeToggleClicked()
        }
        viewBinding.authRegisterUsernameEditText.doAfterTextChanged { text ->
            authViewModel.onRegisterUsernameChanged(text?.toString().orEmpty())
        }
        viewBinding.authEmailEditText.doAfterTextChanged { text ->
            authViewModel.onEmailChanged(text?.toString().orEmpty())
        }
        viewBinding.authPasswordEditText.doAfterTextChanged { text ->
            authViewModel.onPasswordChanged(text?.toString().orEmpty())
        }
        viewBinding.authConfirmPasswordEditText.doAfterTextChanged { text ->
            authViewModel.onConfirmPasswordChanged(text?.toString().orEmpty())
        }
    }

    private fun observeAuthUiState() {
        authViewModel.authUiState.observe(viewLifecycleOwner) { uiState ->
            bindAuthUiState(uiState)
        }
    }

    private fun bindAuthUiState(uiState: AuthUiState) {
        val viewBinding = binding ?: return
        val isRegisterMode = uiState.isRegisterMode

        val titleRes = if (isRegisterMode) R.string.auth_register_title else R.string.auth_login_title
        val subtitleRes = if (isRegisterMode) {
            R.string.auth_register_subtitle
        } else {
            R.string.auth_login_subtitle
        }
        val submitRes = if (isRegisterMode) R.string.auth_register_button else R.string.auth_login_button
        val toggleRes = if (isRegisterMode) {
            R.string.auth_switch_to_login
        } else {
            R.string.auth_switch_to_register
        }

        viewBinding.authTitleTextView.setText(titleRes)
        viewBinding.authSubtitleTextView.setText(subtitleRes)
        viewBinding.authSubmitButton.setText(submitRes)
        viewBinding.authModeToggleButton.setText(toggleRes)
        viewBinding.authRegisterUsernameInputLayout.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        viewBinding.authConfirmPasswordInputLayout.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        viewBinding.authProgressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

        updateEditTextIfDifferent(
            viewBinding.authRegisterUsernameEditText.text?.toString(),
            uiState.registerUsername,
        ) {
            viewBinding.authRegisterUsernameEditText.setText(uiState.registerUsername)
        }
        updateEditTextIfDifferent(viewBinding.authEmailEditText.text?.toString(), uiState.email) {
            viewBinding.authEmailEditText.setText(uiState.email)
        }
        updateEditTextIfDifferent(viewBinding.authPasswordEditText.text?.toString(), uiState.password) {
            viewBinding.authPasswordEditText.setText(uiState.password)
        }
        updateEditTextIfDifferent(
            viewBinding.authConfirmPasswordEditText.text?.toString(),
            uiState.confirmPassword,
        ) {
            viewBinding.authConfirmPasswordEditText.setText(uiState.confirmPassword)
        }

        viewBinding.authRegisterUsernameInputLayout.error = uiState.registerUsernameError
        viewBinding.authEmailInputLayout.error = uiState.emailError
        viewBinding.authPasswordInputLayout.error = uiState.passwordError
        viewBinding.authConfirmPasswordInputLayout.error = uiState.confirmPasswordError

        val isInteractionEnabled = !uiState.isLoading
        viewBinding.authSubmitButton.isEnabled = isInteractionEnabled
        viewBinding.authModeToggleButton.isEnabled = isInteractionEnabled
        viewBinding.authRegisterUsernameEditText.isEnabled = isInteractionEnabled
        viewBinding.authEmailEditText.isEnabled = isInteractionEnabled
        viewBinding.authPasswordEditText.isEnabled = isInteractionEnabled
        viewBinding.authConfirmPasswordEditText.isEnabled = isInteractionEnabled

        if (!uiState.errorMessage.isNullOrBlank()) {
            Snackbar.make(viewBinding.root, uiState.errorMessage, Snackbar.LENGTH_LONG).show()
            authViewModel.onErrorMessageShown()
        }

        if (uiState.isAuthenticated && FirebaseAuth.getInstance().currentUser != null) {
            findNavController().navigate(R.id.action_auth_to_feed)
        }
    }

    private fun updateEditTextIfDifferent(currentValue: String?, targetValue: String, onUpdate: () -> Unit) {
        if (currentValue != targetValue) {
            onUpdate()
        }
    }
}
