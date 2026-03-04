package com.example.food.model

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey

data class MealResponse(
    @SerializedName("meals")
    val meals: List<Meal>?
)

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey
    @SerializedName("idMeal") val id: String,
    @SerializedName("strMeal") val name: String,
    @SerializedName("strMealThumb") val imageUrl: String,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?,
    @SerializedName("strInstructions") val instructions: String?,

    val strIngredient1: String?, val strMeasure1: String?,
    val strIngredient2: String?, val strMeasure2: String?,
    val strIngredient3: String?, val strMeasure3: String?,
    val strIngredient4: String?, val strMeasure4: String?,
    val strIngredient5: String?, val strMeasure5: String?,
    val strIngredient6: String?, val strMeasure6: String?,
    val strIngredient7: String?, val strMeasure7: String?,
    val strIngredient8: String?, val strMeasure8: String?,
    val strIngredient9: String?, val strMeasure9: String?,
    val strIngredient10: String?, val strMeasure10: String?,
    val strIngredient11: String?, val strMeasure11: String?,
    val strIngredient12: String?, val strMeasure12: String?,
    val strIngredient13: String?, val strMeasure13: String?,
    val strIngredient14: String?, val strMeasure14: String?,
    val strIngredient15: String?, val strMeasure15: String?,
    val strIngredient16: String?, val strMeasure16: String?,
    val strIngredient17: String?, val strMeasure17: String?,
    val strIngredient18: String?, val strMeasure18: String?,
    val strIngredient19: String?, val strMeasure19: String?,
    val strIngredient20: String?, val strMeasure20: String?
) {
    fun getIngredients(): List<Pair<String, String>> {
        val ingredients = mutableListOf<Pair<String, String>>()

        if (!strIngredient1.isNullOrBlank()) ingredients.add(strIngredient1 to (strMeasure1 ?: ""))
        if (!strIngredient2.isNullOrBlank()) ingredients.add(strIngredient2 to (strMeasure2 ?: ""))
        if (!strIngredient3.isNullOrBlank()) ingredients.add(strIngredient3 to (strMeasure3 ?: ""))
        if (!strIngredient4.isNullOrBlank()) ingredients.add(strIngredient4 to (strMeasure4 ?: ""))
        if (!strIngredient5.isNullOrBlank()) ingredients.add(strIngredient5 to (strMeasure5 ?: ""))
        if (!strIngredient6.isNullOrBlank()) ingredients.add(strIngredient6 to (strMeasure6 ?: ""))
        if (!strIngredient7.isNullOrBlank()) ingredients.add(strIngredient7 to (strMeasure7 ?: ""))
        if (!strIngredient8.isNullOrBlank()) ingredients.add(strIngredient8 to (strMeasure8 ?: ""))
        if (!strIngredient9.isNullOrBlank()) ingredients.add(strIngredient9 to (strMeasure9 ?: ""))
        if (!strIngredient10.isNullOrBlank()) ingredients.add(strIngredient10 to (strMeasure10 ?: ""))
        if (!strIngredient11.isNullOrBlank()) ingredients.add(strIngredient11 to (strMeasure11 ?: ""))
        if (!strIngredient12.isNullOrBlank()) ingredients.add(strIngredient12 to (strMeasure12 ?: ""))
        if (!strIngredient13.isNullOrBlank()) ingredients.add(strIngredient13 to (strMeasure13 ?: ""))
        if (!strIngredient14.isNullOrBlank()) ingredients.add(strIngredient14 to (strMeasure14 ?: ""))
        if (!strIngredient15.isNullOrBlank()) ingredients.add(strIngredient15 to (strMeasure15 ?: ""))
        if (!strIngredient16.isNullOrBlank()) ingredients.add(strIngredient16 to (strMeasure16 ?: ""))
        if (!strIngredient17.isNullOrBlank()) ingredients.add(strIngredient17 to (strMeasure17 ?: ""))
        if (!strIngredient18.isNullOrBlank()) ingredients.add(strIngredient18 to (strMeasure18 ?: ""))
        if (!strIngredient19.isNullOrBlank()) ingredients.add(strIngredient19 to (strMeasure19 ?: ""))
        if (!strIngredient20.isNullOrBlank()) ingredients.add(strIngredient20 to (strMeasure20 ?: ""))

        return ingredients
    }
}