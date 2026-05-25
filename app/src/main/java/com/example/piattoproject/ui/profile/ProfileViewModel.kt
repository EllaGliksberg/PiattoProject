package com.example.piattoproject.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.utils.ImageUtils
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val profileImageLocalStore: ProfileImageLocalStore,
    private val appContext: Context,
    private val profileRepository: FirebaseProfileRepository = FirebaseProfileRepository(),
    private val userPostsRepository: FirebaseUserPostsRepository = FirebaseUserPostsRepository(),
    private val postRepository: PostRepository = PostRepository(appContext),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ViewModel() {
    private val _profileUiState = MutableLiveData(createInitialState())
    val profileUiState: LiveData<ProfileUiState> = _profileUiState
    private var loadedUserId: String? = null

    init {
        refreshForCurrentUser()
    }

    fun refreshForCurrentUser() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == loadedUserId) {
            return
        }

        loadedUserId = currentUserId
        if (currentUserId == null) {
            _profileUiState.value = createInitialState().copy(isLoading = false)
            return
        }

        _profileUiState.value = createInitialState()
        loadProfile(currentUserId)
        refreshMyPosts()
        refreshSavedPosts()
    }

    fun clearForSignedOutUser() {
        loadedUserId = null
        _profileUiState.value = createInitialState().copy(isLoading = false)
    }

    fun refreshSavedPosts() {
        val expectedUserId = loadedUserId ?: return
        val current = _profileUiState.value ?: createInitialState()
        _profileUiState.value = current.copy(isLoadingSavedPosts = true)
        viewModelScope.launch {
            val posts = postRepository.getSavedPostsForCurrentUser()
            if (!isCurrentUser(expectedUserId)) {
                return@launch
            }
            val state = _profileUiState.value ?: return@launch
            _profileUiState.value = state.copy(savedPosts = posts, isLoadingSavedPosts = false)
        }
    }

    fun refreshMyPosts() {
        val expectedUserId = loadedUserId ?: return
        val current = _profileUiState.value ?: createInitialState()
        _profileUiState.value = current.copy(isLoadingMyPosts = true)
        viewModelScope.launch {
            runCatching { userPostsRepository.loadPostsForSignedInUser() }
                .onSuccess { posts ->
                    if (!isCurrentUser(expectedUserId)) {
                        return@launch
                    }
                    val state = _profileUiState.value ?: return@launch
                    _profileUiState.value = state.copy(myPosts = posts, isLoadingMyPosts = false)
                }
                .onFailure {
                    if (!isCurrentUser(expectedUserId)) {
                        return@launch
                    }
                    val state = _profileUiState.value ?: return@launch
                    _profileUiState.value = state.copy(isLoadingMyPosts = false)
                }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            runCatching {
                userPostsRepository.deletePostForCurrentUser(post.id)
                postRepository.deleteCachedPost(post.id)
            }.onSuccess {
                val state = _profileUiState.value ?: return@launch
                _profileUiState.value = state.copy(
                    myPosts = state.myPosts.filter { it.id != post.id },
                )
            }.onFailure {
                _profileUiState.value = (_profileUiState.value ?: return@launch).copy(
                    errorMessage = "Could not delete post. Please try again.",
                )
            }
        }
    }

    private fun loadProfile(expectedUserId: String) {
        val currentState = _profileUiState.value ?: createInitialState()
        _profileUiState.value = currentState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { profileRepository.loadProfile() }
                .onSuccess { profile ->
                    if (!isCurrentUser(expectedUserId)) {
                        return@launch
                    }
                    _profileUiState.value = createLoadedState(
                        profile = profile,
                        imageReference = profile.imageUrl,
                    )
                }
                .onFailure {
                    if (!isCurrentUser(expectedUserId)) {
                        return@launch
                    }
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
                profileRepository.saveProfile(
                    fullName = displayName,
                    username = username,
                    bio = bio,
                )
            }.onSuccess { savedProfile ->
                val prevPosts = _profileUiState.value?.myPosts ?: currentState.myPosts
                val prevLoadingPosts = _profileUiState.value?.isLoadingMyPosts ?: currentState.isLoadingMyPosts
                val prevSavedPosts = _profileUiState.value?.savedPosts ?: currentState.savedPosts
                _profileUiState.value = createLoadedState(
                    profile = savedProfile,
                    imageReference = currentState.profileImageUri,
                ).copy(
                    myPosts = prevPosts,
                    isLoadingMyPosts = prevLoadingPosts,
                    savedPosts = prevSavedPosts
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

    fun onProfileImageSelected(uri: Uri) {
        val currentState = _profileUiState.value ?: return
        _profileUiState.value = currentState.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val encodedImage = withContext(Dispatchers.IO) {
                    ImageUtils.encodeImageUriToBase64(appContext, uri)
                }
                profileRepository.saveProfileImage(encodedImage)
            }.onSuccess { savedProfile ->
                profileImageLocalStore.saveProfileImageUri(null)
                _profileUiState.value = createLoadedState(
                    profile = savedProfile,
                    imageReference = savedProfile.imageUrl,
                )
            }.onFailure {
                _profileUiState.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "Could not save profile image. Please try again.",
                )
            }
        }
    }

    fun onProfileImageLoadFailed() {
        val currentState = _profileUiState.value ?: return
        if (currentState.profileImageUri == null) {
            return
        }
        _profileUiState.value = currentState.copy(profileImageUri = null)
    }

    fun onErrorMessageShown() {
        val currentState = _profileUiState.value ?: return
        if (currentState.errorMessage.isNullOrBlank()) {
            return
        }
        _profileUiState.value = currentState.copy(errorMessage = null)
    }

    private fun isCurrentUser(expectedUserId: String): Boolean {
        return auth.currentUser?.uid == expectedUserId && loadedUserId == expectedUserId
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
            myPosts = emptyList(),
            isLoadingMyPosts = false,
            savedPosts = emptyList(),
            isLoadingSavedPosts = false
        )
    }

    private fun createLoadedState(profile: FirebaseProfile, imageReference: String?): ProfileUiState {
        val previous = _profileUiState.value
        return ProfileUiState(
            displayName = profile.fullName,
            username = profile.username,
            bio = profile.bio,
            profileImageUri = imageReference,
            editedDisplayName = profile.fullName,
            editedUsername = profile.username,
            editedBio = profile.bio,
            isEditing = false,
            isLoading = false,
            isSaving = false,
            displayNameError = null,
            usernameError = null,
            errorMessage = null,
            myPosts = previous?.myPosts ?: emptyList(),
            isLoadingMyPosts = previous?.isLoadingMyPosts ?: false,
            savedPosts = previous?.savedPosts ?: emptyList(),
            isLoadingSavedPosts = previous?.isLoadingSavedPosts ?: false
        )
    }
}
