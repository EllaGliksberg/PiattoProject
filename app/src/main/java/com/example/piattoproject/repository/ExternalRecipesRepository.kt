package com.example.piattoproject.repository

import com.example.piattoproject.data.remote.externalrecipes.MealApiService
import com.example.piattoproject.data.remote.externalrecipes.MealDto
import com.example.piattoproject.data.remote.externalrecipes.RetrofitClient

class ExternalRecipesRepository(
    private val api: MealApiService = RetrofitClient.mealApi
) {
    suspend fun searchRecipes(query: String): List<MealDto> {
        return api.searchMeals(query).meals ?: emptyList()
    }
}
