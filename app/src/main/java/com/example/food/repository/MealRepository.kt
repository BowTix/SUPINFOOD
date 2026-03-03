package com.example.food.repository

import com.example.food.api.MealApi
import com.example.food.model.Meal
import com.example.food.model.Category

class MealRepository {
    // On crée l'instance de l'API directement ici
    private val api = MealApi.create()

    // Cette fonction sera appelée par le ViewModel
    suspend fun getMeals(query: String): List<Meal> {
        return try {
            val response = api.searchMeals(query)
            // L'API renvoie null si elle ne trouve rien, nous on préfère une liste vide, c'est plus facile à gérer
            response.meals ?: emptyList()
        } catch (e: Exception) {
            // En cas d'erreur (pas de réseau), pour l'instant on renvoie une liste vide
            // On améliorera ça plus tard
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCategories(): List<Category> {
        return try {
            api.getCategories().categories
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMealsByCategory(category: String): List<Meal> {
        return try {
            val response = api.filterByCategory(category)
            response.meals ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMealById(id: String): Meal? {
        return try {
            val response = api.getMealDetails(id)
            // On renvoie la première recette trouvée (ou null)
            response.meals?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}