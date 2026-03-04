package com.example.food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.food.model.Meal
import com.example.food.viewmodel.MealUiState
import com.example.food.viewmodel.MealViewModel
import kotlinx.coroutines.delay

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
                        delay(2000)
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
fun RecipeScreen(viewModel: MealViewModel, onMealClick: (Meal) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isFrench by viewModel.isFrench.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // En-tête et bascule de langue globale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUPINFOOD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

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

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                viewModel.searchMeals(it)
            },
            label = { Text(if (isFrench) "Rechercher (ex: chicken)" else "Search (e.g. chicken)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                Button(onClick = {
                    searchText = ""
                    viewModel.selectCategory(category.name)
                }) {
                    Text(text = category.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gestion des états d'affichage
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val currentState = state) {
                is MealUiState.Loading -> CircularProgressIndicator()

                is MealUiState.Error -> {
                    // Cas : Panne réseau + Cache vide
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83C\uDF10", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isFrench) "Oups, un problème est survenu !" else "Oops, something went wrong !",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.searchMeals(searchText) }) {
                            Text(if (isFrench) "Réessayer" else "Retry")
                        }
                    }
                }

                is MealUiState.Success -> {
                    if (currentState.meals.isEmpty()) {
                        // Cas : Recherche valide mais aucun résultat
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDD0D", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isFrench) "Aucun plat trouvé." else "No meals found.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(currentState.meals) { meal ->
                                Box(modifier = Modifier.clickable { onMealClick(meal) }) {
                                    RecipeItem(meal = meal)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(meal: Meal) {
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
            Text(text = meal.name, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun DetailScreen(meal: Meal, viewModel: MealViewModel, onBack: () -> Unit) {
    val isFrench by viewModel.isFrench.collectAsState()
    val translatedText by viewModel.translatedInstructions.collectAsState()
    val isDownloading by viewModel.isDownloadingModel.collectAsState()
    val translatedIngredientsList by viewModel.translatedIngredients.collectAsState()

    // Traduction automatique à l'ouverture si le mode FR est activé
    LaunchedEffect(isFrench, meal) {
        if (isFrench && translatedText == null) {
            viewModel.translateMeal(meal)
        }
    }

    // Gère le bouton retour matériel (Android)
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Button(onClick = onBack) {
            Text(if (isFrench) "Retour" else "Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        AsyncImage(
            model = meal.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = meal.name, style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "${meal.category ?: "Inconnu"} • ${meal.area ?: "Inconnu"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION INGRÉDIENTS (Lazy loading horizontal) ---
        Text(
            text = if (isFrench) "Ingrédients" else "Ingredients",
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

                // Découpage de la traduction formatée "Nom (Mesure)"
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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (displayMeasure.isNotBlank()) {
                        Text(
                            text = displayMeasure,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION INSTRUCTIONS ---
        Text(text = "Instructions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (isFrench && isDownloading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Traduction...")
            }
        } else {
            Text(
                text = if (isFrench) translatedText ?: "Chargement..." else meal.instructions ?: "No instructions",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}