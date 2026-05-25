package com.example.piattoproject.ui.profile

import android.content.Context

class ProfileImageLocalStore(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getProfileImageUri(): String? {
        return sharedPreferences.getString(KEY_PROFILE_IMAGE_URI, null)
    }

    fun saveProfileImageUri(uri: String?) {
        sharedPreferences.edit().putString(KEY_PROFILE_IMAGE_URI, uri).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "profile_local_store"
        private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    }
}
