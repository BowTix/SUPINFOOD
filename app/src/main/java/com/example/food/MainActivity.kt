package com.example.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.food.model.Category
import com.example.food.viewmodel.MealUiState
import com.example.food.viewmodel.MealViewModel
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MealViewModel by viewModels()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    val selectedMeal by viewModel.selectedMeal.collectAsState()

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        if (selectedMeal == null) {
                            RecipeScreen(
                                viewModel = viewModel,
                                onMealClick = { meal -> viewModel.selectMeal(meal) }
                            )
                        } else {
                            DetailScreen(
                                meal = selectedMeal!!,
                                viewModel = viewModel,
                                onBack = { viewModel.clearSelection() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(viewModel: MealViewModel, onMealClick: (com.example.food.model.Meal) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // On écoute la langue globale
    val isFrench by viewModel.isFrench.collectAsState()

    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- EN-TÊTE AVEC LE SWITCH ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUPINFOOD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // LE SWITCH GLOBAL
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EN", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isFrench,
                    onCheckedChange = { viewModel.toggleLanguage(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("FR", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BARRE DE RECHERCHE (Texte dynamique)
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                viewModel.searchMeals(it)
            },
            label = {
                Text(if (isFrench) "Rechercher (ex: chicken)" else "Search (e.g. chicken)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // CATÉGORIES
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                Button(
                    onClick = {
                        searchText = ""
                        viewModel.selectCategory(category.name)
                    }
                ) {
                    // Petite astuce : Traduire les catégories est complexe (car elles viennent de l'API)
                    // Mais on affiche le nom original pour l'instant
                    Text(text = category.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LISTE
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val currentState = state) {
                is MealUiState.Loading -> CircularProgressIndicator()
                is MealUiState.Error -> Text(currentState.message, color = MaterialTheme.colorScheme.error)
                is MealUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(currentState.meals) { meal ->
                            Box(modifier = Modifier.clickable { onMealClick(meal) }) {
                                RecipeItem(meal = meal)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (currentState.meals.size < viewModel.getTotalMealsCount()) {
                            item {
                                LaunchedEffect(true) { viewModel.loadNextPage() }
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(meal: com.example.food.model.Meal) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = meal.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = meal.name,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun DetailScreen(
    meal: com.example.food.model.Meal,
    viewModel: MealViewModel,
    onBack: () -> Unit
) {
    // On écoute la langue GLOBALE
    val isFrench by viewModel.isFrench.collectAsState()

    val translatedText by viewModel.translatedInstructions.collectAsState()
    val isDownloading by viewModel.isDownloadingModel.collectAsState()
    val translatedIngredientsList by viewModel.translatedIngredients.collectAsState()

    // LA MAGIE : Dès que l'écran s'ouvre (ou que la langue change), on vérifie
    LaunchedEffect(isFrench, meal) {
        if (isFrench && translatedText == null) {
            viewModel.translateMeal(meal)
        }
    }

    // GESTION RETOUR
    androidx.activity.compose.BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Bouton Retour (Texte dynamique)
        Button(onClick = onBack) {
            Text(if (isFrench) "Retour" else "Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image
        AsyncImage(
            model = meal.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Titre
        Text(text = meal.name, style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "${meal.category ?: "Inconnu"} • ${meal.area ?: "Inconnu"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- ZONE INGRÉDIENTS (HORIZONTAL) ---
        Text(
            text = if (isFrench) "Ingrédients" else "Ingredients", // Titre traduit
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        val originalIngredients = meal.getIngredients()

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(originalIngredients.size) { index ->
                val (originalName, originalMeasure) = originalIngredients[index]

                var displayName = originalName
                var displayMeasure = originalMeasure

                if (isFrench && translatedIngredientsList != null && index < translatedIngredientsList!!.size) {
                    val fullString = translatedIngredientsList!![index]
                    if (fullString.contains("(") && fullString.endsWith(")")) {
                        displayName = fullString.substringBeforeLast(" (").trim()
                        displayMeasure = fullString.substringAfterLast(" (").removeSuffix(")").trim()
                    } else {
                        displayName = fullString
                        displayMeasure = ""
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(100.dp)
                ) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = "https://www.themealdb.com/images/ingredients/${originalName}.png",
                                contentDescription = originalName,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (displayMeasure.isNotBlank()) {
                        Text(
                            text = displayMeasure,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- INSTRUCTIONS ---
        Text(
            text = if (isFrench) "Instructions" else "Instructions", // (Bon c'est le même mot ^^)
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isFrench && isDownloading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Traduction...")
            }
        } else {
            Text(
                text = if (isFrench) {
                    translatedText ?: "Chargement..."
                } else {
                    meal.instructions ?: "No instructions"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUPINFOOD",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}