package com.example.food.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.food.data.AppDatabase
import com.example.food.model.Category
import com.example.food.model.Meal
import com.example.food.repository.MealRepository
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MealUiState {
    object Loading : MealUiState()
    data class Success(val meals: List<Meal>) : MealUiState()
    data class Error(val message: String) : MealUiState()
}

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MealRepository(db.mealDao())

    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedMeal = MutableStateFlow<Meal?>(null)
    val selectedMeal: StateFlow<Meal?> = _selectedMeal.asStateFlow()

    // --- ETATS GLOBAUX ---
    private val _isFrench = MutableStateFlow(false)
    val isFrench: StateFlow<Boolean> = _isFrench.asStateFlow()

    private val _translatedInstructions = MutableStateFlow<String?>(null)
    val translatedInstructions: StateFlow<String?> = _translatedInstructions.asStateFlow()

    private val _translatedIngredients = MutableStateFlow<List<String>?>(null)
    val translatedIngredients: StateFlow<List<String>?> = _translatedIngredients.asStateFlow()

    private val _isDownloadingModel = MutableStateFlow(false)
    val isDownloadingModel: StateFlow<Boolean> = _isDownloadingModel.asStateFlow()

    // --- CONFIGURATION ML KIT ---
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.FRENCH)
        .build()
    private val translator = Translation.getClient(options)

    init {
        loadCategories()
        searchMeals("")
        startPeriodicDataRefresh()
    }

    fun toggleLanguage(french: Boolean) {
        _isFrench.value = french
    }

    private fun startPeriodicDataRefresh() {
        viewModelScope.launch {
            while (true) {
                try {
                    repository.getMeals("")
                } catch (e: Exception) { /* Silencieux en cas d'erreur réseau en arrière-plan */ }
                delay(15 * 60 * 1000L) // Rafraîchissement toutes les 15 minutes
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repository.getCategories()
        }
    }

    fun searchMeals(query: String) {
        viewModelScope.launch {
            _uiState.value = MealUiState.Loading
            try {
                val meals = repository.getMeals(query)
                _uiState.value = MealUiState.Success(meals)
            } catch (e: Exception) {
                _uiState.value = MealUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun selectCategory(category: String) {
        viewModelScope.launch {
            _uiState.value = MealUiState.Loading
            try {
                val meals = repository.getMealsByCategory(category)
                _uiState.value = MealUiState.Success(meals)
            } catch (e: Exception) {
                _uiState.value = MealUiState.Error("Impossible de charger la catégorie.")
            }
        }
    }

    fun selectMeal(meal: Meal) {
        viewModelScope.launch {
            // On récupère les détails complets (avec les instructions) avant d'afficher
            val fullMeal = repository.getMealById(meal.id) ?: meal
            _selectedMeal.value = fullMeal
        }
    }

    fun clearSelection() {
        _selectedMeal.value = null
        _translatedInstructions.value = null
        _translatedIngredients.value = null
    }

    // --- TRADUCTION ---
    fun translateMeal(meal: Meal) {
        viewModelScope.launch {
            _isDownloadingModel.value = true

            // Remplacement des sauts de ligne par des balises <br> pour empêcher l'IA de fusionner les étapes de la recette lors de la traduction.
            val instructionsPrepared = (meal.instructions ?: "")
                .replace("\r\n", "\n")
                .replace("\n", " <br> ")

            val ingredientsList = meal.getIngredients()
            val ingredientsPrepared = ingredientsList.joinToString(" ### ") { (name, measure) ->
                "$name ($measure)"
            }

            val conditions = DownloadConditions.Builder().requireWifi().build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator.translate(instructionsPrepared).addOnSuccessListener { text ->
                        val finalInstruction = text
                            .replace(" <br> ", "\n")
                            .replace("<br>", "\n")
                            .replace("< br >", "\n")
                            .replace("&lt;br&gt;", "\n")
                        _translatedInstructions.value = finalInstruction
                    }

                    translator.translate(ingredientsPrepared).addOnSuccessListener { text ->
                        val finalList = text.split(" ### ").map { it.trim() }
                        _translatedIngredients.value = finalList
                        _isDownloadingModel.value = false
                    }.addOnFailureListener { _isDownloadingModel.value = false }
                }
                .addOnFailureListener {
                    _isDownloadingModel.value = false
                }
        }
    }
}