package com.example.food.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.food.model.Category
import com.example.food.model.Meal
import com.example.food.repository.MealRepository
import com.google.mlkit.common.model.DownloadConditions // <--- Important
import com.google.mlkit.nl.translate.TranslateLanguage // <--- Important
import com.google.mlkit.nl.translate.Translation      // <--- Important
import com.google.mlkit.nl.translate.TranslatorOptions // <--- Important
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 1. On définit les 3 états possibles de l'écran
sealed interface MealUiState {
    data object Loading : MealUiState                  // Ça charge (sablier)
    data class Success(val meals: List<Meal>) : MealUiState // Ça a marché (liste)
    data class Error(val message: String) : MealUiState     // Ça a planté (message)
}

// 2. La classe ViewModel
class MealViewModel : ViewModel() {

    private val repository = MealRepository()

    // --- ETATS UI ---
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedMeal = MutableStateFlow<Meal?>(null)
    val selectedMeal: StateFlow<Meal?> = _selectedMeal.asStateFlow()

    // --- GESTION PAGINATION ---
    private var allMeals: List<Meal> = emptyList() // La liste COMPLÈTE (stockée en cache ici)
    private val PAGE_SIZE = 10 // On affiche 10 recettes par page

// --- TRADUCTION ---

    // Config (inchangé)
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.FRENCH)
        .build()
    private val translator = Translation.getClient(options)

    // État pour les INSTRUCTIONS
    private val _translatedInstructions = MutableStateFlow<String?>(null)
    val translatedInstructions: StateFlow<String?> = _translatedInstructions.asStateFlow()

    // NOUVEAU : État pour les INGRÉDIENTS (Liste de textes)
    private val _translatedIngredients = MutableStateFlow<List<String>?>(null)
    val translatedIngredients: StateFlow<List<String>?> = _translatedIngredients.asStateFlow()

    private val _isDownloadingModel = MutableStateFlow(false)
    val isDownloadingModel: StateFlow<Boolean> = _isDownloadingModel.asStateFlow()

    // 1. ÉTAT GLOBAL DE LA LANGUE
    private val _isFrench = MutableStateFlow(false)
    val isFrench: StateFlow<Boolean> = _isFrench.asStateFlow()

    fun toggleLanguage(french: Boolean) {
        _isFrench.value = french
    }

    // On passe l'objet Meal entier pour avoir accès aux ingrédients
    fun translateMeal(meal: Meal) {
        viewModelScope.launch {
            _isDownloadingModel.value = true

            // 1. Préparation des INSTRUCTIONS (avec la ruse <br>)
            val instructionsPreped = (meal.instructions ?: "")
                .replace("\r\n", "\n")
                .replace("\n", " <br> ")

            // 2. Préparation des INGRÉDIENTS
            // On colle tout en une seule phrase séparée par " ### " pour faire une seule requête
            // Ex: "Chicken (1kg) ### Garlic (1 clove) ### ..."
            val ingredientsList = meal.getIngredients()
            val ingredientsPreped = ingredientsList.joinToString(" ### ") { (name, measure) ->
                "$name ($measure)"
            }

            val conditions = DownloadConditions.Builder().requireWifi().build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {

                    // --- TRADUCTION INSTRUCTIONS ---
                    translator.translate(instructionsPreped).addOnSuccessListener { text ->
                        val finalInstruction = text
                            .replace(" <br> ", "\n")
                            .replace("<br>", "\n")
                            .replace("< br >", "\n")
                        _translatedInstructions.value = finalInstruction
                    }

                    // --- TRADUCTION INGRÉDIENTS ---
                    translator.translate(ingredientsPreped).addOnSuccessListener { text ->
                        // On re-découpe la phrase en liste grâce au séparateur "###"
                        val finalList = text.split(" ### ").map { it.trim() }
                        _translatedIngredients.value = finalList

                        // C'est fini !
                        _isDownloadingModel.value = false
                    }.addOnFailureListener { _isDownloadingModel.value = false }

                }
                .addOnFailureListener {
                    _isDownloadingModel.value = false
                }
        }
    }

    fun clearSelection() {
        _selectedMeal.value = null
        _translatedInstructions.value = null
        _translatedIngredients.value = null // Reset
    }

    // On pense à remettre à zéro quand on quitte l'écran
    override fun onCleared() {
        super.onCleared()
        translator.close()
    }

    init {
        viewModelScope.launch {
            _categories.value = repository.getCategories()
        }
        searchMeals("")
    }

    // 1. RECHERCHE (On récupère tout, mais on n'affiche que le début)
    fun searchMeals(query: String) {
        viewModelScope.launch {
            _uiState.value = MealUiState.Loading
            try {
                // On récupère TOUT depuis l'API
                allMeals = repository.getMeals(query)

                if (allMeals.isNotEmpty()) {
                    // On ne donne à l'UI que les 10 premiers
                    val firstPage = allMeals.take(PAGE_SIZE)
                    _uiState.value = MealUiState.Success(firstPage)
                } else {
                    _uiState.value = MealUiState.Error("Rien trouvé...")
                }
            } catch (e: Exception) { _uiState.value = MealUiState.Error("Erreur réseau") }
        }
    }

    // 2. FILTRE CATÉGORIE (Pareil, on récupère tout et on coupe)
    fun selectCategory(categoryName: String) {
        viewModelScope.launch {
            _uiState.value = MealUiState.Loading
            try {
                allMeals = repository.getMealsByCategory(categoryName)
                if (allMeals.isNotEmpty()) {
                    val firstPage = allMeals.take(PAGE_SIZE)
                    _uiState.value = MealUiState.Success(firstPage)
                } else {
                    _uiState.value = MealUiState.Error("Catégorie vide")
                }
            } catch (e: Exception) { _uiState.value = MealUiState.Error("Erreur réseau") }
        }
    }

    // 3. LA FONCTION MAGIQUE : Charger la suite
// Variable pour éviter de charger 2 fois la page si on scrolle comme un fou
    private var isLoadingNextPage = false

    fun loadNextPage() {
        val currentState = _uiState.value

        // On ne fait rien si :
        // 1. On n'est pas en mode succès
        // 2. On est DÉJÀ en train de charger la page suivante
        if (currentState !is MealUiState.Success || isLoadingNextPage) return

        // Si on a déjà tout affiché, on s'arrête
        if (currentState.meals.size >= allMeals.size) return

        viewModelScope.launch {
            isLoadingNextPage = true

            val currentSize = currentState.meals.size
            val nextBatch = allMeals.drop(currentSize).take(PAGE_SIZE)

            // Mise à jour de la liste
            _uiState.value = MealUiState.Success(currentState.meals + nextBatch)

            isLoadingNextPage = false
        }
    }

    // ... (Gardes tes fonctions selectMeal et clearSelection telles quelles) ...
    fun selectMeal(meal: Meal) {
        viewModelScope.launch {
            if (!meal.instructions.isNullOrEmpty()) {
                _selectedMeal.value = meal
            } else {
                val fullMeal = repository.getMealById(meal.id)
                _selectedMeal.value = fullMeal ?: meal
            }
        }
    }

    // Permet à l'écran de savoir combien on a de recettes au total en mémoire
    fun getTotalMealsCount(): Int {
        return allMeals.size
    }
}