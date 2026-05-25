package com.example.piattoproject.data.remote.externalrecipes

import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {

    @GET("search.php")
    suspend fun searchMeals(
        @Query("s") query: String
    ): MealResponseDto
}
