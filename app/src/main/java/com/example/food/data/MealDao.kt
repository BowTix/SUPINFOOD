package com.example.food.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.food.model.Meal

@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    @Query("SELECT * FROM meals WHERE name LIKE '%' || :searchQuery || '%'")
    suspend fun searchMealsLocal(searchQuery: String): List<Meal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<Meal>)

    @Query("DELETE FROM meals")
    suspend fun clearAll()
}