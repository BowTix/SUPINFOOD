package com.example.food.api

import com.example.food.model.MealResponse
import com.example.food.model.CategoryResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MealApi {
    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): MealResponse

    // Récupérer la liste des catégories (Beef, Chicken, etc.)
    @GET("categories.php")
    suspend fun getCategories(): CategoryResponse

    // Récupérer les recettes d'une catégorie spécifique
    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): MealResponse

    // Récupérer une recette précise par son ID
    @GET("lookup.php")
    suspend fun getMealDetails(@Query("i") id: String): MealResponse

    companion object {
        // Un petit utilitaire pour créer l'API facilement (Singleton simple)
        private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

        fun create(): MealApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(MealApi::class.java)
        }
    }
}