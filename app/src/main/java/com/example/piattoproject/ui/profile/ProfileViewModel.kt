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
}
