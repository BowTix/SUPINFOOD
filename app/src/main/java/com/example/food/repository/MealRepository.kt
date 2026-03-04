package com.example.food.repository

import com.example.food.api.MealApi
import com.example.food.data.MealDao
import com.example.food.model.Category
import com.example.food.model.Meal

class MealRepository(private val mealDao: MealDao) {

    private val api = MealApi.create()

    suspend fun getMeals(query: String): List<Meal> {
        return try {
            val response = api.searchMeals(query)
            val mealsFromApi = response.meals ?: emptyList()

            // Mise en cache des résultats réseau pour le mode hors-ligne
            if (mealsFromApi.isNotEmpty()) {
                mealDao.insertMeals(mealsFromApi)
            }
            mealsFromApi
        } catch (e: Exception) {
            // Fallback : En cas de coupure réseau, on interroge la base locale (Room)
            val localMeals = if (query.isEmpty()) {
                mealDao.getAllMeals()
            } else {
                mealDao.searchMealsLocal(query)
            }

            if (localMeals.isEmpty()) {
                throw Exception("Veuillez vérifier votre connexion Internet.")
            }
            localMeals
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
            val allLocalMeals = mealDao.getAllMeals()
            allLocalMeals.filter { it.category == category }
        }
    }

    suspend fun getMealById(id: String): Meal? {
        return try {
            val response = api.getMealDetails(id)
            val meal = response.meals?.firstOrNull()

            if (meal != null) {
                mealDao.insertMeals(listOf(meal))
            }
            meal
        } catch (e: Exception) {
            mealDao.getAllMeals().find { it.id == id }
        }
    }
}