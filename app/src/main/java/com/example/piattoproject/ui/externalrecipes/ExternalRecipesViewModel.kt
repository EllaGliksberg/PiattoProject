package com.example.piattoproject.ui.externalrecipes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.data.remote.externalrecipes.MealDto
import com.example.piattoproject.repository.ExternalRecipesRepository
import kotlinx.coroutines.launch

class ExternalRecipesViewModel : ViewModel() {

    private val repository = ExternalRecipesRepository()

    private val _recipes = MutableLiveData<List<MealDto>>(emptyList())
    val recipes: LiveData<List<MealDto>> = _recipes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun searchRecipes(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _recipes.value = repository.searchRecipes(query)
            } catch (e: Exception) {
                _error.value = "Failed to load recipes"
                _recipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
