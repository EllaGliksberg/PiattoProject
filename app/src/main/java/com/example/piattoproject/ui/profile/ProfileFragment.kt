package com.example.piattoproject.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.piattoproject.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null
    private lateinit var profileViewModel: ProfileViewModel
    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
        }
        profileViewModel.onProfileImageSelected(uri.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileViewModel = ViewModelProvider(this, ProfileViewModelFactory())[ProfileViewModel::class.java]
        setupListeners()
        observeProfileUiState()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun observeProfileUiState() {
        profileViewModel.profileUiState.observe(viewLifecycleOwner) { uiState ->
            bindProfileUiState(uiState)
        }
    }

    private fun setupListeners() {
        val viewBinding = binding ?: return
        viewBinding.editProfileButton.setOnClickListener {
            profileViewModel.onEditClicked()
        }
        viewBinding.cancelEditButton.setOnClickListener {
            profileViewModel.onCancelClicked()
        }
        viewBinding.saveProfileButton.setOnClickListener {
            profileViewModel.onSaveClicked()
        }
        viewBinding.profileImageView.setOnClickListener {
            pickProfileImage.launch(arrayOf("image/*"))
        }
        viewBinding.displayNameEditText.doAfterTextChanged { text ->
            profileViewModel.onEditedDisplayNameChanged(text?.toString().orEmpty())
        }
        viewBinding.usernameEditText.doAfterTextChanged { text ->
            profileViewModel.onEditedUsernameChanged(text?.toString().orEmpty())
        }
        viewBinding.bioEditText.doAfterTextChanged { text ->
            profileViewModel.onEditedBioChanged(text?.toString().orEmpty())
        }
    }

    private fun bindProfileUiState(uiState: ProfileUiState) {
        val viewBinding = binding ?: return
        viewBinding.displayNameTextView.text = uiState.displayName
        viewBinding.usernameTextView.text = uiState.username
        viewBinding.bioTextView.text = uiState.bio
        bindProfileImage(uiState.profileImageUri)
        val shouldShowLoading = uiState.isLoading || uiState.isSaving
        viewBinding.profileLoadingProgressBar.visibility = if (shouldShowLoading) View.VISIBLE else View.GONE
        viewBinding.profileSyncStateTextView.visibility = if (shouldShowLoading) View.VISIBLE else View.GONE

        viewBinding.profileViewModeLayout.visibility = if (uiState.isEditing) View.GONE else View.VISIBLE
        viewBinding.viewModeActionsLayout.visibility = if (uiState.isEditing) View.GONE else View.VISIBLE
        viewBinding.profileEditModeLayout.visibility = if (uiState.isEditing) View.VISIBLE else View.GONE
        viewBinding.editModeActionsLayout.visibility = if (uiState.isEditing) View.VISIBLE else View.GONE

        updateEditTextIfDifferent(viewBinding.displayNameEditText.text?.toString(), uiState.editedDisplayName) {
            viewBinding.displayNameEditText.setText(uiState.editedDisplayName)
        }
        updateEditTextIfDifferent(viewBinding.usernameEditText.text?.toString(), uiState.editedUsername) {
            viewBinding.usernameEditText.setText(uiState.editedUsername)
        }
        updateEditTextIfDifferent(viewBinding.bioEditText.text?.toString(), uiState.editedBio) {
            viewBinding.bioEditText.setText(uiState.editedBio)
        }

        viewBinding.displayNameInputLayout.error = uiState.displayNameError
        viewBinding.usernameInputLayout.error = uiState.usernameError
        val isInteractionEnabled = !uiState.isSaving && !uiState.isLoading
        viewBinding.saveProfileButton.isEnabled = isInteractionEnabled
        viewBinding.editProfileButton.isEnabled = isInteractionEnabled
        viewBinding.cancelEditButton.isEnabled = isInteractionEnabled
        viewBinding.profileImageView.isEnabled = isInteractionEnabled

        if (!uiState.errorMessage.isNullOrBlank()) {
            Snackbar.make(viewBinding.root, uiState.errorMessage, Snackbar.LENGTH_LONG).show()
            profileViewModel.onErrorMessageShown()
        }
    }

    private fun bindProfileImage(profileImageUri: String?) {
        val viewBinding = binding ?: return
        if (profileImageUri.isNullOrBlank()) {
            viewBinding.profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
            return
        }
        val uri = Uri.parse(profileImageUri)
        try {
            viewBinding.profileImageView.setImageURI(null)
            viewBinding.profileImageView.setImageURI(uri)
        } catch (_: SecurityException) {
            profileViewModel.onProfileImageLoadFailed()
            viewBinding.profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
        } catch (_: IllegalArgumentException) {
            profileViewModel.onProfileImageLoadFailed()
            viewBinding.profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    private fun updateEditTextIfDifferent(currentValue: String?, targetValue: String, onUpdate: () -> Unit) {
        if (currentValue != targetValue) {
            onUpdate()
        }
    }

    private inner class ProfileViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    profileImageLocalStore = ProfileImageLocalStore(requireContext().applicationContext),
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
