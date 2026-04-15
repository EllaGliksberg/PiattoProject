package com.example.piattoproject.ui.profile

class ProfileMockDataSource {
    fun getProfileUiState(): ProfileUiState {
        return ProfileUiState(
            displayName = "Ella Gliksberg",
            username = "@ella_cooks",
            bio = "Recipe lover sharing quick and healthy Mediterranean dishes.",
        )
    }
}
